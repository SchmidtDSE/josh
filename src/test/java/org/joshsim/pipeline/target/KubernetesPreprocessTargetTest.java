/**
 * Tests for KubernetesPreprocessTarget.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.BatchAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NamespaceableResource;
import io.fabric8.kubernetes.client.dsl.ScalableResource;
import io.fabric8.kubernetes.client.dsl.V1BatchAPIGroupDSL;
import java.util.List;
import java.util.Map;
import org.joshsim.util.MinioOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;


/**
 * Unit tests for {@link KubernetesPreprocessTarget}.
 *
 * <p>Uses Mockito to mock Fabric8 client — same pattern as
 * {@link KubernetesTargetTest}.</p>
 */
@SuppressWarnings("unchecked")
class KubernetesPreprocessTargetTest {

  private static final String NAMESPACE = "joshsim-lab";
  private static final String IMAGE = "ghcr.io/schmidtdse/joshsim-job:latest";
  private static final String JOB_ID = "pp-abc-123";
  private static final String PREFIX = "batch-jobs/pp-abc-123/inputs/";
  private static final String SIMULATION = "Main";
  private static final String POD_ENDPOINT = "http://minio.default.svc:9000";

  private KubernetesClient mockClient;
  private KubernetesTargetConfig config;
  private ArgumentCaptor<Job> jobCaptor;
  private ArgumentCaptor<Secret> secretCaptor;

  @BeforeEach
  void setUp() {
    mockClient = mock(KubernetesClient.class);

    final BatchAPIGroupDSL mockBatch = mock(BatchAPIGroupDSL.class);
    final V1BatchAPIGroupDSL mockV1 = mock(V1BatchAPIGroupDSL.class);
    final MixedOperation mockJobs = mock(MixedOperation.class);
    final MixedOperation mockNsJobs = mock(MixedOperation.class);

    when(mockClient.batch()).thenReturn(mockBatch);
    when(mockBatch.v1()).thenReturn(mockV1);
    when(mockV1.jobs()).thenReturn(mockJobs);
    when(mockJobs.inNamespace(NAMESPACE)).thenReturn(mockNsJobs);

    jobCaptor = ArgumentCaptor.forClass(Job.class);
    final ScalableResource mockJobResource = mock(ScalableResource.class);
    when(mockNsJobs.resource(jobCaptor.capture())).thenReturn(mockJobResource);
    when(mockJobResource.create()).thenReturn(null);

    final MixedOperation mockSecrets = mock(MixedOperation.class);
    final MixedOperation mockNsSecrets = mock(MixedOperation.class);

    when(mockClient.secrets()).thenReturn(mockSecrets);
    when(mockSecrets.inNamespace(NAMESPACE)).thenReturn(mockNsSecrets);

    secretCaptor = ArgumentCaptor.forClass(Secret.class);
    final NamespaceableResource mockSecretResource = mock(NamespaceableResource.class);
    when(mockNsSecrets.resource(secretCaptor.capture())).thenReturn(mockSecretResource);
    when(mockSecretResource.create()).thenReturn(null);

    config = buildConfig(10, 3600, null);
  }

  @Test
  void dispatchCreatesSingleCompletionJob() throws Exception {
    KubernetesPreprocessTarget target = buildTarget(config);

    target.dispatch(JOB_ID, PREFIX, SIMULATION, buildParams());

    Job job = jobCaptor.getValue();
    assertNotNull(job);
    assertEquals(1, job.getSpec().getCompletions());
    assertEquals(1, job.getSpec().getParallelism());
    assertEquals(3, job.getSpec().getBackoffLimit());
    assertEquals(3600L, job.getSpec().getActiveDeadlineSeconds());
  }

  @Test
  void dispatchSetsPreprocessEntrypoint() throws Exception {
    KubernetesPreprocessTarget target = buildTarget(config);

    target.dispatch(JOB_ID, PREFIX, SIMULATION, buildParams());

    Job job = jobCaptor.getValue();
    Container container = getContainer(job);
    List<String> command = container.getCommand();
    assertEquals("/app/preprocess-entrypoint.sh", command.get(0));
    assertEquals("/app/joshsim-fat.jar", command.get(1));
  }

  @Test
  void dispatchSetsPreprocessJobLabels() throws Exception {
    KubernetesPreprocessTarget target = buildTarget(config);

    target.dispatch(JOB_ID, PREFIX, SIMULATION, buildParams());

    Job job = jobCaptor.getValue();
    assertEquals("josh-" + JOB_ID, job.getMetadata().getName());
    assertEquals(JOB_ID, job.getMetadata().getLabels().get("josh-job-id"));
    assertEquals("joshsim", job.getMetadata().getLabels().get("app"));
    assertEquals("preprocess", job.getMetadata().getLabels().get("josh-job-type"));
  }

  @Test
  void dispatchSetsMinioSecretEnvVars() throws Exception {
    KubernetesPreprocessTarget target = buildTarget(config);

    target.dispatch(JOB_ID, PREFIX, SIMULATION, buildParams());

    Job job = jobCaptor.getValue();
    List<EnvVar> envVars = getContainer(job).getEnv();
    String secretName = "josh-creds-" + JOB_ID;

    assertSecretEnvVar(envVars, "MINIO_ENDPOINT", secretName);
    assertSecretEnvVar(envVars, "MINIO_ACCESS_KEY", secretName);
    assertSecretEnvVar(envVars, "MINIO_SECRET_KEY", secretName);
    assertSecretEnvVar(envVars, "MINIO_BUCKET", secretName);
  }

