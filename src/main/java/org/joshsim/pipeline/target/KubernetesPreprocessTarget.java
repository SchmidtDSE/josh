/**
 * Kubernetes-based preprocessing target using Fabric8 client.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joshsim.util.MinioOptions;


/**
 * Dispatches preprocessing jobs by creating Kubernetes Jobs via Fabric8.
 *
 * <p>Unlike {@link KubernetesTarget} which creates indexed Jobs with one pod
 * per replicate, this target creates a single-completion Job for preprocessing.
 * The pod runs {@code preprocess-entrypoint.sh} which stages inputs from MinIO,
 * runs the preprocess command, and uploads the resulting .jshd back to MinIO.</p>
 */
public class KubernetesPreprocessTarget implements RemotePreprocessTarget {

  private static final String JOB_NAME_PREFIX = "josh-preprocess-";
  private static final String SECRET_NAME_PREFIX = "josh-creds-";
  private static final int BACKOFF_LIMIT = 3;
  private static final String ENTRYPOINT = "/app/preprocess-entrypoint.sh";

  private final KubernetesTargetConfig config;
  private final KubernetesClient client;
  private final MinioOptions minioOptions;

  /**
   * Constructs a KubernetesPreprocessTarget from config and MinIO options.
   *
   * @param config The Kubernetes target configuration.
   * @param minioOptions MinIO options resolved via HierarchyConfig.
   */
  public KubernetesPreprocessTarget(
      KubernetesTargetConfig config,
      MinioOptions minioOptions
  ) {
    this(config, minioOptions, buildClient(config.getContext()));
  }

  /**
   * Constructs a KubernetesPreprocessTarget with an injected client (testing).
   *
   * @param config The Kubernetes target configuration.
   * @param minioOptions MinIO options resolved via HierarchyConfig.
   * @param client The Fabric8 Kubernetes client to use.
   */
  KubernetesPreprocessTarget(
      KubernetesTargetConfig config,
      MinioOptions minioOptions,
      KubernetesClient client
  ) {
    this.config = config;
    this.minioOptions = minioOptions;
    this.client = client;
  }

  @Override
  public void dispatch(
      String jobId,
      String minioPrefix,
      String simulation,
      PreprocessParams params
  ) throws Exception {
    String secretName = SECRET_NAME_PREFIX + jobId;

    createSecret(secretName);

    Job job = new JobBuilder()
        .withNewMetadata()
            .withName(JOB_NAME_PREFIX + jobId)
            .withNamespace(config.getNamespace())
            .addToLabels("josh-job-id", jobId)
            .addToLabels("app", "joshsim")
            .addToLabels("josh-job-type", "preprocess")
        .endMetadata()
        .withNewSpec()
            .withCompletions(1)
            .withParallelism(1)
            .withBackoffLimit(BACKOFF_LIMIT)
            .withActiveDeadlineSeconds(
                (long) config.getTimeoutSeconds()
            )
            .withNewTemplate()
                .withNewSpec()
                    .withRestartPolicy("Never")
                    .addNewContainer()
                        .withName("joshsim-preprocess")
                        .withImage(config.getImage())
                        .withResources(
                            buildResourceRequirements()
                        )
                        .withEnv(buildEnvVars(
                            secretName, jobId,
                            minioPrefix, simulation, params
                        ))
                        .withCommand(
                            ENTRYPOINT,
                            config.getJarPath()
                        )
                    .endContainer()
                .endSpec()
            .endTemplate()
        .endSpec()
        .build();

    client.batch().v1().jobs()
        .inNamespace(config.getNamespace())
        .resource(job)
        .create();
  }

  /**
   * Returns the Fabric8 Kubernetes client used by this target.
   *
   * @return The Kubernetes client.
   */
  public KubernetesClient getClient() {
    return client;
  }

  /**
   * Returns the Kubernetes target configuration.
   *
   * @return The configuration.
   */
  public KubernetesTargetConfig getConfig() {
    return config;
  }

  private void createSecret(String secretName) {
    Map<String, String> resolved =
        minioOptions.getResolvedCredentials();
    resolved.put(
        "MINIO_ENDPOINT", config.getPodMinioEndpoint()
    );
    Map<String, String> data = new HashMap<>();
    resolved.forEach(
        (k, v) -> data.put(k, encode(v))
    );

    Secret secret = new SecretBuilder()
        .withNewMetadata()
            .withName(secretName)
            .withNamespace(config.getNamespace())
        .endMetadata()
        .withType("Opaque")
        .withData(data)
        .build();

    client.secrets()
        .inNamespace(config.getNamespace())
        .resource(secret)
        .create();
  }

