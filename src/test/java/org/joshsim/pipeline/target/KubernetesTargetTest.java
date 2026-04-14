/**
 * Tests for KubernetesTarget.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.BatchAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NamespaceableResource;
import io.fabric8.kubernetes.client.dsl.ScalableResource;
import io.fabric8.kubernetes.client.dsl.V1BatchAPIGroupDSL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joshsim.util.MinioOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;


/**
 * Unit tests for {@link KubernetesTarget}.
 *
 * <p>Uses Mockito to mock Fabric8 client — avoids mock server
 * SSL issues on JDK 21+.</p>
 */
@SuppressWarnings("unchecked")
class KubernetesTargetTest {

  private static final String NAMESPACE = "joshsim-lab";
  private static final String IMAGE =
      "ghcr.io/schmidtdse/joshsim-job:latest";
  private static final String JOB_ID = "abc-123-def";
  private static final String PREFIX =
      "batch-jobs/abc-123-def/inputs/";
  private static final String SIMULATION = "Main";
  private static final String POD_ENDPOINT =
      "http://minio.default.svc:9000";

  private KubernetesClient mockClient;
  private ScalableResource mockJobResource;
  private NamespaceableResource mockSecretResource;
  private KubernetesTargetConfig config;
  private ArgumentCaptor<Job> jobCaptor;
  private ArgumentCaptor<Secret> secretCaptor;

  @BeforeEach
  void setUp() {
    mockClient = mock(KubernetesClient.class);

    // Wire batch().v1().jobs() chain
    final BatchAPIGroupDSL mockBatch =
        mock(BatchAPIGroupDSL.class);
    final V1BatchAPIGroupDSL mockV1 =
        mock(V1BatchAPIGroupDSL.class);
    final MixedOperation mockJobs = mock(MixedOperation.class);
    final MixedOperation mockNsJobs = mock(MixedOperation.class);
    mockJobResource = mock(ScalableResource.class);

    when(mockClient.batch()).thenReturn(mockBatch);
    when(mockBatch.v1()).thenReturn(mockV1);
    when(mockV1.jobs()).thenReturn(mockJobs);
    when(mockJobs.inNamespace(NAMESPACE)).thenReturn(mockNsJobs);

    jobCaptor = ArgumentCaptor.forClass(Job.class);
    when(mockNsJobs.resource(jobCaptor.capture()))
        .thenReturn(mockJobResource);
    when(mockJobResource.create()).thenReturn(null);

    // Wire secrets() chain
    final MixedOperation mockSecrets =
        mock(MixedOperation.class);
    final MixedOperation mockNsSecrets =
        mock(MixedOperation.class);
    mockSecretResource = mock(NamespaceableResource.class);

    when(mockClient.secrets()).thenReturn(mockSecrets);
    when(mockSecrets.inNamespace(NAMESPACE))
        .thenReturn(mockNsSecrets);

    secretCaptor = ArgumentCaptor.forClass(Secret.class);
    when(mockNsSecrets.resource(secretCaptor.capture()))
        .thenReturn(mockSecretResource);
    when(mockSecretResource.create()).thenReturn(null);

    config = buildConfig(10, 3600, null);
  }

  @Test
  void dispatchCreatesIndexedJobWithCorrectSpec()
      throws Exception {
    KubernetesTarget target = buildTarget(config);

    target.dispatch(JOB_ID, PREFIX, SIMULATION, 5);

    Job job = jobCaptor.getValue();
    assertNotNull(job);
    assertEquals("Indexed", job.getSpec().getCompletionMode());
    assertEquals(5, job.getSpec().getCompletions());
    assertEquals(5, job.getSpec().getParallelism());
    assertEquals(3, job.getSpec().getBackoffLimit());
    assertEquals(
        3600L, job.getSpec().getActiveDeadlineSeconds()
    );
  }

