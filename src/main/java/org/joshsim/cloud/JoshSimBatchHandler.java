/**
 * Handler for batch simulation execution from pre-staged local files.
 *
 * <p>Implements the {@code /runBatch} endpoint. Unlike {@code /runReplicate} which streams code
 * and data in the request body and returns results via wire format, this handler runs a simulation
 * from a local directory of pre-staged files and results land in MinIO via {@code minio://} export
 * paths configured in the Josh script. Staging is a separate concern handled by the caller (e.g.,
 * {@code stageFromMinio} CLI command or K8s container entrypoint).</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.HttpString;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.joshsim.JoshSimFacadeUtil;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.JvmCompatibilityLayer;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.io.JvmInputOutputLayerBuilder;
import org.joshsim.lang.io.JvmMappedInputGetter;
import org.joshsim.lang.parse.ParseResult;
import org.joshsim.util.MinioHandler;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.MinioStagingUtil;
import org.joshsim.util.OutputOptions;


/**
 * Handles batch simulation requests where inputs are pre-staged locally.
 *
 * <p>Expected form fields:</p>
 * <ul>
 *   <li>{@code apiKey} — API key for authentication</li>
 *   <li>{@code jobId} — unique job identifier (returned in response)</li>
 *   <li>{@code simulation} — name of the simulation to run</li>
 *   <li>{@code workDir} — path to local directory containing pre-staged simulation files</li>
 * </ul>
 *
 * <p>Optional form fields for serverless environments where staging and execution
 * must happen atomically within the same request:</p>
 * <ul>
 *   <li>{@code stageFromMinio} — set to {@code "true"} to download inputs from MinIO
 *       into {@code workDir} before running. Controls input staging only, not outputs.</li>
 *   <li>{@code minioPrefix} — required when {@code stageFromMinio=true}. The object prefix
 *       to download from (e.g., {@code batch-jobs/abc/inputs/}).</li>
 * </ul>
 */
public class JoshSimBatchHandler implements HttpHandler {