  private static String encode(String value) {
    return Base64.getEncoder().encodeToString(
        value.getBytes(java.nio.charset.StandardCharsets.UTF_8)
    );
  }

  private List<EnvVar> buildEnvVars(
      String secretName,
      String jobId,
      String minioPrefix,
      String simulation,
      PreprocessParams params
  ) {
    List<EnvVar> envVars = new ArrayList<>();
    // MinIO credentials from secret
    envVars.add(secretEnvVar(
        "MINIO_ENDPOINT", secretName, "MINIO_ENDPOINT"
    ));
    envVars.add(secretEnvVar(
        "MINIO_ACCESS_KEY", secretName, "MINIO_ACCESS_KEY"
    ));
    envVars.add(secretEnvVar(
        "MINIO_SECRET_KEY", secretName, "MINIO_SECRET_KEY"
    ));
    envVars.add(secretEnvVar(
        "MINIO_BUCKET", secretName, "MINIO_BUCKET"
    ));
    // Job identification
    envVars.add(plainEnvVar("JOSH_JOB_ID", jobId));
    envVars.add(plainEnvVar("JOSH_MINIO_PREFIX", minioPrefix));
    envVars.add(plainEnvVar("JOSH_SIMULATION", simulation));
    // Preprocess-specific parameters
    envVars.add(plainEnvVar("JOSH_DATA_FILE", params.getDataFile()));
    envVars.add(plainEnvVar("JOSH_VARIABLE", params.getVariable()));
    envVars.add(plainEnvVar("JOSH_UNITS", params.getUnits()));
    envVars.add(plainEnvVar("JOSH_OUTPUT_FILE", params.getOutputFile()));
    envVars.add(plainEnvVar("JOSH_CRS", params.getCrs()));
    envVars.add(plainEnvVar("JOSH_X_COORD", params.getHorizCoord()));
    envVars.add(plainEnvVar("JOSH_Y_COORD", params.getVertCoord()));
    envVars.add(plainEnvVar("JOSH_TIME_DIM", params.getTimeDim()));
    if (params.getTimestep() != null && !params.getTimestep().isEmpty()) {
      envVars.add(plainEnvVar("JOSH_TIMESTEP", params.getTimestep()));
    }
    if (params.getDefaultValue() != null && !params.getDefaultValue().isEmpty()) {
      envVars.add(plainEnvVar("JOSH_DEFAULT_VALUE", params.getDefaultValue()));
    }
    if (params.isParallel()) {
      envVars.add(plainEnvVar("JOSH_PARALLEL", "true"));
    }
    if (params.isAmend()) {
      envVars.add(plainEnvVar("JOSH_AMEND", "true"));
    }
    return envVars;
  }

  private static EnvVar secretEnvVar(
      String envName,
      String secretName,
      String secretKey
  ) {
    return new EnvVarBuilder()
        .withName(envName)
        .withValueFrom(new EnvVarSourceBuilder()
            .withNewSecretKeyRef()
                .withName(secretName)
                .withKey(secretKey)
            .endSecretKeyRef()
            .build())
        .build();
  }

  private static EnvVar plainEnvVar(String name, String value) {
    return new EnvVarBuilder()
        .withName(name)
        .withValue(value)
        .build();
  }

  private ResourceRequirements buildResourceRequirements() {
    Map<String, Map<String, String>> resources =
        config.getResources();
    if (resources == null) {
      return null;
    }

    Map<String, Quantity> requests = new HashMap<>();
    Map<String, Quantity> limits = new HashMap<>();

    Map<String, String> requestMap = resources.get("requests");
    if (requestMap != null) {
      requestMap.forEach(
          (k, v) -> requests.put(k, new Quantity(v))
      );
    }

    Map<String, String> limitMap = resources.get("limits");
    if (limitMap != null) {
      limitMap.forEach(
          (k, v) -> limits.put(k, new Quantity(v))
      );
    }

    ResourceRequirements reqs = new ResourceRequirements();
    if (!requests.isEmpty()) {
      reqs.setRequests(requests);
    }
    if (!limits.isEmpty()) {
      reqs.setLimits(limits);
    }
    return reqs;
  }

  private static KubernetesClient buildClient(String context) {
    Config kubeConfig;
    if (context != null && !context.isEmpty()) {
      kubeConfig = Config.autoConfigure(context);
    } else {
      kubeConfig = Config.autoConfigure(null);
    }
    return new KubernetesClientBuilder()
        .withConfig(kubeConfig)
        .build();
  }
}