  @Test
  void dispatchSetsContainerImage() throws Exception {
    KubernetesTarget target = buildTarget(config);

    target.dispatch(JOB_ID, PREFIX, SIMULATION, 1);

    Job job = jobCaptor.getValue();
    Container container = getContainer(job);
    assertEquals(IMAGE, container.getImage());
  }

  @Test
  void dispatchCreatesSecretWithMinioCreds() throws Exception {
    KubernetesTarget target = buildTarget(config);

    target.dispatch(JOB_ID, PREFIX, SIMULATION, 1);

    Secret secret = secretCaptor.getValue();
    assertNotNull(secret);
    assertEquals(
        "josh-creds-" + JOB_ID,
        secret.getMetadata().getName()
    );
    Map<String, String> data = secret.getData();
    // Secret should use pod endpoint, not host endpoint
    String decodedEndpoint = new String(
        java.util.Base64.getDecoder().decode(
            data.get("MINIO_ENDPOINT")
        )
    );
    assertEquals(POD_ENDPOINT, decodedEndpoint);
    assertNotNull(data.get("MINIO_ACCESS_KEY"));
    assertNotNull(data.get("MINIO_SECRET_KEY"));
    assertNotNull(data.get("MINIO_BUCKET"));
  }

  @Test
  void dispatchSetsSecretRefEnvVars() throws Exception {
    KubernetesTarget target = buildTarget(config);

    target.dispatch(JOB_ID, PREFIX, SIMULATION, 1);

    Job job = jobCaptor.getValue();
    List<EnvVar> envVars = getContainer(job).getEnv();
    String secretName = "josh-creds-" + JOB_ID;

    assertSecretEnvVar(
        envVars, "MINIO_ENDPOINT", secretName
    );
    assertSecretEnvVar(
        envVars, "MINIO_ACCESS_KEY", secretName
    );
    assertSecretEnvVar(
        envVars, "MINIO_SECRET_KEY", secretName
    );
    assertSecretEnvVar(
        envVars, "MINIO_BUCKET", secretName
    );
  }

  @Test
  void dispatchSetsPlainJobEnvVars() throws Exception {
    KubernetesTarget target = buildTarget(config);

    target.dispatch(JOB_ID, PREFIX, SIMULATION, 1);

    Job job = jobCaptor.getValue();
    List<EnvVar> envVars = getContainer(job).getEnv();
    assertPlainEnvVar(envVars, "JOSH_JOB_ID", JOB_ID);
    assertPlainEnvVar(envVars, "JOSH_MINIO_PREFIX", PREFIX);
    assertPlainEnvVar(envVars, "JOSH_SIMULATION", SIMULATION);
  }

  @Test
  void dispatchSetsResourceRequestsAndLimits()
      throws Exception {
    Map<String, Map<String, String>> resources =
        new HashMap<>();
    resources.put(
        "requests", Map.of("cpu", "2", "memory", "4Gi")
    );
    resources.put("limits", Map.of("memory", "256Gi"));
    config = buildConfig(10, 3600, resources);

    KubernetesTarget target = buildTarget(config);
    target.dispatch(JOB_ID, PREFIX, SIMULATION, 1);

    Job job = jobCaptor.getValue();
    ResourceRequirements reqs =
        getContainer(job).getResources();
    assertNotNull(reqs);
    assertEquals(
        new Quantity("2"), reqs.getRequests().get("cpu")
    );
    assertEquals(
        new Quantity("4Gi"), reqs.getRequests().get("memory")
    );
    assertEquals(
        new Quantity("256Gi"), reqs.getLimits().get("memory")
    );
  }

  @Test
  void dispatchHandlesNullResources() throws Exception {
    config = buildConfig(10, 3600, null);

    KubernetesTarget target = buildTarget(config);
    target.dispatch(JOB_ID, PREFIX, SIMULATION, 1);

    Job job = jobCaptor.getValue();
    ResourceRequirements reqs =
        getContainer(job).getResources();
    assertNull(reqs);
  }

