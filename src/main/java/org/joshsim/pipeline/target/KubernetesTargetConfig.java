/**
 * Configuration for a Kubernetes-based batch execution target.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;


/**
 * Kubernetes target configuration parsed from a target profile JSON file.
 *
 * <p>Holds the K8s cluster context, namespace, container image, resource
 * requests/limits, parallelism, and timeout for indexed Job submission.
 * Used by {@code KubernetesTarget} (PR 7) to construct K8s Job specs
 * via the Fabric8 client.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KubernetesTargetConfig {

  @JsonProperty("context")
  private String context;

  @JsonProperty("namespace")
  private String namespace;

  @JsonProperty("image")
  private String image;

  @JsonProperty("resources")
  private Map<String, Map<String, String>> resources;

  @JsonProperty("parallelism")
  private int parallelism;

  @JsonProperty("timeoutSeconds")
  private int timeoutSeconds;

  @JsonProperty("jarPath")
  private String jarPath;

  /**
   * Default constructor for Jackson deserialization.
   */
  public KubernetesTargetConfig() {
  }

  /**
   * Returns the kubectl context name for cluster access.
   *
   * @return The K8s context name.
   */
  public String getContext() {
    return context;
  }

  /**
   * Returns the K8s namespace for job submission.
   *
   * @return The namespace.
   */
  public String getNamespace() {
    return namespace;
  }

  /**
   * Returns the container image for job pods.
   *
   * @return The image reference (e.g., {@code ghcr.io/schmidtdse/joshsim-job:latest}).
   */
  public String getImage() {
    return image;
  }

  /**
   * Returns the resource requests and limits for job pods.
   *
   * <p>Expected structure: {@code {"requests": {"cpu": "2", "memory": "4Gi"},
   * "limits": {"memory": "256Gi"}}}</p>
   *
   * @return The resources map, or null if not specified.
   */
  public Map<String, Map<String, String>> getResources() {
    return resources;
  }

  /**
   * Returns the maximum number of parallel pod completions.
   *
   * @return The parallelism value.
   */
  public int getParallelism() {
    return parallelism;
  }

  /**
   * Returns the job timeout in seconds.
   *
   * @return The timeout in seconds.
   */
  public int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  /**
   * Returns the path to the joshsim fat jar inside the container image.
   *
   * <p>Defaults to {@code /app/joshsim-fat.jar} if not specified in the profile.
   * Override this for custom container images that place the jar elsewhere.</p>
   *
   * @return The jar path inside the container.
   */
  public String getJarPath() {
    if (jarPath == null || jarPath.isEmpty()) {
      return "/app/joshsim-fat.jar";
    }
    return jarPath;
  }
}
