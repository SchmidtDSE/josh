/**
 * Handler for individual simulation replicate tasks.
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
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.apache.sis.referencing.CRS;
import org.joshsim.JoshSimFacadeUtil;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.geo.geometry.EarthGeometryFactory;
import org.joshsim.lang.interpret.JoshProgram;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.io.SandboxExportCallback;
import org.joshsim.lang.io.SandboxInputOutputLayer;
import org.joshsim.lang.io.VirtualFile;
import org.joshsim.lang.parse.ParseResult;


/**
 * Handler logic which executes individual simulation replicates and responds via HTTP 2 streaming.
 */
public class JoshSimWorkerHandler implements HttpHandler {

  private final CloudApiDataLayer apiDataLayer;
  private final EngineGeometryFactory geometryFactory;
  private final boolean useSerial;


  /**
   * Constructs a new JoshSimWorkerHandler.
   *
   * @param apiInternalLayer The cloud API data layer utilized by this handler for API operations.
   * @param sandboxed A boolean flag indicating whether to operate in sandboxed mode.
   * @param crs An optional string representing the coordinate reference system to be used. If not
   *     given, will use grid-space.
   * @param useSerial If true, patch processing will be done serially or, otherwise, parallel
   *     processing will be used.
   * @throws RuntimeException if not in sandboxed mode or if there is an error decoding the CRS.
   */
  public JoshSimWorkerHandler(CloudApiDataLayer apiInternalLayer, boolean sandboxed,
        Optional<String> crs, boolean useSerial) {
    this.apiDataLayer = apiInternalLayer;
    this.useSerial = useSerial;

    if (!sandboxed) {
      throw new RuntimeException("Only sandboxed mode is supported at this time.");
    }

    if (crs.isPresent()) {
      try {
        geometryFactory = new EarthGeometryFactory(CRS.forCode(crs.get()));
      } catch (Exception e) {
        throw new RuntimeException("Failed to parse CRS: " + e);
      }
    } else {
      geometryFactory = new GridGeometryFactory();
    }
  }

  /**
   * Simulate a single replicate and stream the results via HTTP 2.
   *
   * <p>Execute a single replicate using the form-encoded code found in the code input on the
   * request and stream any results back. This acts like a facade in that it uses JoshSimFacadeUtil
   * to interpret the program (returning an invalid user input status code and error message if an
   * issue was encountered) and runs the simulation with patches processed in parallel. The code
   * for the simulation is read from form-encoded code field and the simulation name from the name
   * form-encoded field.</p>
   *
   * @param httpServerExchange The exchange through which this request should execute.
   */
  @Override
  public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
    if (httpServerExchange.isInIoThread()) {
      httpServerExchange.dispatch(this);
      return;
    }

    if (!CorsUtil.addCorsHeaders(httpServerExchange)) {
      return;
    }

    long startTime = System.nanoTime();
    Optional<String> apiKey = handleRequestTrusted(httpServerExchange);
    long endTime = System.nanoTime();