  @Test
  void dispatchCapsParallelismAtReplicates() throws Exception {
    config = buildConfig(10, 3600, null);

    KubernetesTarget target = buildTarget(config);
    target.dispatch(JOB_ID, PREFIX, SIMULATION, 3);

    Job job = jobCaptor.getValue();
    assertEquals(3, job.getSpec().getParallelism());
    assertEquals(3, job.getSpec().getCompletions());
  }

  @Test
  void dispatchJobNameFormat() throws Exception {
    KubernetesTarget target = buildTarget(config);

    target.dispatch(JOB_ID, PREFIX, SIMULATION, 1);

    Job job = jobCaptor.getValue();
    assertEquals(
        "josh-" + JOB_ID, job.getMetadata().getName()
    );
    assertEquals(NAMESPACE, job.getMetadata().getNamespace());
    assertEquals(
        JOB_ID,
        job.getMetadata().getLabels().get("josh-job-id")
    );
    assertEquals(
        "joshsim",
        job.getMetadata().getLabels().get("app")
    );
  }

  @Test
  void dispatchUsesEntrypointScript() throws Exception {
    KubernetesTarget target = buildTarget(config);

    target.dispatch(JOB_ID, PREFIX, SIMULATION, 1);

    Job job = jobCaptor.getValue();
    Container container = getContainer(job);
    List<String> command = container.getCommand();
    assertEquals("/app/entrypoint.sh", command.get(0));
    assertEquals("/app/joshsim-fat.jar", command.get(1));
  }

  private KubernetesTarget buildTarget(
      KubernetesTargetConfig cfg
  ) {
    return new KubernetesTarget(
        cfg, buildTestMinioOptions(), mockClient
    );
  }

  private MinioOptions buildTestMinioOptions() {
    return new MinioOptions() {
      @Override
      protected String getEnvValue(String name) {
        return switch (name) {
          case "MINIO_ENDPOINT" -> "https://minio.test";
          case "MINIO_ACCESS_KEY" -> "access";
          case "MINIO_SECRET_KEY" -> "secret";
          case "MINIO_BUCKET" -> "test-bucket";
          default -> null;
        };
      }
    };
  }

  private KubernetesTargetConfig buildConfig(
      int parallelism,
      int timeout,
      Map<String, Map<String, String>> resources
  ) {
    KubernetesTargetConfig cfg = new KubernetesTargetConfig();
    try {
      setField(cfg, "context", "test-context");
      setField(cfg, "namespace", NAMESPACE);
      setField(cfg, "image", IMAGE);
      setField(cfg, "resources", resources);
      setField(cfg, "parallelism", parallelism);
      setField(cfg, "timeoutSeconds", timeout);
      setField(cfg, "podMinioEndpoint", POD_ENDPOINT);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return cfg;
  }

  private static void setField(
      Object obj, String name, Object value
  ) throws Exception {
    java.lang.reflect.Field field =
        obj.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(obj, value);
  }

  private Container getContainer(Job job) {
    return job.getSpec().getTemplate().getSpec()
        .getContainers().get(0);
  }

  private void assertSecretEnvVar(
      List<EnvVar> envVars,
      String envName,
      String secretName
  ) {
    EnvVar found = envVars.stream()
        .filter(e -> envName.equals(e.getName()))
        .findFirst()
        .orElse(null);
    assertNotNull(found, "Missing env var: " + envName);
    assertNotNull(
        found.getValueFrom(),
        envName + " should use valueFrom"
    );
    assertEquals(
        secretName,
        found.getValueFrom().getSecretKeyRef().getName()
    );
  }

  private void assertPlainEnvVar(
      List<EnvVar> envVars, String name, String value
  ) {
    EnvVar found = envVars.stream()
        .filter(e -> name.equals(e.getName()))
        .findFirst()
        .orElse(null);
    assertNotNull(found, "Missing env var: " + name);
    assertEquals(value, found.getValue());
  }
}
