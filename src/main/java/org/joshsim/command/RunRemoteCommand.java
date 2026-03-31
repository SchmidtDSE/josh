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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import org.joshsim.JoshSimCommander;
import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.joshsim.pipeline.job.JoshJobFileInfo;
import org.joshsim.pipeline.job.config.JobVariationParser;
import org.joshsim.pipeline.remote.RunRemoteContext;
import org.joshsim.pipeline.remote.RunRemoteContextBuilder;
import org.joshsim.pipeline.remote.RunRemoteLocalLeaderStrategy;
import org.joshsim.pipeline.remote.RunRemoteOffloadLeaderStrategy;
import org.joshsim.pipeline.remote.RunRemoteStrategy;
import org.joshsim.pipeline.remote.batch.BatchJobStrategy;
import org.joshsim.pipeline.remote.batch.KubernetesConfig;
import org.joshsim.pipeline.remote.batch.KubernetesTarget;
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
                   + "Required for HTTP targets (Josh Cloud and custom endpoints). "
                   + "Not required for batch targets (kubernetes, ssh)."
  )
  private String apiKey;

  @Option(
      names = "--data",
      description = "External data files to include (format: filename=path;filename2=path2)"
  )
  private String[] dataFiles = new String[0];

  @Option(
      names = "--custom-tag",
      description = "Custom template parameters (format: name=value). Can be specified "
                  + "multiple times."
  )
  private String[] customTags = new String[0];


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

  @Option(
      names = "--upload-source",
      description = "Upload source .josh file to MinIO after simulation completes",
      defaultValue = "false"
  )
  private boolean uploadSource = false;

  @Option(
      names = "--upload-config",
      description = "Upload config files (.jshc) to MinIO after simulation completes",
      defaultValue = "false"
  )
  private boolean uploadConfig = false;

  @Option(
      names = "--upload-data",
      description = "Upload data files (.jshd) to MinIO after simulation completes",
      defaultValue = "false"
  )
  private boolean uploadData = false;

  // --- Batch target options ---

  @Option(
      names = "--target",
      description = "Execution target: http (default), kubernetes, or ssh.",
      defaultValue = "http"
  )
  private String target = "http";

  @Option(
      names = "--replicates-per-job",
      description = "Sequential replicates per job unit (default: 1). "
                  + "Higher values mean fewer jobs, each running more replicates sequentially.",
      defaultValue = "1"
  )
  private int replicatesPerJob = 1;

  // --- Kubernetes target options ---

  @Option(
      names = "--k8s-namespace",
      description = "Kubernetes namespace (default: default)",
      defaultValue = "default"
  )
  private String k8sNamespace = "default";

  @Option(
      names = "--k8s-context",
      description = "kubectl context name (uses current context if absent)"
  )
  private String k8sContext;

  @Option(
      names = "--k8s-image",
      description = "Container image for batch worker pods (required for kubernetes target)"
  )
  private String k8sImage;

  @Option(
      names = "--k8s-memory",
      description = "Memory resource request (e.g., '256Gi', required for kubernetes target)"
  )
  private String k8sMemory;

  @Option(
      names = "--k8s-cpu",
      description = "CPU resource request (e.g., '64', default: '4')",
      defaultValue = "4"
  )
  private String k8sCpu = "4";

  @Option(
      names = "--k8s-gpu",
      description = "Number of GPUs to request (default: 0)",
      defaultValue = "0"
  )
  private int k8sGpu = 0;

  @Option(
      names = "--k8s-gpu-product",
      description = "GPU product name for node selection (e.g., 'NVIDIA-A100-SXM4-80GB')"
  )
  private String k8sGpuProduct;

  @Option(
      names = "--k8s-timeout",
      description = "Job timeout in seconds (default: 86400 = 24h)",
      defaultValue = "86400"
  )
  private long k8sTimeout = 86400;

  @Option(
      names = "--k8s-backoff-limit",
      description = "Number of retries for failed pods (default: 1)",
      defaultValue = "1"
  )
  private int k8sBackoffLimit = 1;

  @Option(
      names = "--k8s-parallelism",
      description = "Max concurrent pods (default: same as --replicates)",
      defaultValue = "-1"
  )
  private int k8sParallelism = -1;

  /**
   * Parses custom parameter command-line options.
   *
   * @return Map of custom parameter names to values
   * @throws IllegalArgumentException if any custom tag is malformed or uses reserved names
   */
  private Map<String, String> parseCustomParameters() {
    Map<String, String> customParameters = new HashMap<>();
    for (String customTag : customTags) {
      int equalsIndex = customTag.indexOf('=');
      if (equalsIndex <= 0 || equalsIndex == customTag.length() - 1) {
        throw new IllegalArgumentException("Invalid custom-tag format: " + customTag
            + ". Expected format: name=value");
      }
      String name = customTag.substring(0, equalsIndex).trim();
      String value = customTag.substring(equalsIndex + 1);

      // Validate name doesn't conflict with reserved templates
      if ("replicate".equals(name) || "step".equals(name) || "variable".equals(name)) {
        throw new IllegalArgumentException("Custom parameter name '" + name
            + "' conflicts with reserved template variable");
      }

      customParameters.put(name, value);
    }
    return customParameters;
  }

  @Override
  public Integer call() {
    // Validate replicates parameter
    if (replicates < 1) {
      output.printError("Number of replicates must be at least 1");
      return SERIALIZATION_ERROR_CODE;
    }
    try {
      URI endpointUri = null;

      if ("http".equals(target)) {
        // HTTP target: validate endpoint and API key
        if (apiKey == null || apiKey.trim().isEmpty()) {
          output.printError("--api-key is required for HTTP targets.");
          return SERIALIZATION_ERROR_CODE;
        }

        boolean usingJoshCloud = isUsingJoshCloud();

        if (usingJoshCloud) {
          output.printInfo("Using Josh Cloud for remote execution");
          output.printInfo("Your simulation will run on our community infrastructure");
          validateJoshCloudApiKey();
        } else {
          output.printInfo("Using custom endpoint: " + endpoint);
        }

        endpointUri = validateAndParseEndpoint(endpoint);
      } else {
        output.printInfo("Using " + target + " target for batch execution");
      }

      // Execute remote simulation using strategy pattern
      List<JoshJob> jobs = executeRemoteSimulation(endpointUri);

      output.printInfo("Remote simulation completed successfully");

      // Upload artifacts if requested and MinIO is configured
      if (minioOptions.isMinioOutput()) {
        // Upload the josh file if requested
        if (uploadSource) {
          boolean joshSuccess = JoshSimCommander.saveToMinio("run", file, minioOptions, output);
          if (!joshSuccess) {
            return SERIALIZATION_ERROR_CODE;
          }
        }

        // Upload config files if requested
        if (uploadConfig) {
          Integer configResult = uploadArtifacts(jobs, ".jshc", "run");
          if (configResult != 0) {
            return configResult;
          }
        }

        // Upload data files if requested
        if (uploadData) {
          Integer dataResult = uploadArtifacts(jobs, ".jshd", "run");
          if (dataResult != 0) {
            return dataResult;
          }
        }
      }

      return 0;

    } catch (URISyntaxException e) {
      output.printError("Invalid endpoint URL: " + e.getMessage());
      return HTTP_ERROR_CODE;
    } catch (IOException e) {
      if (isUsingJoshCloud()) {
        output.printError("Josh Cloud execution failed: " + getRootCauseMessage(e));
        output.printError("Please check your API key and network connection.");
        output.printError("Visit https://joshsim.org for support.");
      } else {
        output.printError("Custom endpoint execution failed: " + getRootCauseMessage(e));
        output.printError("Please verify your endpoint URL and API key.");
      }
      return NETWORK_ERROR_CODE;
    } catch (Exception e) {
      if (isUsingJoshCloud()) {
        output.printError("Josh Cloud execution failed: " + getRootCauseMessage(e));
        output.printError("Please check your API key and network connection.");
        output.printError("Visit https://joshsim.org for support.");
      } else {
        output.printError("Custom endpoint execution failed: " + getRootCauseMessage(e));
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
   * @return List of jobs executed
   * @throws IOException if network communication fails
   * @throws InterruptedException if the operation is interrupted
   */
  private List<JoshJob> executeRemoteSimulation(URI endpointUri)
      throws IOException, InterruptedException {
    // Extract simulation metadata for progress tracking
    SimulationMetadata metadata = extractSimulationMetadata();

    output.printInfo("Simulation has " + metadata.getTotalSteps() + " steps "
        + "(from step " + metadata.getStepsLow() + " to " + metadata.getStepsHigh() + ")");

    // Parse custom parameters from command line
    Map<String, String> customParameters = parseCustomParameters();

    // Create job configurations using JobVariationParser for grid search
    JoshJobBuilder templateJobBuilder = new JoshJobBuilder()
        .setReplicates(replicates)
        .setCustomParameters(customParameters);
    JobVariationParser parser = new JobVariationParser();
    List<JoshJobBuilder> jobBuilders = parser.parseDataFiles(templateJobBuilder, dataFiles);

    // Build all job instances
    List<JoshJob> jobs = jobBuilders.stream()
        .map(JoshJobBuilder::build)
        .toList();

    // Report grid search information
    output.printInfo("Grid search will execute " + jobs.size() + " job combination(s) "
        + "with " + replicates + " replicate(s) each");
    output.printInfo("Total simulations to run: " + (jobs.size() * replicates));
    output.printInfo("");

    // Read Josh simulation code
    String joshCode = Files.readString(file.toPath(), StandardCharsets.UTF_8);

    // Select execution strategy (same for all job combinations)
    RunRemoteStrategy strategy = selectExecutionStrategy();

    // Execute remote simulation for each job combination
    for (int jobIndex = 0; jobIndex < jobs.size(); jobIndex++) {
      JoshJob currentJob = jobs.get(jobIndex);

      if (jobs.size() > 1) {
        output.printInfo("Simulation " + (jobIndex + 1) + "/" + jobs.size());
      }

      // Serialize external data for this job combination
      String externalDataSerialized = serializeExternalDataForJob(currentJob);

      // Create a new progress calculator for THIS job combination
      // This ensures replicate numbers are 1/N for each simulation, not cumulative
      ProgressCalculator progressCalculator = new ProgressCalculator(
          metadata.getTotalSteps(), replicates
      );

      // Create execution context for this job combination
      RunRemoteContextBuilder contextBuilder = new RunRemoteContextBuilder()
          .withFile(file)
          .withSimulation(simulation)
          .withUseFloat64(useFloat64)
          .withJob(currentJob)
          .withJoshCode(joshCode)
          .withMetadata(metadata)
          .withProgressCalculator(progressCalculator)
          .withOutputOptions(output)
          .withMinioOptions(minioOptions)
          .withMaxConcurrentWorkers(concurrentWorkers)
          .withTargetType(target)
          .withReplicatesPerJob(replicatesPerJob);

      // HTTP-specific fields
      if ("http".equals(target)) {
        contextBuilder
            .withEndpointUri(endpointUri)
            .withApiKey(apiKey)
            .withExternalDataSerialized(externalDataSerialized);
      }

      // Kubernetes-specific fields
      if ("kubernetes".equals(target)) {
        contextBuilder.withKubernetesConfig(buildKubernetesConfig());
      }

      RunRemoteContext context = contextBuilder.build();

      // Execute strategy for this job combination
      strategy.execute(context);
    }

    return jobs;
  }

  /**
   * Selects the appropriate execution strategy based on command line options.
   *
   * @return The execution strategy to use
   */
  private RunRemoteStrategy selectExecutionStrategy() {
    switch (target) {
      case "kubernetes":
        return new BatchJobStrategy(
            new KubernetesTarget(buildKubernetesConfig(), output)
        );
      case "http":
      default:
        if (useRemoteLeader) {
          return new RunRemoteOffloadLeaderStrategy();
        } else {
          return new RunRemoteLocalLeaderStrategy();
        }
    }
  }

  /**
   * Builds a KubernetesConfig from CLI flags.
   *
   * @return the Kubernetes configuration
   */
  private KubernetesConfig buildKubernetesConfig() {
    KubernetesConfig.Builder builder = new KubernetesConfig.Builder()
        .setNamespace(k8sNamespace)
        .setCpu(k8sCpu)
        .setGpu(k8sGpu)
        .setTimeoutSeconds(k8sTimeout)
        .setBackoffLimit(k8sBackoffLimit)
        .setParallelism(k8sParallelism);

    if (k8sContext != null) {
      builder.setContext(k8sContext);
    }
    if (k8sImage != null) {
      builder.setImage(k8sImage);
    }
    if (k8sMemory != null) {
      builder.setMemory(k8sMemory);
    }
    if (k8sGpuProduct != null) {
      builder.setGpuProduct(k8sGpuProduct);
    }

    return builder.build();
  }


  /**
   * Serializes external data files for a specific job using the wire format.
   *
   * <p>Converts files from the job's file mappings into wire format string
   * compatible with remote JoshSimServer. Text files are included as plain text
   * with tab characters replaced by spaces for safety. Binary files are Base64
   * encoded. Format: filename\tbinary_flag\tcontent\t</p>
   *
   * @param job The job containing file mappings to serialize
   * @return Serialized external data string in wire format
   * @throws IOException if file reading fails
   */
  private String serializeExternalDataForJob(JoshJob job) throws IOException {
    Map<String, JoshJobFileInfo> fileMapping = job.getFileInfos();
    StringBuilder serialized = new StringBuilder();

    for (Map.Entry<String, JoshJobFileInfo> entry : fileMapping.entrySet()) {
      String filename = entry.getKey();
      String filepath = entry.getValue().getPath();

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
   * Serializes external data files using the wire format (legacy method).
   *
   * <p>This method is kept for backward compatibility but now delegates to
   * serializeExternalDataForJob using the first job combination.</p>
   *
   * @return Serialized external data string in wire format
   * @throws IOException if file reading fails
   * @deprecated Use serializeExternalDataForJob instead
   */
  @Deprecated
  @SuppressWarnings("unused")
  private String serializeExternalData() throws IOException {
    JoshJobBuilder tempBuilder = new JoshJobBuilder();
    JobVariationParser parser = new JobVariationParser();
    List<JoshJobBuilder> jobBuilders = parser.parseDataFiles(tempBuilder, dataFiles);
    JoshJob tempJob = jobBuilders.get(0).build();
    return serializeExternalDataForJob(tempJob);
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
   * Walks the exception chain to find the first non-null, non-empty message.
   *
   * @param e The exception to inspect
   * @return The first meaningful message found, or the exception class name as fallback
   */
  private String getRootCauseMessage(Exception e) {
    Throwable current = e;
    while (current != null) {
      if (current.getMessage() != null && !current.getMessage().isEmpty()) {
        return current.getMessage();
      }
      current = current.getCause();
    }
    return e.getClass().getSimpleName();
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
   * Uploads artifact files with the given extension from all jobs.
   * If no files are found in jobs, scans the working directory for files with the extension.
   *
   * @param jobs List of jobs to extract files from
   * @param extension File extension to match (e.g., ".jshc", ".jshd")
   * @param subDirectories Subdirectory path in MinIO bucket
   * @return 0 if successful, error code otherwise
   */
  private Integer uploadArtifacts(List<JoshJob> jobs, String extension, String subDirectories) {
    // Collect unique file paths across all jobs
    Set<String> uniqueFilePaths = new HashSet<>();
    for (JoshJob job : jobs) {
      for (JoshJobFileInfo fileInfo : job.getFileInfos().values()) {
        if (fileInfo.getPath().endsWith(extension)) {
          uniqueFilePaths.add(fileInfo.getPath());
        }
      }
    }

    // If no files found in jobs (no --data flag used), scan working directory
    if (uniqueFilePaths.isEmpty()) {
      File workingDir = file.getParentFile();
      if (workingDir == null) {
        workingDir = new File(".");
      }

      File[] filesInDir = workingDir.listFiles((dir, name) -> name.endsWith(extension));
      if (filesInDir != null) {
        for (File f : filesInDir) {
          uniqueFilePaths.add(f.getPath());
        }
      }
    }

    // Upload each unique file
    for (String filePath : uniqueFilePaths) {
      File artifactFile = new File(filePath);
      if (artifactFile.exists()) {
        boolean success = JoshSimCommander.saveToMinio(
            subDirectories, artifactFile, minioOptions, output
        );
        if (!success) {
          return SERIALIZATION_ERROR_CODE;
        }
      } else {
        output.printError("Artifact file not found: " + filePath);
      }
    }

    return 0;
  }

}
