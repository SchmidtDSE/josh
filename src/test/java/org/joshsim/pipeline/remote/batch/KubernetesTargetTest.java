package org.joshsim.pipeline.remote.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joshsim.util.OutputOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for KubernetesTarget YAML rendering, status parsing, and command building.
 */
public class KubernetesTargetTest {

  private KubernetesConfig config;
  private KubernetesTarget target;

  @BeforeEach
  void setUp() {
    config = new KubernetesConfig.Builder()
        .setContext("test-context")
        .setNamespace("test-ns")
        .setImage("ghcr.io/schmidtdse/joshsim-job:latest")
        .setMemory("256Gi")
        .setCpu("64")
        .setBackoffLimit(2)
        .setParallelism(10)
        .setTimeoutSeconds(7200)
        .build();

    target = new KubernetesTarget(config, new OutputOptions());
  }

  @Test
  void renderJobYaml_shouldContainIndexedCompletions() {
    BatchJobSpec spec = buildSpec(50, 1, 10);

    String yaml = target.renderJobYaml(spec, "josh-sim-abc123");

    assertTrue(yaml.contains("completionMode: Indexed"));
    assertTrue(yaml.contains("completions: 50"));
    assertTrue(yaml.contains("parallelism: 10"));
  }

  @Test
  void renderJobYaml_shouldContainCorrectImage() {
    BatchJobSpec spec = buildSpec(5, 1, 5);

    String yaml = target.renderJobYaml(spec, "josh-sim-abc123");

    assertTrue(yaml.contains("image: ghcr.io/schmidtdse/joshsim-job:latest"));
  }

  @Test
  void renderJobYaml_shouldContainResourceRequests() {
    BatchJobSpec spec = buildSpec(5, 1, 5);

    String yaml = target.renderJobYaml(spec, "josh-sim-abc123");

    assertTrue(yaml.contains("memory: \"256Gi\""));
    assertTrue(yaml.contains("cpu: \"64\""));
  }

  @Test
  void renderJobYaml_shouldContainRunFromMinioCommand() {
    BatchJobSpec spec = buildSpec(5, 1, 5);

    String yaml = target.renderJobYaml(spec, "josh-sim-abc123");

    assertTrue(yaml.contains("runFromMinio"));
    assertTrue(yaml.contains("--input-prefix="));
    assertTrue(yaml.contains("--simulation=TestSim"));
    assertTrue(yaml.contains("--replicate-id=$(JOB_COMPLETION_INDEX)"));
  }

  @Test
  void renderJobYaml_shouldContainNamespaceAndName() {
    BatchJobSpec spec = buildSpec(5, 1, 5);

    String yaml = target.renderJobYaml(spec, "josh-sim-abc123");

    assertTrue(yaml.contains("name: josh-sim-abc123"));
    assertTrue(yaml.contains("namespace: test-ns"));
  }

  @Test
  void renderJobYaml_withNodeSelector_shouldIncludeLabels() {
    Map<String, String> nodeSelector = new HashMap<>();
    nodeSelector.put("nautilus.io/ram", "512gi");
    nodeSelector.put("nvidia.com/gpu.product", "A100");

    KubernetesConfig configWithSelector = new KubernetesConfig.Builder()
        .setImage("test:latest")
        .setMemory("256Gi")
        .setNamespace("test-ns")
        .setNodeSelector(nodeSelector)
        .build();

    KubernetesTarget targetWithSelector = new KubernetesTarget(
        configWithSelector, new OutputOptions()
    );

    BatchJobSpec spec = buildSpec(5, 1, 5);
    String yaml = targetWithSelector.renderJobYaml(spec, "josh-sim-abc123");

    assertTrue(yaml.contains("nodeSelector:"));
    assertTrue(yaml.contains("nautilus.io/ram: \"512gi\""));
  }

  @Test
  void renderJobYaml_withGpu_shouldIncludeGpuResources() {
    BatchJobSpec spec = new BatchJobSpec.Builder()
        .setJobId("abc123")
        .setSimulation("TestSim")
        .setImage("test:latest")
        .setInputPrefix("abc123/input/")
        .setOutputPrefix("abc123/output/")
        .setMemory("256Gi")
        .setCpu("64")
        .setGpu(2)
        .setTotalReplicates(5)
        .setMaxParallelism(5)
        .setTimeoutSeconds(3600)
        .build();

    String yaml = target.renderJobYaml(spec, "josh-sim-abc123");

    assertTrue(yaml.contains("nvidia.com/gpu: \"2\""));
  }

