/**
 * Specification for a batch job to be submitted to a remote target.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.remote.batch;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.joshsim.util.MinioOptions;


/**
 * Immutable specification passed to {@link RemoteBatchTarget#submitJob(BatchJobSpec)}.
 *
 * <p>Contains all information needed for the target to create and configure a batch job,
 * including container image, resource requirements, replicate configuration, and MinIO
 * credentials for input/output staging.</p>
 */
public class BatchJobSpec {

  private final String jobId;
  private final String simulation;
  private final String image;
  private final String inputPrefix;
  private final String outputPrefix;
  private final MinioOptions minioOptions;
  private final String memory;
  private final String cpu;
  private final int gpu;
  private final int totalReplicates;
  private final int replicatesPerJob;
  private final int maxParallelism;
  private final long timeoutSeconds;
  private final boolean useFloat64;
  private final Map<String, String> env;
  private final Map<String, String> labels;

  /**
   * Creates a new BatchJobSpec via the builder.
   *
   * @param builder the builder containing spec values
   */
  private BatchJobSpec(Builder builder) {
    this.jobId = builder.jobId;
    this.simulation = builder.simulation;
    this.image = builder.image;
    this.inputPrefix = builder.inputPrefix;
    this.outputPrefix = builder.outputPrefix;
    this.minioOptions = builder.minioOptions;
    this.memory = builder.memory;
    this.cpu = builder.cpu;
    this.gpu = builder.gpu;
    this.totalReplicates = builder.totalReplicates;
    this.replicatesPerJob = builder.replicatesPerJob;
    this.maxParallelism = builder.maxParallelism;
    this.timeoutSeconds = builder.timeoutSeconds;
    this.useFloat64 = builder.useFloat64;
    this.env = Collections.unmodifiableMap(new HashMap<>(builder.env));
    this.labels = Collections.unmodifiableMap(new HashMap<>(builder.labels));
  }

  /**
   * Get the unique job identifier.
   *
   * @return the job ID
   */
  public String getJobId() {
    return jobId;
  }

  /**
   * Get the simulation name to execute.
   *
   * @return the simulation name
   */
  public String getSimulation() {
    return simulation;
  }

  /**
   * Get the container image for workers.
   *
   * @return the image reference
   */
  public String getImage() {
    return image;
  }

  /**
   * Get the MinIO prefix where inputs are staged.
   *
   * @return the input prefix (e.g., "job-123/input/")
   */
  public String getInputPrefix() {
    return inputPrefix;
  }

  /**
   * Get the MinIO prefix where outputs will be written.
   *
   * @return the output prefix (e.g., "job-123/output/")
   */
  public String getOutputPrefix() {
    return outputPrefix;
  }

  /**
   * Get the MinIO connection options.
   *
   * @return the MinIO options
   */
  public MinioOptions getMinioOptions() {
    return minioOptions;
  }

  /**
   * Get the memory resource request.
   *
   * @return the memory string (e.g., "256Gi")
   */
  public String getMemory() {
    return memory;
  }

  /**
   * Get the CPU resource request.
   *
   * @return the CPU string (e.g., "64")
   */
  public String getCpu() {
    return cpu;
  }

  /**
   * Get the GPU count.
   *
   * @return the GPU count (0 if not needed)
   */
  public int getGpu() {
    return gpu;
  }

  /**
   * Get the total number of replicates to run.
   *
   * @return the total replicate count
   */
  public int getTotalReplicates() {
    return totalReplicates;
  }

  /**
   * Get the number of sequential replicates per job unit.
   *
   * @return replicates per job (default 1)
   */
  public int getReplicatesPerJob() {
    return replicatesPerJob;
  }

  /**
   * Get the maximum number of concurrent job units.
   *
   * @return the max parallelism
   */
  public int getMaxParallelism() {
    return maxParallelism;
  }

  /**
   * Get the job timeout in seconds.
   *
   * @return the timeout in seconds
   */
  public long getTimeoutSeconds() {
    return timeoutSeconds;
  }

  /**
   * Whether to use float64 instead of BigDecimal.
   *
   * @return true if using float64
   */
  public boolean isUseFloat64() {
    return useFloat64;
  }

  /**
   * Get additional environment variables for the worker.
   *
   * @return unmodifiable map of env vars
   */
  public Map<String, String> getEnv() {
    return env;
  }

  /**
   * Get target-specific labels (e.g., K8s node selectors).
   *
   * @return unmodifiable map of labels
   */
  public Map<String, String> getLabels() {
    return labels;
  }