  @Test
  void dispatchSetsPreprocessEnvVars() throws Exception {
    KubernetesPreprocessTarget target = buildTarget(config);
    PreprocessParams params = buildParams();

    target.dispatch(JOB_ID, PREFIX, SIMULATION, params);

    Job job = jobCaptor.getValue();
    List<EnvVar> envVars = getContainer(job).getEnv();
    assertPlainEnvVar(envVars, "JOSH_JOB_ID", JOB_ID);
    assertPlainEnvVar(envVars, "JOSH_MINIO_PREFIX", PREFIX);
    assertPlainEnvVar(envVars, "JOSH_SIMULATION", SIMULATION);
    assertPlainEnvVar(envVars, "JOSH_DATA_FILE", "data.nc");
    assertPlainEnvVar(envVars, "JOSH_VARIABLE", "temperature");
    assertPlainEnvVar(envVars, "JOSH_UNITS", "celsius");
    assertPlainEnvVar(envVars, "JOSH_OUTPUT_FILE", "output.jshd");
    assertPlainEnvVar(envVars, "JOSH_CRS", "EPSG:4326");
    assertPlainEnvVar(envVars, "JOSH_X_COORD", "lon");
    assertPlainEnvVar(envVars, "JOSH_Y_COORD", "lat");
    assertPlainEnvVar(envVars, "JOSH_TIME_DIM", "calendar_year");
  }

  @Test
  void dispatchIncludesOptionalTimestepEnvVar() throws Exception {
    KubernetesPreprocessTarget target = buildTarget(config);
    PreprocessParams params = new PreprocessParams(
        "data.nc", "temp", "celsius", "output.jshd",
        "EPSG:4326", "lon", "lat", "calendar_year",
        "2020", null, false, false
    );

    target.dispatch(JOB_ID, PREFIX, SIMULATION, params);

    Job job = jobCaptor.getValue();
    List<EnvVar> envVars = getContainer(job).getEnv();
    assertPlainEnvVar(envVars, "JOSH_TIMESTEP", "2020");
  }

  @Test
  void dispatchIncludesParallelEnvVarWhenTrue() throws Exception {
    KubernetesPreprocessTarget target = buildTarget(config);
    PreprocessParams params = new PreprocessParams(
        "data.nc", "temp", "celsius", "output.jshd",
        "EPSG:4326", "lon", "lat", "calendar_year",
        null, null, true, false
    );

    target.dispatch(JOB_ID, PREFIX, SIMULATION, params);

    Job job = jobCaptor.getValue();
    List<EnvVar> envVars = getContainer(job).getEnv();
    assertPlainEnvVar(envVars, "JOSH_PARALLEL", "true");
  }

  @Test
  void dispatchCreatesSecretWithPodEndpoint() throws Exception {
    KubernetesPreprocessTarget target = buildTarget(config);

    target.dispatch(JOB_ID, PREFIX, SIMULATION, buildParams());

    Secret secret = secretCaptor.getValue();
    assertNotNull(secret);
    Map<String, String> data = secret.getData();
    String decodedEndpoint = new String(
        java.util.Base64.getDecoder().decode(data.get("MINIO_ENDPOINT"))
    );
    assertEquals(POD_ENDPOINT, decodedEndpoint);
  }

  @Test
  void dispatchHandlesNullResources() throws Exception {
    config = buildConfig(10, 3600, null);
    KubernetesPreprocessTarget target = buildTarget(config);

    target.dispatch(JOB_ID, PREFIX, SIMULATION, buildParams());

    Job job = jobCaptor.getValue();
    assertNull(getContainer(job).getResources());
  }

  // --- Helpers ---

  private PreprocessParams buildParams() {
    return new PreprocessParams(
        "data.nc", "temperature", "celsius", "output.jshd",
        "EPSG:4326", "lon", "lat", "calendar_year",
        null, null, false, false
    );
  }

  private KubernetesPreprocessTarget buildTarget(KubernetesTargetConfig cfg) {
    return new KubernetesPreprocessTarget(cfg, buildTestMinioOptions(), mockClient);
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
      int parallelism, int timeout,
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

  private static void setField(Object obj, String name, Object value) throws Exception {
    java.lang.reflect.Field field = obj.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(obj, value);
  }

  private Container getContainer(Job job) {
    return job.getSpec().getTemplate().getSpec().getContainers().get(0);
  }

  private void assertSecretEnvVar(List<EnvVar> envVars, String envName, String secretName) {
    EnvVar found = envVars.stream()
        .filter(ev -> envName.equals(ev.getName()))
        .findFirst()
        .orElse(null);
    assertNotNull(found, "Missing env var: " + envName);
    assertNotNull(found.getValueFrom(), envName + " should use valueFrom");
    assertEquals(secretName, found.getValueFrom().getSecretKeyRef().getName());
  }

  private void assertPlainEnvVar(List<EnvVar> envVars, String name, String value) {
    EnvVar found = envVars.stream()
        .filter(ev -> name.equals(ev.getName()))
        .findFirst()
        .orElse(null);
    assertNotNull(found, "Missing env var: " + name);
    assertEquals(value, found.getValue());
  }
}
