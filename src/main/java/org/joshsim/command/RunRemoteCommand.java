/**
 * Command line interface handler for running Josh simulations on remote servers.
 *
 * <p>This class implements the 'runRemote' command which executes simulations on a remote
 * JoshSimServer instance via HTTP streaming. It handles file serialization, API authentication,
 * and local persistence of results received from the remote server.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.joshsim.lang.io.ExportFacade;
import org.joshsim.lang.io.ExportFacadeFactory;
import org.joshsim.lang.io.ExportTarget;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.io.JvmInputOutputLayer;
import org.joshsim.lang.io.NamedMap;
import org.joshsim.lang.io.WireConverter;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import org.joshsim.util.WireResponseParser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/**
 * Command handler for executing Josh simulations on remote servers.
 *
 * <p>Processes command line arguments to run a specified simulation on a remote JoshSimServer.
 * Supports HTTP streaming communication, API key authentication, and selective file transmission
 * via the --data option. Results are persisted locally using existing export facades.</p>
 */
@Command(
    name = "runRemote",
    description = "Run a simulation on a remote JoshSimServer"
)
public class RunRemoteCommand implements Callable<Integer> {
  private static final int HTTP_ERROR_CODE = 101;
  private static final int SERIALIZATION_ERROR_CODE = 102;
  private static final int NETWORK_ERROR_CODE = 103;
  private static final int UNKNOWN_ERROR_CODE = 404;

  @Parameters(index = "0", description = "Path to Josh simulation file")
  private File file;

  @Parameters(index = "1", description = "Simulation name to execute")
  private String simulation;

  @Option(
      names = "--endpoint",
      description = "Remote JoshSimServer endpoint URL",
      required = true
  )
  private String endpoint;

  @Option(
      names = "--api-key",
      description = "API key for remote server authentication",
      required = true
  )
  private String apiKey;

  @Option(
      names = "--data",
      description = "External data files to include (format: filename=path)",
      split = ","
  )
  private String[] dataFiles = new String[0];

  @Option(
      names = "--replicate",
      description = "Replicate number",
      defaultValue = "0"
  )
  private int replicateNumber;

  @Option(
      names = "--use-float-64",
      description = "Use double instead of BigDecimal, offering speed but lower precision.",
      defaultValue = "false"
  )
  private boolean useFloat64;

  @Mixin
  private OutputOptions output = new OutputOptions();

  @Mixin
  private MinioOptions minioOptions = new MinioOptions();

  @Override
  public Integer call() {
    try {
      // Validate endpoint URL
      URI endpointUri = validateAndParseEndpoint(endpoint);
      
      // Execute remote simulation
      executeRemoteSimulation(endpointUri);
      
      output.printInfo("Remote simulation completed successfully");
      return 0;
      
    } catch (URISyntaxException e) {
      output.printError("Invalid endpoint URL: " + e.getMessage());
      return HTTP_ERROR_CODE;
    } catch (IOException e) {
      output.printError("Network error: " + e.getMessage());
      return NETWORK_ERROR_CODE;
    } catch (Exception e) {
      output.printError("Unexpected error: " + e.getMessage());
      return UNKNOWN_ERROR_CODE;
    }
  }

  /**
   * Validates and parses the endpoint URL, ensuring it's properly formatted.
   *
   * @param endpoint The endpoint URL string to validate
   * @return A URI object representing the validated endpoint
   * @throws URISyntaxException if the endpoint URL is malformed
   */
  private URI validateAndParseEndpoint(String endpoint) throws URISyntaxException {
    URI uri = new URI(endpoint);
    
    // Ensure the scheme is http or https
    String scheme = uri.getScheme();
    if (scheme == null 
        || (!scheme.equals("http") && !scheme.equals("https"))) {
      throw new URISyntaxException(endpoint, 
          "Endpoint must use http:// or https://");
    }
    
    // Ensure the endpoint ends with /runReplicates
    String path = uri.getPath();
    if (path == null || !path.endsWith("/runReplicates")) {
      // Append /runReplicates if not present
      String newPath = (path == null || path.isEmpty()) 
          ? "/runReplicates" : path + "/runReplicates";
      uri = new URI(scheme, uri.getUserInfo(), uri.getHost(), uri.getPort(), 
          newPath, uri.getQuery(), uri.getFragment());
    }
    
    return uri;
  }

