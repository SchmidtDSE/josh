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
  public RunRemoteContext(File file, String simulation, int replicateNumber, boolean useFloat64,
                         URI endpointUri, String apiKey, String[] dataFiles,
                         String joshCode, String externalDataSerialized,
                         SimulationMetadata metadata, ProgressCalculator progressCalculator,
                         OutputOptions outputOptions, MinioOptions minioOptions,
                         int maxConcurrentWorkers) {
    this.file = file;
    this.simulation = simulation;
    this.replicateNumber = replicateNumber;
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

  public File getFile() {
    return file;
  }

  public String getSimulation() {
    return simulation;
  }

  public int getReplicateNumber() {
    return replicateNumber;
  }

  public boolean isUseFloat64() {
    return useFloat64;
  }

  public URI getEndpointUri() {
    return endpointUri;
  }

  public String getApiKey() {
    return apiKey;
  }

  public String[] getDataFiles() {
    return dataFiles;
  }

  public String getJoshCode() {
    return joshCode;
  }

  public String getExternalDataSerialized() {
    return externalDataSerialized;
  }

  public SimulationMetadata getMetadata() {
    return metadata;
  }

  public ProgressCalculator getProgressCalculator() {
    return progressCalculator;
  }

  public OutputOptions getOutputOptions() {
    return outputOptions;
  }

  public MinioOptions getMinioOptions() {
    return minioOptions;
  }

  public int getMaxConcurrentWorkers() {
    return maxConcurrentWorkers;
  }
}