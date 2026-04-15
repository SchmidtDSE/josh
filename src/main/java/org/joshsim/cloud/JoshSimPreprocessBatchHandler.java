/**
 * Handler for batch preprocessing of external data to jshd format.
 *
 * <p>Implements the {@code /preprocessBatch} endpoint. Accepts a pre-staged directory
 * containing a Josh script and data file, runs preprocessing via {@link
 * org.joshsim.command.PreprocessUtil}, and uploads the resulting .jshd file to MinIO.
 * Status tracking follows the same lifecycle as {@code /runBatch}.</p>
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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.joshsim.command.PreprocessUtil;
import org.joshsim.util.MinioHandler;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.MinioStagingUtil;
import org.joshsim.util.OutputOptions;


/**
 * Handles batch preprocessing requests where inputs are pre-staged locally.
 *
 * <p>Expected form fields:</p>
 * <ul>
 *   <li>{@code apiKey} — API key for authentication</li>
 *   <li>{@code jobId} — unique job identifier</li>
 *   <li>{@code simulation} — name of the simulation (for grid/metadata extraction)</li>
 *   <li>{@code dataFile} — filename of the data file within workDir</li>
 *   <li>{@code variable} — variable name or band number</li>
 *   <li>{@code units} — units string for simulation use</li>
 *   <li>{@code outputFile} — filename for the output .jshd</li>
 *   <li>{@code workDir} — path to local directory containing pre-staged files</li>
 * </ul>
 *
 * <p>Optional form fields:</p>
 * <ul>
 *   <li>{@code stageFromMinio} / {@code minioPrefix} — same as /runBatch</li>
 *   <li>{@code crs} (default {@code EPSG:4326})</li>
 *   <li>{@code xCoord} (default {@code lon})</li>
 *   <li>{@code yCoord} (default {@code lat})</li>
 *   <li>{@code timeDim} (default {@code calendar_year})</li>
 *   <li>{@code timestep} — single timestep for parallel preprocessing</li>
 *   <li>{@code defaultValue} — default fill value</li>
 *   <li>{@code parallel} (default {@code false})</li>
 *   <li>{@code amend} (default {@code false})</li>
 * </ul>
 */
public class JoshSimPreprocessBatchHandler implements HttpHandler {