  /**
   * Computes the number of job units needed given totalReplicates and replicatesPerJob.
   *
   * @return the number of job units (ceiling division)
   */
  public int getJobCount() {
    return (totalReplicates + replicatesPerJob - 1) / replicatesPerJob;
  }

  /**
   * Builder for constructing BatchJobSpec instances.
   */
  public static class Builder {
    private String jobId;
    private String simulation;
    private String image;
    private String inputPrefix;
    private String outputPrefix;
    private MinioOptions minioOptions;
    private String memory;
    private String cpu;
    private int gpu = 0;
    private int totalReplicates = 1;
    private int replicatesPerJob = 1;
    private int maxParallelism;
    private long timeoutSeconds = 86400;
    private boolean useFloat64 = false;
    private Map<String, String> env = new HashMap<>();
    private Map<String, String> labels = new HashMap<>();

    /**
     * Set the job ID.
     *
     * @param jobId unique identifier
     * @return this builder
     */
    public Builder setJobId(String jobId) {
      this.jobId = jobId;
      return this;
    }

    /**
     * Set the simulation name.
     *
     * @param simulation the simulation name
     * @return this builder
     */
    public Builder setSimulation(String simulation) {
      this.simulation = simulation;
      return this;
    }

    /**
     * Set the container image.
     *
     * @param image the image reference
     * @return this builder
     */
    public Builder setImage(String image) {
      this.image = image;
      return this;
    }

    /**
     * Set the MinIO input prefix.
     *
     * @param inputPrefix the prefix path
     * @return this builder
     */
    public Builder setInputPrefix(String inputPrefix) {
      this.inputPrefix = inputPrefix;
      return this;
    }

    /**
     * Set the MinIO output prefix.
     *
     * @param outputPrefix the prefix path
     * @return this builder
     */
    public Builder setOutputPrefix(String outputPrefix) {
      this.outputPrefix = outputPrefix;
      return this;
    }

    /**
     * Set the MinIO options.
     *
     * @param minioOptions the MinIO connection options
     * @return this builder
     */
    public Builder setMinioOptions(MinioOptions minioOptions) {
      this.minioOptions = minioOptions;
      return this;
    }

    /**
     * Set the memory resource request.
     *
     * @param memory the memory string
     * @return this builder
     */
    public Builder setMemory(String memory) {
      this.memory = memory;
      return this;
    }

    /**
     * Set the CPU resource request.
     *
     * @param cpu the CPU string
     * @return this builder
     */
    public Builder setCpu(String cpu) {
      this.cpu = cpu;
      return this;
    }

    /**
     * Set the GPU count.
     *
     * @param gpu the GPU count
     * @return this builder
     */
    public Builder setGpu(int gpu) {
      this.gpu = gpu;
      return this;
    }

    /**
     * Set the total replicate count.
     *
     * @param totalReplicates the total replicates
     * @return this builder
     */
    public Builder setTotalReplicates(int totalReplicates) {
      this.totalReplicates = totalReplicates;
      return this;
    }

    /**
     * Set the replicates per job unit.
     *
     * @param replicatesPerJob replicates per job (default 1)
     * @return this builder
     */
    public Builder setReplicatesPerJob(int replicatesPerJob) {
      this.replicatesPerJob = replicatesPerJob;
      return this;
    }

    /**
     * Set the max concurrent job units.
     *
     * @param maxParallelism the parallelism limit
     * @return this builder
     */
    public Builder setMaxParallelism(int maxParallelism) {
      this.maxParallelism = maxParallelism;
      return this;
    }

    /**
     * Set the job timeout.
     *
     * @param timeoutSeconds the timeout in seconds
     * @return this builder
     */
    public Builder setTimeoutSeconds(long timeoutSeconds) {
      this.timeoutSeconds = timeoutSeconds;
      return this;
    }

    /**
     * Set whether to use float64.
     *
     * @param useFloat64 true for double, false for BigDecimal
     * @return this builder
     */
    public Builder setUseFloat64(boolean useFloat64) {
      this.useFloat64 = useFloat64;
      return this;
    }

    /**
     * Set additional environment variables.
     *
     * @param env map of env vars
     * @return this builder
     */
    public Builder setEnv(Map<String, String> env) {
      this.env = new HashMap<>(env);
      return this;
    }

    /**
     * Set target-specific labels.
     *
     * @param labels map of labels
     * @return this builder
     */
    public Builder setLabels(Map<String, String> labels) {
      this.labels = new HashMap<>(labels);
      return this;
    }

    /**
     * Build the BatchJobSpec.
     *
     * @return a new immutable BatchJobSpec
     */
    public BatchJobSpec build() {
      return new BatchJobSpec(this);
    }
  }
}