  /**
   * Executes the simulation on the remote server via HTTP streaming.
   *
   * @param endpointUri The validated endpoint URI
   * @throws IOException if network communication fails
   * @throws InterruptedException if the operation is interrupted
   */
  private void executeRemoteSimulation(URI endpointUri) throws IOException, InterruptedException {
    // Create HTTP client with HTTP/2 support
    HttpClient client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    // Create HTTP request
    HttpRequest request = createRemoteRequest(endpointUri);
    
    output.printInfo("Sending simulation request to " + endpointUri);
    
    // Send request and process streaming response
    HttpResponse<Stream<String>> response = client.send(
        request,
        HttpResponse.BodyHandlers.ofLines()
    );
    
    if (response.statusCode() != 200) {
      throw new RuntimeException("Remote execution failed with status: " + response.statusCode());
    }
    
    output.printInfo("Connected to remote server, processing streaming response...");
    processStreamingResponse(response.body());
  }

  /**
   * Creates the HTTP request with simulation code and external data.
   *
   * @param endpointUri The endpoint URI
   * @return The configured HTTP request
   * @throws IOException if file reading fails
   */
  private HttpRequest createRemoteRequest(URI endpointUri) throws IOException {
    // Read simulation file content
    String joshCode = Files.readString(file.toPath(), StandardCharsets.UTF_8);
    
    // Serialize external data files
    String externalDataSerialized = serializeExternalData();
    
    // Build form data
    String formBody = buildFormData(joshCode, simulation, apiKey, externalDataSerialized);
    
    return HttpRequest.newBuilder()
        .uri(endpointUri)
        .header("Content-Type", "application/x-www-form-urlencoded")
        .timeout(Duration.ofMinutes(30))
        .POST(HttpRequest.BodyPublishers.ofString(formBody))
        .build();
  }

  /**
   * Serializes external data files using the wire format.
   * 
   * <p>Converts files specified via --data option into wire format string
   * compatible with remote JoshSimServer. Text files are included as plain text
   * with tab characters replaced by spaces for safety. Binary files are Base64
   * encoded. Format: filename\tbinary_flag\tcontent\t</p>
   *
   * @return Serialized external data string in wire format
   * @throws IOException if file reading fails
   */
  private String serializeExternalData() throws IOException {
    Map<String, String> fileMapping = parseDataFiles(dataFiles);
    StringBuilder serialized = new StringBuilder();
    
    for (Map.Entry<String, String> entry : fileMapping.entrySet()) {
      String filename = entry.getKey();
      String filepath = entry.getValue();
      
      // Read file content
      byte[] content = Files.readAllBytes(Paths.get(filepath));
      boolean isBinary = !isTextFile(filename);
      
      String contentStr;
      if (isBinary) {
        contentStr = Base64.getEncoder().encodeToString(content);
      } else {
        contentStr = new String(content, StandardCharsets.UTF_8)
            .replace("\t", "    "); // Tab replacement for safety
      }
      
      // Format: filename\tbinary_flag\tcontent\t
      serialized.append(filename).append("\t")
                .append(isBinary ? "1" : "0").append("\t")
                .append(contentStr).append("\t");
    }
    
    return serialized.toString();
  }

  /**
   * Determines if a file is text or binary based on its extension.
   * 
   * <p>Uses file extension to determine content type. Text files (.csv, .txt, .jshc, .josh)
   * are transmitted as plain text. All other files are treated as binary and Base64 encoded.</p>
   *
   * @param filename The filename to check
   * @return true if the file is text, false if binary
   */
  private boolean isTextFile(String filename) {
    String extension = filename.substring(filename.lastIndexOf("."));
    
    switch (extension) {
      case ".csv":
      case ".txt":
      case ".jshc":
      case ".josh":
        return true;
      default:
        return false;
    }
  }