    long runtimeSeconds = (endTime - startTime) / 1_000_000_000;
    apiDataLayer.log(apiKey.orElse(""), "simulate", runtimeSeconds);
  }

  /**
   * Execute a request without interacting with the API service internals.
   *
   * <p>Execute a request without interactingw ith the API service inernals as described in
   * handleRequest which checks the API key and reports logging.</p>
   *
   * @param httpServerExchange The exchange through which this request should execute.
   * @returns The API key used in the request or empty string if rejected.
   */
  public Optional<String> handleRequestTrusted(HttpServerExchange httpServerExchange) {
    if (!httpServerExchange.getRequestMethod().equalToString("POST")) {
      httpServerExchange.setStatusCode(405);
      return Optional.empty();
    }

    httpServerExchange.setStatusCode(200);
    httpServerExchange.getResponseHeaders().put(new HttpString("Content-Type"), "text/plain");
    httpServerExchange.startBlocking();

    FormDataParser parser = FormParserFactory.builder().build().createParser(httpServerExchange);
    if (parser == null) {
      httpServerExchange.setStatusCode(400);
      return Optional.empty();
    }

    FormData formData = null;
    try {
      formData = parser.parseBlocking();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    ApiKeyUtil.ApiCheckResult apiCheckResult = ApiKeyUtil.checkApiKey(formData, apiDataLayer);
    if (!apiCheckResult.getKeyIsValid()) {
      httpServerExchange.setStatusCode(401);
      return Optional.empty();
    }
    String apiKey = apiCheckResult.getApiKey();

    boolean hasCode = formData.contains("code");
    boolean hasName = formData.contains("name");
    boolean hasExternalData = formData.contains("externalData");
    boolean hasFavorBigDecimal = formData.contains("favorBigDecimal");
    boolean hasRequired = hasCode && hasName && hasExternalData && hasFavorBigDecimal;
    if (!hasRequired) {
      httpServerExchange.setStatusCode(400);
      return Optional.of(apiKey);
    }

    String code = formData.getFirst("code").getValue();
    String simulationName = formData.getFirst("name").getValue();
    String externalData = formData.getFirst("externalData").getValue();
    boolean favorBigDecimal = Boolean.parseBoolean(
        formData.getFirst("favorBigDecimal").getValue()
    );

    ParseResult result = JoshSimFacadeUtil.parse(code);
    if (result.hasErrors()) {
      httpServerExchange.setStatusCode(400);
      httpServerExchange.getResponseHeaders().put(new HttpString("Content-Type"), "text/plain");
      httpServerExchange.getResponseSender().send(result.getErrors().iterator().next().toString());
      return Optional.of(apiKey);
    }

    try {
      InputOutputLayer inputOutputLayer = getLayer(httpServerExchange, externalData);
      EngineValueFactory valueFactory = new EngineValueFactory(favorBigDecimal);
      JoshProgram program = JoshSimFacadeUtil.interpret(
          valueFactory,
          geometryFactory,
          result,
          inputOutputLayer
      );

      if (!program.getSimulations().hasPrototype(simulationName)) {
        httpServerExchange.setStatusCode(404);
        return Optional.of(apiKey);
      }

      InputOutputLayer layer = getLayer(httpServerExchange, externalData);
      JoshSimFacadeUtil.runSimulation(
          valueFactory,
          geometryFactory,
          layer,
          program,
          simulationName,
          (step) -> {}, // No step reporting needed for worker
          useSerial
      );
    } catch (RuntimeException e) {
      httpServerExchange.setStatusCode(500);
      httpServerExchange.getResponseHeaders().put(new HttpString("Content-Type"), "text/plain");
      
      String errorMessage = buildInformativeErrorMessage(e);
      httpServerExchange.getResponseSender().send(errorMessage);
      return Optional.of(apiKey);
    } catch (Exception e) {
      httpServerExchange.setStatusCode(500);
      httpServerExchange.getResponseHeaders().put(new HttpString("Content-Type"), "text/plain");
      
      String errorMessage = buildInformativeErrorMessage(e);
      httpServerExchange.getResponseSender().send(errorMessage);
      return Optional.of(apiKey);
    }
    httpServerExchange.endExchange();
    return Optional.of(apiKey);
  }

  /**
   * Create a new input / output layer that writes exports to an exchange.
   *
   * @param httpServerExchange The exchange where the response should be streamed.
   * @param externalData String serialization of the virtual file system.
   * @return The newly created layer.
   */
  private InputOutputLayer getLayer(HttpServerExchange httpServerExchange, String externalData) {
    Map<String, VirtualFile> virtualFiles = VirtualFileSystemWireDeserializer.load(externalData);

    SandboxExportCallback exportCallback = (export) -> {
      try {
        httpServerExchange.getOutputStream().write((export + "\n").getBytes());
        httpServerExchange.getOutputStream().flush();
      } catch (IOException e) {
        throw new RuntimeException("Error streaming response", e);
      }
    };
    return new SandboxInputOutputLayer(virtualFiles, exportCallback);
  }

  /**
   * Build an informative error message for external data loading failures.
   *
   * @param e The exception that occurred during simulation setup or execution.
   * @return A user-friendly error message describing the external data issue.
   */
  private String buildInformativeErrorMessage(Exception e) {
    String originalMessage = e.getMessage() != null ? e.getMessage() : "";
    
    // Handle specific external data error patterns
    if (originalMessage.contains("Cannot find virtual file:")) {
      String fileName = originalMessage.substring(originalMessage.indexOf(": ") + 2);
      return String.format(
          "External data file not found: '%s'. Please ensure the file is included in your external data upload and the filename matches exactly.", 
          fileName
      );
    }
    
    if (originalMessage.contains("CSV must contain 'longitude' and 'latitude' columns")) {
      return "External CSV data is missing required columns. CSV files must contain 'longitude' and 'latitude' columns for geospatial data processing.";
    }
    
    if (originalMessage.contains("Invalid numeric value in column")) {
      return String.format(
          "External data contains invalid numeric values: %s. Please check that all numeric columns contain valid numbers.", 
          originalMessage
      );
    }
    
    if (originalMessage.contains("Failure in loading a jshd resource:")) {
      String innerMessage = originalMessage.substring(originalMessage.indexOf(": ") + 2);
      return String.format(
          "Failed to load external data resource: %s. Please verify the file format and contents are correct.", 
          innerMessage
      );
    }
    
    if (originalMessage.contains("No suitable reader found for file:")) {
      String fileName = originalMessage.substring(originalMessage.indexOf(": ") + 2);
      return String.format(
          "Unsupported external data file format: '%s'. Supported formats include CSV and NetCDF files.", 
          fileName
      );
    }
    
    // Handle IOException from CSV reading
    if (e instanceof IOException || originalMessage.contains("IOException")) {
      if (originalMessage.contains("No such file or directory")) {
        return "External data file could not be accessed. Please ensure the file exists and is properly uploaded.";
      }
      return String.format(
          "Error reading external data file: %s. Please verify the file format and contents.", 
          originalMessage
      );
    }
    
    // Default case for other external data related errors
    if (originalMessage.toLowerCase().contains("external") || 
        originalMessage.toLowerCase().contains("file") ||
        originalMessage.toLowerCase().contains("csv") ||
        originalMessage.toLowerCase().contains("data")) {
      return String.format(
          "External data processing error: %s. Please check your external data files and try again.", 
          originalMessage
      );
    }
    
    // Fallback for unrecognized errors
    return String.format("Simulation error: %s", originalMessage);
  }

}
