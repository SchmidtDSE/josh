/**
 * Builder for constructing RunRemoteContext instances.
 *
 * <p>This class follows the builder pattern to construct RunRemoteContext objects
 * with all necessary parameters for remote simulation execution. It provides a fluent
 * interface for setting parameters and ensures proper construction of the context object.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.remote;

import java.io.File;
import java.net.URI;
import java.util.Optional;
import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import org.joshsim.util.ProgressCalculator;
import org.joshsim.util.SimulationMetadata;

/**
 * Builder for creating RunRemoteContext instances with a fluent interface.
 *
 * <p>This builder follows the pattern established in the codebase for constructing
 * complex objects with multiple parameters. It provides method chaining for easy
 * configuration and ensures all required parameters are set before building.</p>
 */
public class RunRemoteContextBuilder {

  // Basic simulation parameters
  private Optional<File> file = Optional.empty();
  private Optional<String> simulation = Optional.empty();
  private boolean useFloat64 = false;

  // Job configuration
  private Optional<JoshJob> job = Optional.empty();

  // Remote endpoint configuration
  private Optional<URI> endpointUri = Optional.empty();
  private Optional<String> apiKey = Optional.empty();

  // Josh simulation code and external data
  private Optional<String> joshCode = Optional.empty();
  private Optional<String> externalDataSerialized = Optional.empty();

  // Progress tracking and metadata
  private Optional<SimulationMetadata> metadata = Optional.empty();
  private Optional<ProgressCalculator> progressCalculator = Optional.empty();

  // Output and configuration options
  private Optional<OutputOptions> outputOptions = Optional.empty();
  private Optional<MinioOptions> minioOptions = Optional.empty();

  // Execution parameters for local leader mode
  private int maxConcurrentWorkers = 10;
  private int replicateNumber = 0;

  /**
   * Set the Josh simulation file.
   *
   * @param file The Josh simulation file
   * @return This builder instance for chaining
   */
  public RunRemoteContextBuilder withFile(File file) {
    this.file = Optional.ofNullable(file);
    return this;
  }

  /**
   * Set the simulation name to execute.
   *
   * @param simulation The simulation name to execute
   * @return This builder instance for chaining
   */
  public RunRemoteContextBuilder withSimulation(String simulation) {
    this.simulation = Optional.ofNullable(simulation);
    return this;
  }

  /**
   * Set the job configuration.
   *
   * @param job The JoshJob containing file mappings and replicate count
   * @return This builder instance for chaining
   */
  public RunRemoteContextBuilder withJob(JoshJob job) {
    this.job = Optional.ofNullable(job);
    return this;
  }

  /**
   * Set whether to use double precision instead of BigDecimal.
   *
   * @param useFloat64 Whether to use double precision
   * @return This builder instance for chaining
   */
  public RunRemoteContextBuilder withUseFloat64(boolean useFloat64) {
    this.useFloat64 = useFloat64;
    return this;
  }

  /**
   * Set the validated endpoint URI.
   *
   * @param endpointUri The validated endpoint URI
   * @return This builder instance for chaining
   */
  public RunRemoteContextBuilder withEndpointUri(URI endpointUri) {
    this.endpointUri = Optional.ofNullable(endpointUri);
    return this;
  }

  /**
   * Set the API key for authentication.
   *
   * @param apiKey The API key for authentication
   * @return This builder instance for chaining
   */
  public RunRemoteContextBuilder withApiKey(String apiKey) {
    this.apiKey = Optional.ofNullable(apiKey);
    return this;
  }


  /**
   * Set the Josh simulation code content.
   *
   * @param joshCode The Josh simulation code content
   * @return This builder instance for chaining
   */
  public RunRemoteContextBuilder withJoshCode(String joshCode) {
    this.joshCode = Optional.ofNullable(joshCode);
    return this;
  }

  /**
   * Set the serialized external data.
   *
   * @param externalDataSerialized The serialized external data
   * @return This builder instance for chaining
   */
  public RunRemoteContextBuilder withExternalDataSerialized(String externalDataSerialized) {
    this.externalDataSerialized = Optional.ofNullable(externalDataSerialized);
    return this;
  }

