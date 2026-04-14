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
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.BatchAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.ScalableResource;
import io.fabric8.kubernetes.client.dsl.V1BatchAPIGroupDSL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  private KubernetesClient mockClient;
  private MixedOperation mockJobs;
  private ScalableResource mockResource;
  private KubernetesTargetConfig config;
  private ArgumentCaptor<Job> jobCaptor;

  @BeforeEach
  void setUp() {
    mockClient = mock(KubernetesClient.class);
    final BatchAPIGroupDSL mockBatch =
        mock(BatchAPIGroupDSL.class);
    final V1BatchAPIGroupDSL mockV1 =
        mock(V1BatchAPIGroupDSL.class);
    mockJobs = mock(MixedOperation.class);
    final MixedOperation mockNsJobs = mock(MixedOperation.class);
    mockResource = mock(ScalableResource.class);

    when(mockClient.batch()).thenReturn(mockBatch);
    when(mockBatch.v1()).thenReturn(mockV1);
    when(mockV1.jobs()).thenReturn(mockJobs);
    when(mockJobs.inNamespace(NAMESPACE)).thenReturn(mockNsJobs);

    jobCaptor = ArgumentCaptor.forClass(Job.class);
    when(mockNsJobs.resource(jobCaptor.capture()))
        .thenReturn(mockResource);
    when(mockResource.create()).thenReturn(null);

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
  void dispatchSetsMinioAndJobEnvVars() throws Exception {
    KubernetesTarget target = buildTarget(config);

    target.dispatch(JOB_ID, PREFIX, SIMULATION, 1);

    Job job = jobCaptor.getValue();
    List<EnvVar> envVars = getContainer(job).getEnv();
    assertEnvVar(envVars, "MINIO_ENDPOINT", "https://minio.test");
    assertEnvVar(envVars, "MINIO_ACCESS_KEY", "access");
    assertEnvVar(envVars, "MINIO_SECRET_KEY", "secret");
    assertEnvVar(envVars, "MINIO_BUCKET", "test-bucket");
    assertEnvVar(envVars, "JOSH_JOB_ID", JOB_ID);
    assertEnvVar(envVars, "JOSH_MINIO_PREFIX", PREFIX);
    assertEnvVar(envVars, "JOSH_SIMULATION", SIMULATION);
  }

  @Test
  void dispatchSetsResourceRequestsAndLimits() throws Exception {
    Map<String, Map<String, String>> resources = new HashMap<>();
    resources.put(
        "requests", Map.of("cpu", "2", "memory", "4Gi")
    );
    resources.put("limits", Map.of("memory", "256Gi"));
    config = buildConfig(10, 3600, resources);

    KubernetesTarget target = buildTarget(config);
    target.dispatch(JOB_ID, PREFIX, SIMULATION, 1);

    Job job = jobCaptor.getValue();
    ResourceRequirements reqs = getContainer(job).getResources();
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
    ResourceRequirements reqs = getContainer(job).getResources();
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
  void dispatchSetsContainerCommand() throws Exception {
    KubernetesTarget target = buildTarget(config);

    target.dispatch(JOB_ID, PREFIX, SIMULATION, 1);

    Job job = jobCaptor.getValue();
    Container container = getContainer(job);
    assertEquals(
        List.of("/bin/sh", "-c"), container.getCommand()
    );
    String args = container.getArgs().get(0);
    assertTrue(args.contains("stageFromMinio"));
    assertTrue(args.contains("/app/joshsim-fat.jar"));
    assertTrue(args.contains("--replicates=1"));
  }

  private KubernetesTarget buildTarget(
      KubernetesTargetConfig cfg
  ) {
    return new KubernetesTarget(
        cfg,
        "https://minio.test",
        "access",
        "secret",
        "test-bucket",
        mockClient
    );
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

  private void assertEnvVar(
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
