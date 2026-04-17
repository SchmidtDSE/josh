/**
 * A parsed target profile representing a remote compute target.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.joshsim.util.MinioHandler;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;


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

  /** Path to the JSON file this profile was loaded from. Set by TargetProfileLoader. */
  private transient String sourceFilePath;

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
   * Builds a {@link MinioOptions} resolved through {@link
   * org.joshsim.util.HierarchyConfig}.
   *
   * <p>Values are resolved in priority order: JSON profile file →
   * environment variables. This means MinIO credentials do not need
   * to live in the profile JSON — they can be set via
   * {@code MINIO_ENDPOINT}, {@code MINIO_ACCESS_KEY}, etc.</p>
   *
   * @return A configured MinioOptions instance.
   */
  public MinioOptions buildMinioOptions() {
    MinioOptions options = new MinioOptions();
    options.setConfigFile(sourceFilePath);
    return options;
  }

  /**
   * Sets the file path this profile was loaded from. Called by {@link TargetProfileLoader}.
   *
   * @param path The absolute path to the JSON profile file.
   */
  void setSourceFilePath(String path) {
    this.sourceFilePath = path;
  }

  /**
   * Returns the file path this profile was loaded from.
   *
   * @return The source file path, or null if not loaded from a file.
   */
  public String getSourceFilePath() {
    return sourceFilePath;
  }

  /**
   * Creates a MinioHandler configured with this profile's MinIO credentials.
   *
   * <p>Uses the profile's JSON file as a {@link MinioOptions} config source,
   * reusing the same config hierarchy as all other MinIO operations.</p>
   *
   * @param output For logging information and errors.
   * @return A MinioHandler ready for staging and polling operations.
   * @throws Exception If MinIO client creation or bucket validation fails.
   */
  public MinioHandler buildMinioHandler(OutputOptions output)
      throws Exception {
    return new MinioHandler(buildMinioOptions(), output);
  }

  /**
   * Creates the appropriate {@link BatchPollingStrategy} for this target.
   *
   * <p>HTTP targets use {@link MinioPollingStrategy} (reads status.json
   * from MinIO). Kubernetes targets use {@link KubernetesPollingStrategy}
   * (queries the K8s Job API via Fabric8).</p>
   *
   * @param output For logging during MinIO handler construction.
   * @return A polling strategy configured for this target's type.
   * @throws Exception If strategy construction fails.
   */
  public BatchPollingStrategy buildPollingStrategy(
      OutputOptions output
  ) throws Exception {
    if ("http".equals(type)) {
      return new MinioPollingStrategy(buildMinioHandler(output));
    } else if ("kubernetes".equals(type)) {
      KubernetesTargetConfig k8sConfig = getKubernetesConfig();
      String context = k8sConfig.getContext();
      Config kubeConfig = (context != null && !context.isEmpty())
          ? Config.autoConfigure(context)
          : Config.autoConfigure(null);
      KubernetesClient client = new KubernetesClientBuilder()
          .withConfig(kubeConfig)
          .build();
      return new KubernetesPollingStrategy(
          client, k8sConfig.getNamespace()
      );
    }
    throw new IllegalArgumentException(
        "Unsupported target type for polling: " + type
        + ". Supported types: http, kubernetes"
    );
  }
}
