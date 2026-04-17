/**
 * Kubernetes-based batch execution target using Fabric8 client.
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
import io.fabric8.kubernetes.api.model.Toleration;
import io.fabric8.kubernetes.api.model.TolerationBuilder;
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
 * Dispatches batch jobs by creating Kubernetes indexed Jobs via Fabric8.
 *
 * <p>Each call to {@link #dispatch} creates a K8s Job with
 * {@code completionMode: Indexed} where each pod runs one replicate.
 * MinIO credentials are stored in a K8s Secret and referenced via
 * {@code secretKeyRef} so they don't appear in the Job spec.</p>
 */
public class KubernetesTarget implements RemoteBatchTarget {

  private static final String JOB_NAME_PREFIX = "josh-";
  private static final String SECRET_NAME_PREFIX = "josh-creds-";
  private static final int BACKOFF_LIMIT = 3;
  private static final String ENTRYPOINT = "/app/run-entrypoint.sh";

  private final KubernetesTargetConfig config;
  private final KubernetesClient client;
  private final MinioOptions minioOptions;

  /**
   * Constructs a KubernetesTarget from config and MinIO options.
   *
   * <p>Creates a Fabric8 client configured for the kubectl context
   * specified in the config. If the context is null, Fabric8 uses
   * default auto-discovery (~/.kube/config or in-cluster SA).</p>
   *
   * <p>MinIO credentials are resolved through {@link
   * org.joshsim.util.HierarchyConfig} — they can come from the
   * target profile JSON, environment variables, or CLI flags.
   * Secrets do not need to live in the profile file.</p>
   *
   * @param config The Kubernetes target configuration.
   * @param minioOptions MinIO options resolved via HierarchyConfig.
   */
  public KubernetesTarget(
      KubernetesTargetConfig config,
      MinioOptions minioOptions
  ) {
    this(config, minioOptions, buildClient(config.getContext()));
  }

  /**
   * Constructs a KubernetesTarget with an injected client (testing).
   *
   * @param config The Kubernetes target configuration.
   * @param minioOptions MinIO options resolved via HierarchyConfig.
   * @param client The Fabric8 Kubernetes client to use.
   */
  KubernetesTarget(
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
      int replicates
  ) throws Exception {
    String secretName = SECRET_NAME_PREFIX + jobId;

    createSecret(secretName);

    JobBuilder jobBuilder = new JobBuilder()
        .withNewMetadata()
            .withName(JOB_NAME_PREFIX + jobId)
            .withNamespace(config.getNamespace())
            .addToLabels("josh-job-id", jobId)
            .addToLabels("app", "joshsim")
        .endMetadata()
        .withNewSpec()
            .withCompletionMode("Indexed")
            .withCompletions(replicates)
            .withParallelism(
                Math.min(config.getParallelism(), replicates)
            )
            .withBackoffLimit(BACKOFF_LIMIT)
            .withActiveDeadlineSeconds(
                (long) config.getTimeoutSeconds()
            )
            .withTtlSecondsAfterFinished(
                config.getTtlSecondsAfterFinished()
            )
            .withNewTemplate()
                .withNewSpec()
                    .withRestartPolicy("Never")
                    .addNewContainer()
                        .withName("joshsim")
                        .withImage(config.getImage())
                        .withResources(
                            buildResourceRequirements()
                        )
                        .withEnv(buildEnvVars(
                            secretName, jobId,
                            minioPrefix, simulation
                        ))
                        .withCommand(
                            ENTRYPOINT,
                            config.getJarPath()
                        )
                    .endContainer()
                .endSpec()
            .endTemplate()
        .endSpec();

    if (config.isSpot()) {
      applySpotConfig(jobBuilder);
    }

    Job job = jobBuilder.build();

    client.batch().v1().jobs()
        .inNamespace(config.getNamespace())
        .resource(job)
        .create();
  }

  /**
   * Returns the Fabric8 Kubernetes client used by this target.
   *
   * <p>Shared with {@link KubernetesPollingStrategy} to avoid
   * creating multiple connections to the same cluster.</p>
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
    // Pods use the explicit pod endpoint, not the host-resolved one
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
      String simulation
  ) {
    List<EnvVar> envVars = new ArrayList<>();
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
    envVars.add(plainEnvVar("JOSH_JOB_ID", jobId));
    envVars.add(plainEnvVar("JOSH_MINIO_PREFIX", minioPrefix));
    envVars.add(plainEnvVar("JOSH_SIMULATION", simulation));
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

  private void applySpotConfig(JobBuilder jobBuilder) {
    Toleration spotToleration = new TolerationBuilder()
        .withKey("cloud.google.com/gke-spot")
        .withOperator("Equal")
        .withValue("true")
        .withEffect("NoSchedule")
        .build();
    jobBuilder.editSpec()
        .editTemplate()
            .editSpec()
                .addToNodeSelector(
                    "cloud.google.com/gke-spot", "true"
                )
                .addToTolerations(spotToleration)
            .endSpec()
        .endTemplate()
        .endSpec();
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
