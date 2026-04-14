/**
 * A parsed target profile representing a remote compute target.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Target profile parsed from a JSON file at {@code ~/.josh/targets/<name>.json}.
 *
 * <p>Each profile configures a specific compute target (Cloud Run, Kubernetes cluster,
 * etc.) with its dispatch configuration and MinIO credentials. The {@code type} field
 * discriminates between target types, and the corresponding config block holds
 * type-specific settings.</p>
 *
 * <p>Profiles are self-contained — each one holds its own MinIO credentials.
 * Different targets with different storage backends get different profiles.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TargetProfile {

  @JsonProperty("type")
  private String type;

  @JsonProperty("http")
  private HttpTargetConfig httpConfig;

  @JsonProperty("kubernetes")
  private KubernetesTargetConfig kubernetesConfig;

  @JsonProperty("minio_endpoint")
  private String minioEndpoint;

  @JsonProperty("minio_access_key")
  private String minioAccessKey;

  @JsonProperty("minio_secret_key")
  private String minioSecretKey;

  @JsonProperty("minio_bucket")
  private String minioBucket;

  /**
   * Default constructor for Jackson deserialization.
   */
  public TargetProfile() {
  }

  /**
   * Returns the target type discriminator.
   *
   * @return The type string (e.g., "http", "kubernetes").
   */
  public String getType() {
    return type;
  }

  /**
   * Returns the HTTP target configuration, or null if this is not an HTTP target.
   *
   * @return The HTTP config, or null.
   */
  public HttpTargetConfig getHttpConfig() {
    return httpConfig;
  }

  /**
   * Returns the Kubernetes target configuration, or null if this is not a K8s target.
   *
   * @return The Kubernetes config, or null.
   */
  public KubernetesTargetConfig getKubernetesConfig() {
    return kubernetesConfig;
  }

  /**
   * Returns the MinIO endpoint URL for this target's storage.
   *
   * @return The MinIO endpoint.
   */
  public String getMinioEndpoint() {
    return minioEndpoint;
  }

  /**
   * Returns the MinIO access key for this target's storage.
   *
   * @return The MinIO access key.
   */
  public String getMinioAccessKey() {
    return minioAccessKey;
  }

  /**
   * Returns the MinIO secret key for this target's storage.
   *
   * @return The MinIO secret key.
   */
  public String getMinioSecretKey() {
    return minioSecretKey;
  }

  /**
   * Returns the MinIO bucket name for this target's storage.
   *
   * @return The MinIO bucket name.
   */
  public String getMinioBucket() {
    return minioBucket;
  }
}