  /**
   * Set the simulation metadata for progress tracking.
   *
   * @param metadata The simulation metadata
   * @return This builder instance for chaining
   */
  public RunRemoteContextBuilder withMetadata(SimulationMetadata metadata) {
    this.metadata = Optional.ofNullable(metadata);
    return this;
  }

  /**
   * Set the progress calculator instance.
   *
   * @param progressCalculator The progress calculator instance
   * @return This builder instance for chaining
   */
  public RunRemoteContextBuilder withProgressCalculator(ProgressCalculator progressCalculator) {
    this.progressCalculator = Optional.ofNullable(progressCalculator);
    return this;
  }

  /**
   * Set the output options for logging.
   *
   * @param outputOptions The output options
   * @return This builder instance for chaining
   */
  public RunRemoteContextBuilder withOutputOptions(OutputOptions outputOptions) {
    this.outputOptions = Optional.ofNullable(outputOptions);
    return this;
  }

  /**
   * Set the MinIO options for cloud storage.
   *
   * @param minioOptions The MinIO options
   * @return This builder instance for chaining
   */
  public RunRemoteContextBuilder withMinioOptions(MinioOptions minioOptions) {
    this.minioOptions = Optional.ofNullable(minioOptions);
    return this;
  }

  /**
   * Set the maximum concurrent workers for local leader mode.
   *
   * @param maxConcurrentWorkers Maximum concurrent workers
   * @return This builder instance for chaining
   */
  public RunRemoteContextBuilder withMaxConcurrentWorkers(int maxConcurrentWorkers) {
    this.maxConcurrentWorkers = maxConcurrentWorkers;
    return this;
  }

  /**
   * Set the starting replicate number offset.
   *
   * @param replicateNumber Starting replicate number offset
   * @return This builder instance for chaining
   */
  public RunRemoteContextBuilder withReplicateNumber(int replicateNumber) {
    this.replicateNumber = replicateNumber;
    return this;
  }

  /**
   * Build the RunRemoteContext instance.
   *
   * @return A new RunRemoteContext instance with the configured parameters
   * @throws IllegalStateException if any required parameters are missing
   */
  public RunRemoteContext build() {
    validateRequiredParameters();

    return new RunRemoteContext(
        file.get(), simulation.get(), useFloat64,
        endpointUri.get(), apiKey.get(), job.get(),
        joshCode.get(), externalDataSerialized.get(),
        metadata.get(), progressCalculator.get(),
        outputOptions.get(), minioOptions.get(),
        maxConcurrentWorkers, replicateNumber
    );
  }

  /**
   * Validates that all required parameters have been set.
   *
   * @throws IllegalStateException if any required parameter is null
   */
  private void validateRequiredParameters() {
    if (!file.isPresent()) {
      throw new IllegalStateException("File is required");
    }
    if (!simulation.isPresent()) {
      throw new IllegalStateException("Simulation name is required");
    }
    if (!job.isPresent()) {
      throw new IllegalStateException("Job is required");
    }
    if (!endpointUri.isPresent()) {
      throw new IllegalStateException("Endpoint URI is required");
    }
    if (!apiKey.isPresent()) {
      throw new IllegalStateException("API key is required");
    }
    if (!joshCode.isPresent()) {
      throw new IllegalStateException("Josh code is required");
    }
    if (!externalDataSerialized.isPresent()) {
      throw new IllegalStateException("External data serialized is required");
    }
    if (!metadata.isPresent()) {
      throw new IllegalStateException("Metadata is required");
    }
    if (!progressCalculator.isPresent()) {
      throw new IllegalStateException("Progress calculator is required");
    }
    if (!outputOptions.isPresent()) {
      throw new IllegalStateException("Output options are required");
    }
    if (!minioOptions.isPresent()) {
      throw new IllegalStateException("Minio options are required");
    }
  }
}