  /**
   * Builds the form data for the HTTP request.
   * 
   * <p>Creates URL-encoded form data compatible with JoshSimServer /runReplicates endpoint.
   * Includes simulation code, name, API key, external data, and configuration options.</p>
   *
   * @param joshCode The simulation code
   * @param simulation The simulation name
   * @param apiKey The API key for authentication
   * @param externalData The serialized external data in wire format
   * @return The URL-encoded form request body
   */
  private String buildFormData(String joshCode, String simulation, String apiKey, 
                              String externalData) {
    Map<String, String> formData = new HashMap<>();
    formData.put("code", joshCode);
    formData.put("name", simulation);
    formData.put("replicates", "1"); // Currently supporting single replicate
    formData.put("apiKey", apiKey);
    formData.put("externalData", externalData);
    formData.put("favorBigDecimal", String.valueOf(!useFloat64));
    
    // URL encode the form data
    StringBuilder formBody = new StringBuilder();
    boolean first = true;
    for (Map.Entry<String, String> entry : formData.entrySet()) {
      if (!first) {
        formBody.append("&");
      }
      first = false;
      
      try {
        String encodedKey = java.net.URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
        String encodedValue = java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
        formBody.append(encodedKey).append("=").append(encodedValue);
      } catch (Exception e) {
        throw new RuntimeException("Failed to encode form data", e);
      }
    }
    
    return formBody.toString();
  }

  /**
   * Processes streaming HTTP response from remote server.
   * 
   * <p>Parses each response line using WireResponseParser and persists simulation
   * data using the configured ExportFacade. Supports real-time progress feedback
   * and error handling for remote execution failures.</p>
   *
   * @param responseStream Stream of response lines from remote server
   * @throws RuntimeException if response parsing or data persistence fails
   */
  private void processStreamingResponse(Stream<String> responseStream) {
    // Initialize export system using Component 2 infrastructure
    InputOutputLayer ioLayer = new JvmInputOutputLayer(replicateNumber);
    ExportFacadeFactory exportFactory = ioLayer.getExportFacadeFactory();
    
    // Map to track export facades per entity name to support multiple data types
    Map<String, ExportFacade> exportFacades = new HashMap<>();
    
    AtomicLong currentStep = new AtomicLong(0);
    AtomicInteger completedReplicates = new AtomicInteger(0);
    
    try {
      responseStream.forEach(line -> {
        try {
          // Parse line using WireResponseParser
          WireResponseParser.ParsedResponse parsed = 
              WireResponseParser.parseEngineResponse(line.trim());
          
          switch (parsed.getType()) {
            case DATUM:
              // Deserialize wire format to NamedMap using Component 1
              NamedMap namedMap = WireConverter.deserializeFromString(
                  parsed.getDataLine());
              
              // Get or create export facade for this entity type
              String entityName = namedMap.getName();
              ExportFacade exportFacade = exportFacades.get(entityName);
              if (exportFacade == null) {
                // Create export target for CSV output
                ExportTarget target = new ExportTarget("file", entityName + ".csv");
                exportFacade = exportFactory.build(target);
                exportFacade.start();
                exportFacades.put(entityName, exportFacade);
              }
              
              // Persist using Component 2 NamedMap write capability
              exportFacade.write(namedMap, currentStep.get());
              break;
              
            case PROGRESS:
              currentStep.set(parsed.getStepCount());
              output.printInfo(String.format("Progress: step %d", 
                  currentStep.get()));
              break;
              
            case END:
              completedReplicates.incrementAndGet();
              output.printInfo("Replicate completed");
              break;
              
            case ERROR:
              throw new RuntimeException("Remote execution error: " 
                  + parsed.getErrorMessage());
              
            default:
              break;
          }
          
        } catch (Exception e) {
          throw new RuntimeException("Failed to process streaming response: " 
              + e.getMessage(), e);
        }
      });
      
    } finally {
      // Ensure all export facades are properly closed
      for (ExportFacade facade : exportFacades.values()) {
        try {
          facade.join();
        } catch (Exception e) {
          output.printError("Failed to close export facade: " + e.getMessage());
        }
      }
    }
    
    output.printInfo("Results saved locally via export facade");
  }
  

  /**
   * Parses the data files option into a mapping from filename to path.
   *
   * @param dataFiles Array of data file specifications in format "filename=path"
   * @return Map from filename to path
   * @throws IllegalArgumentException if any data file specification is invalid
   */
  private Map<String, String> parseDataFiles(String[] dataFiles) {
    Map<String, String> mapping = new HashMap<>();
    for (String dataFile : dataFiles) {
      String[] parts = dataFile.split("=", 2);
      if (parts.length != 2) {
        throw new IllegalArgumentException("Invalid data file format: " + dataFile
            + ". Expected format: filename=path");
      }
      mapping.put(parts[0].trim(), parts[1].trim());
    }
    return mapping;
  }
}