/**
 * Kubernetes-based batch execution target using Fabric8 client.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Dispatches batch jobs by creating Kubernetes indexed Jobs via Fabric8.
 *
 * <p>Each call to {@link #dispatch} creates a K8s Job with
 * {@code completionMode: Indexed} where each pod runs one replicate.
 * MinIO credentials are passed as environment variables so the container
 * can stage inputs and write results without CLI flags.</p>
 */
public class KubernetesTarget implements RemoteBatchTarget {

  private static final String JOB_NAME_PREFIX = "josh-";
  private static final int BACKOFF_LIMIT = 3;

  private final KubernetesTargetConfig config;
  private final KubernetesClient client;
  private final String minioEndpoint;
  private final String minioAccessKey;
  private final String minioSecretKey;
  private final String minioBucket;

  /**
   * Constructs a KubernetesTarget from config and MinIO credentials.
   *
   * <p>Creates a Fabric8 client configured for the kubectl context
   * specified in the config. If the context is null, Fabric8 uses
   * default auto-discovery (~/.kube/config or in-cluster SA).</p>
   *
   * @param config The Kubernetes target configuration.
   * @param minioEndpoint MinIO endpoint URL for pod env vars.
   * @param minioAccessKey MinIO access key for pod env vars.
   * @param minioSecretKey MinIO secret key for pod env vars.
   * @param minioBucket MinIO bucket name for pod env vars.
   */
  public KubernetesTarget(
      KubernetesTargetConfig config,
      String minioEndpoint,
      String minioAccessKey,
      String minioSecretKey,
      String minioBucket
  ) {
    this(
        config, minioEndpoint, minioAccessKey, minioSecretKey, minioBucket,
        buildClient(config.getContext())
    );
  }

  /**
   * Constructs a KubernetesTarget with an injected client (for testing).
   *
   * @param config The Kubernetes target configuration.
   * @param minioEndpoint MinIO endpoint URL for pod env vars.
   * @param minioAccessKey MinIO access key for pod env vars.
   * @param minioSecretKey MinIO secret key for pod env vars.
   * @param minioBucket MinIO bucket name for pod env vars.
   * @param client The Fabric8 Kubernetes client to use.
   */
  KubernetesTarget(
      KubernetesTargetConfig config,
      String minioEndpoint,
      String minioAccessKey,
      String minioSecretKey,
      String minioBucket,
      KubernetesClient client
  ) {
    this.config = config;
    this.minioEndpoint = minioEndpoint;
    this.minioAccessKey = minioAccessKey;
    this.minioSecretKey = minioSecretKey;
    this.minioBucket = minioBucket;
    this.client = client;
  }

  @Override
  public void dispatch(
      String jobId,
      String minioPrefix,
      String simulation,
      int replicates
  ) throws Exception {
    Job job = new JobBuilder()
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
            .withNewTemplate()
                .withNewSpec()
                    .withRestartPolicy("Never")
                    .addNewContainer()
                        .withName("joshsim")
                        .withImage(config.getImage())
                        .withResources(buildResourceRequirements())
                        .withEnv(buildEnvVars(
                            jobId, minioPrefix, simulation
                        ))
                        .withCommand("/bin/sh", "-c")
                        .withArgs(buildContainerCommand())
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

  private List<EnvVar> buildEnvVars(
      String jobId,
      String minioPrefix,
      String simulation
  ) {
    List<EnvVar> envVars = new ArrayList<>();
    envVars.add(envVar("MINIO_ENDPOINT", minioEndpoint));
    envVars.add(envVar("MINIO_ACCESS_KEY", minioAccessKey));
    envVars.add(envVar("MINIO_SECRET_KEY", minioSecretKey));
    envVars.add(envVar("MINIO_BUCKET", minioBucket));
    envVars.add(envVar("JOSH_JOB_ID", jobId));
    envVars.add(envVar("JOSH_MINIO_PREFIX", minioPrefix));
    envVars.add(envVar("JOSH_SIMULATION", simulation));
    return envVars;
  }

  private static EnvVar envVar(String name, String value) {
    return new EnvVarBuilder()
        .withName(name)
        .withValue(value)
        .build();
  }

  private io.fabric8.kubernetes.api.model.ResourceRequirements
      buildResourceRequirements() {
    Map<String, Map<String, String>> resources = config.getResources();
    if (resources == null) {
      return null;
    }

    Map<String, Quantity> requests = new HashMap<>();
    Map<String, Quantity> limits = new HashMap<>();

    Map<String, String> requestMap = resources.get("requests");
    if (requestMap != null) {
      requestMap.forEach((k, v) -> requests.put(k, new Quantity(v)));
    }

    Map<String, String> limitMap = resources.get("limits");
    if (limitMap != null) {
      limitMap.forEach((k, v) -> limits.put(k, new Quantity(v)));
    }

    io.fabric8.kubernetes.api.model.ResourceRequirements reqs =
        new io.fabric8.kubernetes.api.model.ResourceRequirements();
    if (!requests.isEmpty()) {
      reqs.setRequests(requests);
    }
    if (!limits.isEmpty()) {
      reqs.setLimits(limits);
    }
    return reqs;
  }

  private String buildContainerCommand() {
    String jar = config.getJarPath();
    return "java -jar " + jar + " stageFromMinio"
        + " --prefix=$JOSH_MINIO_PREFIX"
        + " --output-dir=/tmp/work"
        + " && SCRIPT=$(find /tmp/work -name '*.josh'"
        + " -type f | head -1)"
        + " && java -jar " + jar
        + " run \"$SCRIPT\" $JOSH_SIMULATION"
        + " --replicates=1";
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
