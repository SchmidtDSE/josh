/**
 * Configuration for Kubernetes batch execution target.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.remote.batch;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Immutable configuration for connecting to and submitting jobs on a Kubernetes cluster.
 *
 * <p>Contains kubectl context, namespace, container image, resource requests, and optional
 * node selection criteria. Values are typically populated from CLI flags or target profiles.</p>
 */
public class KubernetesConfig {

  private final String context;
  private final String namespace;
  private final String image;
  private final String memory;
  private final String cpu;
  private final int gpu;
  private final String gpuProduct;
  private final long timeoutSeconds;
  private final int backoffLimit;
  private final int parallelism;
  private final Map<String, String> nodeSelector;

  /**
   * Creates a new KubernetesConfig via the builder.
   *
   * @param builder the builder containing configuration values
   */
  private KubernetesConfig(Builder builder) {
    this.context = builder.context;
    this.namespace = builder.namespace;
    this.image = builder.image;
    this.memory = builder.memory;
    this.cpu = builder.cpu;
    this.gpu = builder.gpu;
    this.gpuProduct = builder.gpuProduct;
    this.timeoutSeconds = builder.timeoutSeconds;
    this.backoffLimit = builder.backoffLimit;
    this.parallelism = builder.parallelism;
    this.nodeSelector = Collections.unmodifiableMap(new HashMap<>(builder.nodeSelector));
  }

  /**
   * Get the kubectl context name. May be null to use the current context.
   *
   * @return the kubectl context, or null
   */
  public String getContext() {
    return context;
  }

  /**
   * Get the Kubernetes namespace.
   *
   * @return the namespace
   */
  public String getNamespace() {
    return namespace;
  }

  /**
   * Get the container image for batch worker pods.
   *
   * @return the container image reference
   */
  public String getImage() {
    return image;
  }

  /**
   * Get the memory resource request (e.g., "256Gi").
   *
   * @return the memory request string
   */
  public String getMemory() {
    return memory;
  }

  /**
   * Get the CPU resource request (e.g., "64").
   *
   * @return the CPU request string
   */
  public String getCpu() {
    return cpu;
  }

  /**
   * Get the number of GPUs requested. 0 means no GPU.
   *
   * @return the GPU count
   */
  public int getGpu() {
    return gpu;
  }

  /**
   * Get the GPU product name for node selection (e.g., "NVIDIA-A100-SXM4-80GB").
   * May be null if no GPU product constraint is needed.
   *
   * @return the GPU product name, or null
   */
  public String getGpuProduct() {
    return gpuProduct;
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
   * Get the backoff limit for failed pod retries.
   *
   * @return the backoff limit
   */
  public int getBackoffLimit() {
    return backoffLimit;
  }

  /**
   * Get the maximum number of pods to run concurrently.
   *
   * @return the parallelism value
   */
  public int getParallelism() {
    return parallelism;
  }

  /**
   * Get the node selector labels for pod scheduling.
   *
   * @return an unmodifiable map of node selector labels
   */
  public Map<String, String> getNodeSelector() {
    return nodeSelector;
  }

  /**
   * Builder for constructing KubernetesConfig instances.
   */
  public static class Builder {
    private String context;
    private String namespace = "default";
    private String image;
    private String memory;
    private String cpu = "4";
    private int gpu = 0;
    private String gpuProduct;
    private long timeoutSeconds = 86400;
    private int backoffLimit = 1;
    private int parallelism = -1;
    private Map<String, String> nodeSelector = new HashMap<>();

    /**
     * Set the kubectl context.
     *
     * @param context the kubectl context name
     * @return this builder
     */
    public Builder setContext(String context) {
      this.context = context;
      return this;
    }

    /**
     * Set the Kubernetes namespace.
     *
     * @param namespace the namespace
     * @return this builder
     */
    public Builder setNamespace(String namespace) {
      this.namespace = namespace;
      return this;
    }

    /**
     * Set the container image.
     *
     * @param image the container image reference
     * @return this builder
     */
    public Builder setImage(String image) {
      this.image = image;
      return this;
    }

    /**
     * Set the memory resource request.
     *
     * @param memory the memory request (e.g., "256Gi")
     * @return this builder
     */
    public Builder setMemory(String memory) {
      this.memory = memory;
      return this;
    }

    /**
     * Set the CPU resource request.
     *
     * @param cpu the CPU request (e.g., "64")
     * @return this builder
     */
    public Builder setCpu(String cpu) {
      this.cpu = cpu;
      return this;
    }

    /**
     * Set the GPU count.
     *
     * @param gpu the number of GPUs
     * @return this builder
     */
    public Builder setGpu(int gpu) {
      this.gpu = gpu;
      return this;
    }

    /**
     * Set the GPU product name for node selection.
     *
     * @param gpuProduct the GPU product name
     * @return this builder
     */
    public Builder setGpuProduct(String gpuProduct) {
      this.gpuProduct = gpuProduct;
      return this;
    }

    /**
     * Set the job timeout in seconds.
     *
     * @param timeoutSeconds the timeout
     * @return this builder
     */
    public Builder setTimeoutSeconds(long timeoutSeconds) {
      this.timeoutSeconds = timeoutSeconds;
      return this;
    }

    /**
     * Set the backoff limit for pod retries.
     *
     * @param backoffLimit the backoff limit
     * @return this builder
     */
    public Builder setBackoffLimit(int backoffLimit) {
      this.backoffLimit = backoffLimit;
      return this;
    }

    /**
     * Set the maximum concurrent pods.
     *
     * @param parallelism the parallelism value (-1 means same as replicates)
     * @return this builder
     */
    public Builder setParallelism(int parallelism) {
      this.parallelism = parallelism;
      return this;
    }

    /**
     * Set the node selector labels.
     *
     * @param nodeSelector map of label key to value
     * @return this builder
     */
    public Builder setNodeSelector(Map<String, String> nodeSelector) {
      this.nodeSelector = new HashMap<>(nodeSelector);
      return this;
    }

    /**
     * Build the KubernetesConfig.
     *
     * @return a new immutable KubernetesConfig
     * @throws IllegalStateException if required fields are missing
     */
    public KubernetesConfig build() {
      if (image == null || image.isEmpty()) {
        throw new IllegalStateException("Container image is required for Kubernetes target");
      }
      if (memory == null || memory.isEmpty()) {
        throw new IllegalStateException("Memory request is required for Kubernetes target");
      }
      return new KubernetesConfig(this);
    }
  }
}
