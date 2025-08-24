/**
 * Parameter object containing context for remote execution strategies.
 *
 * <p>This class encapsulates all the parameters needed for remote simulation execution,
 * providing a clean interface between RunRemoteCommand and strategy implementations.
 * It includes endpoint information, authentication, simulation data, and configuration options.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.net.URI;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import org.joshsim.util.ProgressCalculator;
import org.joshsim.util.SimulationMetadata;

/**
 * Context object containing all parameters needed for remote execution.
 *
 * <p>This class follows the parameter object pattern to reduce the number of
 * method parameters and provide a stable interface for strategy implementations.
 * It contains endpoint configuration, authentication, simulation data, and
 * execution options.</p>
 */
public class RunRemoteContext {

  // Basic simulation parameters
  private final File file;
  private final String simulation;
  private final int replicateNumber;
  private final int replicates;
  private final boolean useFloat64;

  // Remote endpoint configuration
  private final URI endpointUri;
  private final String apiKey;
  private final String[] dataFiles;

  // Josh simulation code and external data
  private final String joshCode;
  private final String externalDataSerialized;

  // Progress tracking and metadata
  private final SimulationMetadata metadata;
  private final ProgressCalculator progressCalculator;

  // Output and configuration options
  private final OutputOptions outputOptions;
  private final MinioOptions minioOptions;

  // Execution parameters for local leader mode
  private final int maxConcurrentWorkers;

  /**
   * Creates a new RunRemoteContext with all necessary parameters.
   *
   * @param file The Josh simulation file
   * @param simulation The simulation name to execute
   * @param replicateNumber The replicate number
   * @param replicates The number of replicates to run
   * @param useFloat64 Whether to use double precision instead of BigDecimal
   * @param endpointUri The validated endpoint URI
   * @param apiKey The API key for authentication
   * @param dataFiles Array of external data file specifications
   * @param joshCode The Josh simulation code content
   * @param externalDataSerialized The serialized external data
   * @param metadata The simulation metadata for progress tracking
   * @param progressCalculator The progress calculator instance
   * @param outputOptions The output options for logging
   * @param minioOptions The MinIO options for cloud storage
   * @param maxConcurrentWorkers Maximum concurrent workers for local leader mode
   */
  public RunRemoteContext(File file, String simulation, int replicateNumber, int replicates,
      boolean useFloat64, URI endpointUri, String apiKey, String[] dataFiles,
      String joshCode, String externalDataSerialized,
      SimulationMetadata metadata, ProgressCalculator progressCalculator,
      OutputOptions outputOptions, MinioOptions minioOptions,
      int maxConcurrentWorkers) {
    this.file = file;
    this.simulation = simulation;
    this.replicateNumber = replicateNumber;
    this.replicates = replicates;
    this.useFloat64 = useFloat64;
    this.endpointUri = endpointUri;
    this.apiKey = apiKey;
    this.dataFiles = dataFiles;
    this.joshCode = joshCode;
    this.externalDataSerialized = externalDataSerialized;
    this.metadata = metadata;
    this.progressCalculator = progressCalculator;
    this.outputOptions = outputOptions;
    this.minioOptions = minioOptions;
    this.maxConcurrentWorkers = maxConcurrentWorkers;
  }

  /**
   * Gets the Josh simulation file.
   *
   * @return The file containing the Josh simulation code
   */
  public File getFile() {
    return file;
  }

  /**
   * Gets the simulation name to execute.
   *
   * @return The name of the simulation to run
   */
  public String getSimulation() {
    return simulation;
  }

  /**
   * Gets the replicate number for this execution.
   *
   * @return The replicate number
   */
  public int getReplicateNumber() {
    return replicateNumber;
  }

  /**
   * Gets the number of replicates to run.
   *
   * @return The number of replicates
   */
  public int getReplicates() {
    return replicates;
  }

  /**
   * Checks if double precision should be used instead of BigDecimal.
   *
   * @return true if double precision should be used, false for BigDecimal
   */
  public boolean isUseFloat64() {
    return useFloat64;
  }

  /**
   * Gets the validated endpoint URI for remote execution.
   *
   * @return The URI of the remote Josh simulation endpoint
   */
  public URI getEndpointUri() {
    return endpointUri;
  }

  /**
   * Gets the API key for authentication.
   *
   * @return The API key used for remote endpoint authentication
   */
  public String getApiKey() {
    return apiKey;
  }

  /**
   * Gets the external data file specifications.
   *
   * @return Array of external data file specifications, may be null
   */
  public String[] getDataFiles() {
    return dataFiles;
  }

  /**
   * Gets the Josh simulation code content.
   *
   * @return The complete Josh simulation code as a string
   */
  public String getJoshCode() {
    return joshCode;
  }

  /**
   * Gets the serialized external data.
   *
   * @return The external data in serialized format, may be null
   */
  public String getExternalDataSerialized() {
    return externalDataSerialized;
  }

  /**
   * Gets the simulation metadata for progress tracking.
   *
   * @return The simulation metadata containing step count and other tracking information
   */
  public SimulationMetadata getMetadata() {
    return metadata;
  }

  /**
   * Gets the progress calculator instance.
   *
   * @return The progress calculator for tracking simulation progress
   */
  public ProgressCalculator getProgressCalculator() {
    return progressCalculator;
  }

  /**
   * Gets the output options for logging and display.
   *
   * @return The output options configuration
   */
  public OutputOptions getOutputOptions() {
    return outputOptions;
  }

  /**
   * Gets the MinIO options for cloud storage.
   *
   * @return The MinIO options configuration, may be null
   */
  public MinioOptions getMinioOptions() {
    return minioOptions;
  }

  /**
   * Gets the maximum number of concurrent workers for local leader mode.
   *
   * @return The maximum concurrent workers allowed
   */
  public int getMaxConcurrentWorkers() {
    return maxConcurrentWorkers;
  }
}
