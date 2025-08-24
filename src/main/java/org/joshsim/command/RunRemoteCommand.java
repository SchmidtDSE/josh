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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import org.joshsim.util.ProgressCalculator;
import org.joshsim.util.SimulationMetadata;
import org.joshsim.util.SimulationMetadataExtractor;
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
    description = "Run a simulation on Josh Cloud or a custom remote JoshSimServer"
)
public class RunRemoteCommand implements Callable<Integer> {
  private static final String JOSH_CLOUD_ENDPOINT =
      "https://josh-executor-prod-1007495489273.us-west1.run.app";

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
      description = "Remote JoshSimServer endpoint URL. "
                   + "Defaults to Josh Cloud (${DEFAULT-VALUE}) if not specified. "
                   + "Use custom endpoint for your own infrastructure.",
      required = false,
      defaultValue = JOSH_CLOUD_ENDPOINT
  )
  private String endpoint = JOSH_CLOUD_ENDPOINT;

  @Option(
      names = "--api-key",
      description = "API key for authentication. "
                   + "Required for Josh Cloud and custom endpoints. "
                   + "Get Josh Cloud API key from https://joshsim.org",
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

  @Option(
      names = "--remote-leader",
      description = "Use remote leader for coordination (true) or manage locally (false). "
                  + "Default: false. When false, coordinates multiple parallel worker calls "
                  + "locally. When true, offloads all coordination to remote JoshSimLeaderHandler.",
      defaultValue = "false"
  )
  private boolean useRemoteLeader;

  @Option(
      names = "--concurrent-workers",
      description = "Maximum number of concurrent worker requests when using local leader mode. "
                  + "Ignored when --remote-leader=true. Default: 10.",
      defaultValue = "10"
  )
  private int concurrentWorkers = 10;

  @Option(
      names = "--replicates",
      description = "Number of replicates to run",
      defaultValue = "1"
  )
  private int replicates = 1;

  @Override
  public Integer call() {
    // Validate replicates parameter
    if (replicates < 1) {
      output.printError("Number of replicates must be at least 1");
      return SERIALIZATION_ERROR_CODE;
    }
    try {
      // Detect if using Josh Cloud vs custom endpoint
      boolean usingJoshCloud = isUsingJoshCloud();

      if (usingJoshCloud) {
        output.printInfo("Using Josh Cloud for remote execution");
        output.printInfo("Your simulation will run on our community infrastructure");
        validateJoshCloudApiKey();
      } else {
        output.printInfo("Using custom endpoint: " + endpoint);
      }

      // Validate endpoint URL
      URI endpointUri = validateAndParseEndpoint(endpoint);

      // Execute remote simulation using strategy pattern
      executeRemoteSimulation(endpointUri);

      output.printInfo("Remote simulation completed successfully");
      return 0;

    } catch (URISyntaxException e) {
      output.printError("Invalid endpoint URL: " + e.getMessage());
      return HTTP_ERROR_CODE;
    } catch (IOException e) {
      if (isUsingJoshCloud()) {
        output.printError("Josh Cloud execution failed: " + e.getMessage());
        output.printError("Please check your API key and network connection.");
        output.printError("Visit https://joshsim.org for support.");
      } else {
        output.printError("Custom endpoint execution failed: " + e.getMessage());
        output.printError("Please verify your endpoint URL and API key.");
      }
      return NETWORK_ERROR_CODE;
    } catch (Exception e) {
      if (isUsingJoshCloud()) {
        output.printError("Josh Cloud execution failed: " + e.getMessage());
        output.printError("Please check your API key and network connection.");
        output.printError("Visit https://joshsim.org for support.");
      } else {
        output.printError("Custom endpoint execution failed: " + e.getMessage());
        output.printError("Please verify your endpoint URL and API key.");
      }
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
   * Executes the simulation using the strategy pattern.
   *
   * @param endpointUri The validated endpoint URI
   * @throws IOException if network communication fails
   * @throws InterruptedException if the operation is interrupted
   */
  private void executeRemoteSimulation(URI endpointUri) throws IOException, InterruptedException {
    // Extract simulation metadata for progress tracking
    SimulationMetadata metadata = extractSimulationMetadata();

    output.printInfo("Simulation has " + metadata.getTotalSteps() + " steps "
        + "(from step " + metadata.getStepsLow() + " to " + metadata.getStepsHigh() + ")");

    // Initialize progress calculator
    ProgressCalculator progressCalculator = new ProgressCalculator(
        metadata.getTotalSteps(), replicates
    );

    // Read Josh simulation code
    String joshCode = Files.readString(file.toPath(), StandardCharsets.UTF_8);

    // Serialize external data
    String externalDataSerialized = serializeExternalData();

    // Create execution context using builder
    RunRemoteContext context = new RunRemoteContextBuilder()
        .withFile(file)
        .withSimulation(simulation)
        .withReplicateNumber(replicateNumber)
        .withReplicates(replicates)
        .withUseFloat64(useFloat64)
        .withEndpointUri(endpointUri)
        .withApiKey(apiKey)
        .withDataFiles(dataFiles)
        .withJoshCode(joshCode)
        .withExternalDataSerialized(externalDataSerialized)
        .withMetadata(metadata)
        .withProgressCalculator(progressCalculator)
        .withOutputOptions(output)
        .withMinioOptions(minioOptions)
        .withMaxConcurrentWorkers(concurrentWorkers)
        .build();

    // Select and execute strategy
    RunRemoteStrategy strategy = selectExecutionStrategy();
    strategy.execute(context);
  }

  /**
   * Selects the appropriate execution strategy based on command line options.
   *
   * @return The execution strategy to use
   */
  private RunRemoteStrategy selectExecutionStrategy() {
    if (useRemoteLeader) {
      return new RunRemoteOffloadLeaderStrategy();
    } else {
      return new RunRemoteLocalLeaderStrategy();
    }
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
    String extension = filename.substring(filename.lastIndexOf('.'));

    return switch (extension) {
      case ".csv", ".txt", ".jshc", ".josh" -> true;
      default -> false;
    };
  }


  /**
   * Extracts simulation metadata from the input Josh script file.
   *
   * @return SimulationMetadata containing step information
   * @throws RuntimeException if metadata extraction fails
   */
  private SimulationMetadata extractSimulationMetadata() {
    try {
      return SimulationMetadataExtractor.extractMetadata(file, simulation);
    } catch (Exception e) {
      output.printError("Failed to extract simulation metadata, using defaults: " + e.getMessage());
      // Return default metadata if extraction fails
      return new SimulationMetadata(0, 10, 11);
    }
  }



  /**
   * Determines if the current endpoint is Josh Cloud.
   *
   * @return true if using Josh Cloud, false for custom endpoint
   */
  private boolean isUsingJoshCloud() {
    return endpoint.equals(JOSH_CLOUD_ENDPOINT);
  }

  /**
   * Validates Josh Cloud API key format if applicable.
   * Josh Cloud API keys should follow specific format patterns.
   *
   * @throws IllegalArgumentException if Josh Cloud API key format is invalid
   */
  private void validateJoshCloudApiKey() {
    if (isUsingJoshCloud()) {
      // Add Josh Cloud API key validation if needed
      // This could check for specific format requirements
      if (apiKey == null || apiKey.trim().isEmpty()) {
        throw new IllegalArgumentException(
            "API key is required for Josh Cloud. "
            + "Get your API key from https://joshsim.org");
      }

      output.printInfo("Validated Josh Cloud API key");
    }
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