  private static final ExecutorService BATCH_EXECUTOR =
      Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("batch-preprocess-" + thread.threadId());
        thread.setDaemon(true);
        return thread;
      });

  private final CloudApiDataLayer apiDataLayer;

  /**
   * Constructs a new JoshSimPreprocessBatchHandler.
   *
   * @param apiDataLayer The cloud API data layer for API key validation and logging.
   */
  public JoshSimPreprocessBatchHandler(CloudApiDataLayer apiDataLayer) {
    this.apiDataLayer = apiDataLayer;
  }

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
    apiDataLayer.log(apiKey.orElse(""), "preprocessBatch", runtimeSeconds);
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
   * Processes parsed form data for a preprocess batch request.
   *
   * <p>Package-visible for testing.</p>
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

    // Validate required fields
    String[] requiredFields = {
        "jobId", "simulation", "dataFile", "variable", "units", "outputFile", "workDir"
    };
    for (String field : requiredFields) {
      if (!formData.contains(field)) {
        sendJsonError(exchange, 400, "missing-fields",
            "Required fields: jobId, simulation, dataFile, variable, units, outputFile, workDir");
        return Optional.of(apiKey);
      }
    }

    String jobId = formData.getFirst("jobId").getValue();
    File workDir = new File(formData.getFirst("workDir").getValue());

    Optional<String> stagingError = stageInputsIfRequested(formData, jobId, workDir);
    if (stagingError.isPresent()) {
      sendJsonError(exchange, stagingError.get().startsWith("5") ? 500 : 400, jobId,
          stagingError.get().substring(4));
      return Optional.of(apiKey);
    }

    if (!workDir.exists() || !workDir.isDirectory()) {
      sendJsonError(exchange, 400, jobId,
          "workDir does not exist or is not a directory: " + workDir.getPath());
      return Optional.of(apiKey);
    }

    MinioHandler statusMinio = initStatusMinio(jobId);
    String simulation = formData.getFirst("simulation").getValue();
    String dataFile = formData.getFirst("dataFile").getValue();
    String variable = formData.getFirst("variable").getValue();
    String units = formData.getFirst("units").getValue();
    String outputFile = formData.getFirst("outputFile").getValue();

    // Optional preprocess fields
    String crs = getFormValue(formData, "crs", "EPSG:4326");
    String horizCoord = getFormValue(formData, "xCoord", "lon");
    String vertCoord = getFormValue(formData, "yCoord", "lat");
    String timeDim = getFormValue(formData, "timeDim", "calendar_year");
    String timestep = getFormValue(formData, "timestep", null);
    String defaultValue = getFormValue(formData, "defaultValue", null);
    boolean parallel = "true".equalsIgnoreCase(getFormValue(formData, "parallel", "false"));
    boolean amend = "true".equalsIgnoreCase(getFormValue(formData, "amend", "false"));

    String statusPath = "batch-status/" + jobId + "/status.json";
    sendJsonAccepted(exchange, jobId, statusPath);

    String capturedApiKey = apiKey;
    CompletableFuture.runAsync(() -> {
      runPreprocessWithStatus(
          statusMinio, jobId, simulation, workDir, dataFile, variable, units,
          outputFile, crs, horizCoord, vertCoord, timeDim, timestep, defaultValue,
          parallel, amend, statusPath, capturedApiKey
      );
    }, BATCH_EXECUTOR);

    return Optional.of(apiKey);
  }

  private void runPreprocessWithStatus(MinioHandler statusMinio, String jobId,
      String simulation, File workDir, String dataFile, String variable, String units,
      String outputFile, String crs, String horizCoord, String vertCoord, String timeDim,
      String timestep, String defaultValue, boolean parallel, boolean amend,
      String statusPath, String apiKey) {
    writeStatus(statusMinio, statusPath, buildStatusJson(
        "running", jobId, "startedAt", Instant.now().toString()
    ));

    try {
      executePreprocessJob(jobId, simulation, workDir, dataFile, variable, units,
          outputFile, crs, horizCoord, vertCoord, timeDim, timestep, defaultValue, parallel, amend);

      // Upload result .jshd to MinIO
      uploadResult(jobId, workDir, outputFile);

      writeStatus(statusMinio, statusPath, buildStatusJson(
          "complete", jobId, "completedAt", Instant.now().toString(),
          "resultPath", "batch-jobs/" + jobId + "/outputs/" + outputFile
      ));
    } catch (Exception e) {
      SecurityUtil.logSecureError(apiDataLayer, apiKey, "preprocessBatch", e, null);

      String safeMessage = e.getMessage() != null
          ? e.getMessage().replace("\"", "\\\"").replace("\n", " ")
          : "unknown error";
      writeStatus(statusMinio, statusPath, buildStatusJson(
          "error", jobId, "failedAt", Instant.now().toString(), "message", safeMessage
      ));
    }
  }

  private void executePreprocessJob(String jobId, String simulation, File workDir,
      String dataFile, String variable, String units, String outputFile,
      String crs, String horizCoord, String vertCoord, String timeDim,
      String timestep, String defaultValue, boolean parallel, boolean amend)
      throws Exception {

    File scriptFile = LocalFileUtil.findScriptFile(workDir);
    File dataFilePath = new File(workDir, dataFile);
    File outputFilePath = new File(workDir, outputFile);

    PreprocessUtil.PreprocessOptions options = new PreprocessUtil.PreprocessOptions(
        crs, horizCoord, vertCoord, timeDim,
        timestep != null ? timestep : "", defaultValue, parallel, amend
    );

    PreprocessUtil.preprocess(
        scriptFile, simulation, dataFilePath.getPath(), variable, units,
        outputFilePath, options, new OutputOptions()
    );
  }

  private void uploadResult(String jobId, File workDir, String outputFile) throws Exception {
    File resultFile = new File(workDir, outputFile);
    if (!resultFile.exists()) {
      throw new IOException("Preprocessing completed but output file not found: " + resultFile);
    }

    try {
      MinioHandler minio = new MinioHandler(new MinioOptions(), new OutputOptions());
      String objectPath = "batch-jobs/" + jobId + "/outputs/" + outputFile;
      if (!minio.uploadFile(resultFile, objectPath)) {
        throw new IOException("Failed to upload result to MinIO: " + objectPath);
      }
    } catch (Exception e) {
      throw new IOException("Failed to upload result to MinIO: " + e.getMessage(), e);
    }
  }

  private Optional<String> stageInputsIfRequested(FormData formData, String jobId, File workDir) {
    boolean shouldStage = formData.contains("stageFromMinio")
        && "true".equalsIgnoreCase(formData.getFirst("stageFromMinio").getValue());

    if (!shouldStage) {
      return Optional.empty();
    }

    if (!formData.contains("minioPrefix")) {
      return Optional.of("400:minioPrefix is required when stageFromMinio=true");
    }

    String minioPrefix = formData.getFirst("minioPrefix").getValue();
    try {
      if (!workDir.exists() && !workDir.mkdirs()) {
        return Optional.of("400:Failed to create workDir: " + workDir.getPath());
      }
      MinioOptions minioOptions = new MinioOptions();
      MinioHandler minio = new MinioHandler(minioOptions, new OutputOptions());
      MinioStagingUtil.stageFromMinio(minio, minioPrefix, workDir, new OutputOptions());
    } catch (Exception e) {
      return Optional.of("500:Staging from MinIO failed: " + e.getMessage());
    }

    return Optional.empty();
  }

  private MinioHandler initStatusMinio(String jobId) {
    try {
      return new MinioHandler(new MinioOptions(), new OutputOptions());
    } catch (Exception e) {
      System.err.println("[preprocessBatch] Status tracking unavailable for job " + jobId
          + ": " + e.getMessage());
      return null;
    }
  }

  private static String getFormValue(FormData formData, String field, String defaultValue) {
    if (formData.contains(field)) {
      String value = formData.getFirst(field).getValue();
      if (value != null && !value.isEmpty()) {
        return value;
      }
    }
    return defaultValue;
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
      System.err.println("[preprocessBatch] Failed to write status to " + statusPath
          + ": " + e.getMessage());
    }
  }

  private void sendJsonAccepted(HttpServerExchange exchange, String jobId, String statusPath) {
    exchange.setStatusCode(202);
    exchange.getResponseHeaders().put(new HttpString("Content-Type"), "application/json");
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("status", "accepted");
    fields.put("jobId", jobId);
    fields.put("statusPath", statusPath);
    exchange.getResponseSender().send(toJson(fields));
  }

  private void sendJsonError(HttpServerExchange exchange, int statusCode, String jobId,
      String message) {
    exchange.setStatusCode(statusCode);
    exchange.getResponseHeaders().put(new HttpString("Content-Type"), "application/json");
    String safeMessage = message.replace("\"", "\\\"").replace("\n", " ");
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("status", "error");
    fields.put("jobId", jobId);
    fields.put("message", safeMessage);
    exchange.getResponseSender().send(toJson(fields));
  }

  private String buildStatusJson(String status, String jobId, String... extra) {
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("status", status);
    fields.put("jobId", jobId);
    for (int i = 0; i < extra.length; i += 2) {
      fields.put(extra[i], extra[i + 1]);
    }
    return toJson(fields);
  }

  private static String toJson(Map<String, String> fields) {
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    for (Map.Entry<String, String> entry : fields.entrySet()) {
      if (!first) {
        sb.append(",");
      }
      sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
      first = false;
    }
    sb.append("}");
    return sb.toString();
  }
}