  private static final ExecutorService BATCH_EXECUTOR =
      Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("batch-sim-" + thread.threadId());
        thread.setDaemon(true);
        return thread;
      });

  private final CloudApiDataLayer apiDataLayer;

  /**
   * Constructs a new JoshSimBatchHandler.
   *
   * @param apiDataLayer The cloud API data layer for API key validation and logging.
   */
  public JoshSimBatchHandler(CloudApiDataLayer apiDataLayer) {
    this.apiDataLayer = apiDataLayer;
  }

  // TODO: Extract common handleRequest boilerplate to an abstract base handler.
  //       See identical pattern in Worker, Leader, Parse, ConfigDiscovery handlers.
  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    if (exchange.isInIoThread()) {
      exchange.dispatch(this);
      return;
    }

    if (!CorsUtil.addCorsHeaders(exchange)) {
      return;
    }

    long startTime = System.nanoTime();
    Optional<String> apiKey = handleRequestInner(exchange);
    long endTime = System.nanoTime();

    long runtimeSeconds = (endTime - startTime) / 1_000_000_000;
    apiDataLayer.log(apiKey.orElse(""), "batch", runtimeSeconds);
  }

  private Optional<String> handleRequestInner(HttpServerExchange exchange) {
    if (!exchange.getRequestMethod().equalToString("POST")) {
      exchange.setStatusCode(405);
      return Optional.empty();
    }

    exchange.startBlocking();

    FormDataParser parser = FormParserFactory.builder().build().createParser(exchange);
    if (parser == null) {
      sendJsonError(exchange, 400, "missing-form", "Request must be form-encoded");
      return Optional.empty();
    }

    FormData formData;
    try {
      formData = parser.parseBlocking();
    } catch (IOException e) {
      sendJsonError(exchange, 400, "parse-error", "Failed to parse form data");
      return Optional.empty();
    }

    return processFormData(exchange, formData);
  }

  /**
   * Processes parsed form data for a batch request.
   *
   * <p>Package-visible for testing — allows tests to bypass form parsing by providing
   * pre-built FormData directly.</p>
   *
   * @param exchange The HTTP exchange for sending responses.
   * @param formData The parsed form data containing request parameters.
   * @return The API key used, or empty if the request was rejected.
   */
  Optional<String> processFormData(HttpServerExchange exchange, FormData formData) {
    ApiKeyUtil.ApiCheckResult apiCheck = ApiKeyUtil.checkApiKey(formData, apiDataLayer);
    if (!apiCheck.getKeyIsValid()) {
      sendJsonError(exchange, 401, "unauthorized", "Invalid API key");
      return Optional.empty();
    }
    String apiKey = apiCheck.getApiKey();

    if (!formData.contains("jobId") || !formData.contains("simulation")
        || !formData.contains("workDir")) {
      sendJsonError(exchange, 400, "missing-fields",
          "Required fields: jobId, simulation, workDir");
      return Optional.of(apiKey);
    }

    String jobId = formData.getFirst("jobId").getValue();
    File workDir = new File(formData.getFirst("workDir").getValue());

    // Opt-in: stage inputs from MinIO before running (for serverless environments)
    boolean shouldStage = formData.contains("stageFromMinio")
        && "true".equalsIgnoreCase(formData.getFirst("stageFromMinio").getValue());

    if (shouldStage) {
      if (!formData.contains("minioPrefix")) {
        sendJsonError(exchange, 400, jobId,
            "minioPrefix is required when stageFromMinio=true");
        return Optional.of(apiKey);
      }
      String minioPrefix = formData.getFirst("minioPrefix").getValue();
      try {
        if (!workDir.exists() && !workDir.mkdirs()) {
          sendJsonError(exchange, 400, jobId,
              "Failed to create workDir: " + workDir.getPath());
          return Optional.of(apiKey);
        }
        MinioOptions minioOptions = new MinioOptions();
        MinioHandler minio = new MinioHandler(minioOptions, new OutputOptions());
        MinioStagingUtil.stageFromMinio(minio, minioPrefix, workDir, new OutputOptions());
      } catch (Exception e) {
        sendJsonError(exchange, 500, jobId, "Staging from MinIO failed: " + e.getMessage());
        return Optional.of(apiKey);
      }
    }

    if (!workDir.exists() || !workDir.isDirectory()) {
      sendJsonError(exchange, 400, jobId,
          "workDir does not exist or is not a directory: " + workDir.getPath());
      return Optional.of(apiKey);
    }

    MinioHandler statusMinioInit = null;
    try {
      MinioOptions statusMinioOptions = new MinioOptions();
      statusMinioInit = new MinioHandler(statusMinioOptions, new OutputOptions());
    } catch (Exception e) {
      System.err.println("[batch] Status tracking unavailable for job " + jobId
          + ": " + e.getMessage());
    }
    final MinioHandler statusMinio = statusMinioInit;

    String simulation = formData.getFirst("simulation").getValue();
    String statusPath = "batch-status/" + jobId + "/status.json";
    sendJsonAccepted(exchange, jobId, statusPath);

    String capturedApiKey = apiKey;
    CompletableFuture.runAsync(() -> {
      runBatchWithStatus(statusMinio, jobId, simulation, workDir, statusPath, capturedApiKey);
    }, BATCH_EXECUTOR);

    return Optional.of(apiKey);
  }

  private void runBatchWithStatus(MinioHandler statusMinio, String jobId,
      String simulation, File workDir, String statusPath, String apiKey) {
    writeStatus(statusMinio, statusPath, String.format(
        "{\"status\":\"running\",\"jobId\":\"%s\",\"startedAt\":\"%s\"}",
        jobId, Instant.now().toString()
    ));

    try {
      executeBatchJob(jobId, simulation, workDir);

      writeStatus(statusMinio, statusPath, String.format(
          "{\"status\":\"complete\",\"jobId\":\"%s\",\"completedAt\":\"%s\"}",
          jobId, Instant.now().toString()
      ));
    } catch (Exception e) {
      SecurityUtil.logSecureError(apiDataLayer, apiKey, "batch", e, null);

      String safeMessage = e.getMessage() != null
          ? e.getMessage().replace("\"", "\\\"").replace("\n", " ")
          : "unknown error";
      writeStatus(statusMinio, statusPath, String.format(
          "{\"status\":\"error\",\"jobId\":\"%s\",\"message\":\"%s\",\"failedAt\":\"%s\"}",
          jobId, safeMessage, Instant.now().toString()
      ));
    }
  }

  private void writeStatus(MinioHandler statusMinio, String statusPath, String statusJson) {
    if (statusMinio == null) {
      return;
    }
    try {
      statusMinio.putBytes(
          statusJson.getBytes(StandardCharsets.UTF_8),
          statusPath,
          "application/json"
      );
    } catch (Exception e) {
      System.err.println("[batch] Failed to write status to " + statusPath
          + ": " + e.getMessage());
    }
  }

  private void executeBatchJob(String jobId, String simulation, File workDir) throws Exception {
    // Ensure JVM threading for parallel patch export
    CompatibilityLayerKeeper.set(new JvmCompatibilityLayer());

    File scriptFile = LocalFileUtil.findScriptFile(workDir);
    String code = Files.readString(scriptFile.toPath());

    ParseResult parseResult = JoshSimFacadeUtil.parse(code);
    if (parseResult.hasErrors()) {
      throw new IllegalArgumentException(
          "Parse errors in Josh script: " + parseResult.getErrors().iterator().next()
      );
    }

    Map<String, String> fileMapping = LocalFileUtil.buildFileMapping(workDir);
    MinioOptions minioOptions = new MinioOptions();
    InputOutputLayer inputOutputLayer = new JvmInputOutputLayerBuilder()
        .withInputStrategy(new JvmMappedInputGetter(fileMapping))
        .withMinioOptions(minioOptions)
        .build();

    ValueSupportFactory valueFactory = new ValueSupportFactory();
    GridGeometryFactory geometryFactory = new GridGeometryFactory();

    JoshProgram program = JoshSimFacadeUtil.interpret(
        valueFactory, geometryFactory, parseResult, inputOutputLayer
    );

    if (!program.getSimulations().hasPrototype(simulation)) {
      throw new IllegalArgumentException("Simulation not found: " + simulation);
    }

    JoshSimFacadeUtil.runSimulation(
        valueFactory,
        geometryFactory,
        inputOutputLayer,
        program,
        simulation,
        (step) -> { /* progress unused in batch mode */ },
        false,
        Optional.empty()
    );
  }

  private void sendJsonAccepted(HttpServerExchange exchange, String jobId, String statusPath) {
    exchange.setStatusCode(202);
    exchange.getResponseHeaders().put(new HttpString("Content-Type"), "application/json");
    exchange.getResponseSender().send(
        String.format(
            "{\"status\":\"accepted\",\"jobId\":\"%s\",\"statusPath\":\"%s\"}",
            jobId, statusPath
        )
    );
  }

  private void sendJsonError(HttpServerExchange exchange, int statusCode, String jobId,
      String message) {
    exchange.setStatusCode(statusCode);
    exchange.getResponseHeaders().put(new HttpString("Content-Type"), "application/json");
    String safeMessage = message.replace("\"", "\\\"").replace("\n", " ");
    exchange.getResponseSender().send(
        String.format("{\"status\":\"error\",\"jobId\":\"%s\",\"message\":\"%s\"}",
            jobId, safeMessage)
    );
  }
}