  @Test
  void renderJobYaml_withReplicatesPerJob_shouldComputeCorrectJobCount() {
    // 50 replicates / 5 per job = 10 jobs
    BatchJobSpec spec = new BatchJobSpec.Builder()
        .setJobId("abc123")
        .setSimulation("TestSim")
        .setImage("test:latest")
        .setInputPrefix("abc123/input/")
        .setOutputPrefix("abc123/output/")
        .setMemory("256Gi")
        .setCpu("64")
        .setTotalReplicates(50)
        .setReplicatesPerJob(5)
        .setMaxParallelism(10)
        .setTimeoutSeconds(3600)
        .build();

    assertEquals(10, spec.getJobCount());

    String yaml = target.renderJobYaml(spec, "josh-sim-abc123");
    assertTrue(yaml.contains("completions: 10"));
  }

  // --- Status parsing tests ---

  @Test
  void parseJobStatus_allSucceeded_shouldReturnComplete() {
    assertEquals(JobStatus.COMPLETE, target.parseJobStatus("50,0,0,50"));
  }

  @Test
  void parseJobStatus_someFailed_shouldReturnFailed() {
    assertEquals(JobStatus.FAILED, target.parseJobStatus("48,2,0,50"));
  }

  @Test
  void parseJobStatus_someActive_shouldReturnRunning() {
    assertEquals(JobStatus.RUNNING, target.parseJobStatus("10,0,5,50"));
  }

  @Test
  void parseJobStatus_noneStarted_shouldReturnPending() {
    assertEquals(JobStatus.PENDING, target.parseJobStatus("0,0,0,50"));
  }

  @Test
  void parseJobStatus_emptyFields_shouldReturnPending() {
    assertEquals(JobStatus.PENDING, target.parseJobStatus(",,,"));
  }

  @Test
  void parseJobStatus_partialSuccess_shouldReturnRunning() {
    assertEquals(JobStatus.RUNNING, target.parseJobStatus("25,0,0,50"));
  }

  // --- Command building tests ---

  @Test
  void buildKubectlCommand_shouldIncludeContextAndNamespace() {
    List<String> cmd = target.buildKubectlCommand("get", "pods");

    assertEquals("kubectl", cmd.get(0));
    assertEquals("--context=test-context", cmd.get(1));
    assertEquals("--namespace=test-ns", cmd.get(2));
    assertEquals("get", cmd.get(3));
    assertEquals("pods", cmd.get(4));
  }

  @Test
  void buildKubectlCommand_withoutContext_shouldOmitContextFlag() {
    KubernetesConfig noContextConfig = new KubernetesConfig.Builder()
        .setNamespace("default")
        .setImage("test:latest")
        .setMemory("8Gi")
        .build();

    KubernetesTarget noContextTarget = new KubernetesTarget(
        noContextConfig, new OutputOptions()
    );

    List<String> cmd = noContextTarget.buildKubectlCommand("get", "pods");

    assertEquals("kubectl", cmd.get(0));
    assertEquals("--namespace=default", cmd.get(1));
    assertEquals("get", cmd.get(2));
  }

  // --- Xmx computation tests ---

  @Test
  void computeXmxWithGiShouldReturnApprox80Percent() {
    // 256 * 0.8 = 204.8, truncated to 204
    assertEquals("204g", KubernetesTarget.computeXmx("256Gi"));
  }

  @Test
  void computeXmxWithSmallGiShouldTruncate() {
    // 8 * 0.8 = 6.4, truncated to 6
    assertEquals("6g", KubernetesTarget.computeXmx("8Gi"));
  }

  @Test
  void computeXmxWithMiShouldReturn80Percent() {
    // 4096 * 0.8 = 3276.8, truncated to 3276
    assertEquals("3276m", KubernetesTarget.computeXmx("4096Mi"));
  }

  @Test
  void computeXmx_null_shouldReturnDefault() {
    assertEquals("4g", KubernetesTarget.computeXmx(null));
  }

  // --- JobStatus enum tests ---

  @Test
  void jobStatus_terminalStates() {
    assertTrue(JobStatus.COMPLETE.isTerminal());
    assertTrue(JobStatus.FAILED.isTerminal());
    assertFalse(JobStatus.PENDING.isTerminal());
    assertFalse(JobStatus.RUNNING.isTerminal());
  }

  /**
   * Helper to build a BatchJobSpec with common defaults.
   */
  private BatchJobSpec buildSpec(int replicates, int perJob, int parallelism) {
    return new BatchJobSpec.Builder()
        .setJobId("abc123")
        .setSimulation("TestSim")
        .setImage("ghcr.io/schmidtdse/joshsim-job:latest")
        .setInputPrefix("abc123/input/")
        .setOutputPrefix("abc123/output/")
        .setMemory("256Gi")
        .setCpu("64")
        .setTotalReplicates(replicates)
        .setReplicatesPerJob(perJob)
        .setMaxParallelism(parallelism)
        .setTimeoutSeconds(7200)
        .build();
  }
}
