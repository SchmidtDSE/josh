package org.joshsim.pipeline.remote.batch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import org.joshsim.pipeline.job.JoshJob;
import org.joshsim.pipeline.job.JoshJobBuilder;
import org.joshsim.pipeline.remote.RunRemoteContext;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import org.joshsim.util.ProgressCalculator;
import org.joshsim.util.SimulationMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for BatchJobStrategy with mocked RemoteBatchTarget.
 */
public class BatchJobStrategyTest {

  @Mock
  private RemoteBatchTarget mockTarget;

  @TempDir
  File tempDir;

  private BatchJobStrategy strategy;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    strategy = new BatchJobStrategy(mockTarget);
  }

  @Test
  void generateJobId_shouldReturnNonEmpty() {
    String jobId = BatchJobStrategy.generateJobId();
    assertNotNull(jobId);
    assertEquals(8, jobId.length());
  }

  @Test
  void generateJobId_shouldBeUnique() {
    String id1 = BatchJobStrategy.generateJobId();
    String id2 = BatchJobStrategy.generateJobId();
    assertTrue(!id1.equals(id2), "Two generated IDs should be different");
  }

  @Test
  void buildSpec_shouldPopulateFieldsFromContext() throws Exception {
    RunRemoteContext context = buildTestContext(10);

    String jobId = "test-id";
    String inputPrefix = jobId + "/input/";
    BatchJobSpec spec = strategy.buildSpec(context, jobId, inputPrefix);

    assertEquals("test-id", spec.getJobId());
    assertEquals("TestSim", spec.getSimulation());
    assertEquals("ghcr.io/test/joshsim-job:latest", spec.getImage());
    assertEquals("test-id/input/", spec.getInputPrefix());
    assertEquals("test-id/output/", spec.getOutputPrefix());
    assertEquals(10, spec.getTotalReplicates());
    assertEquals(1, spec.getReplicatesPerJob());
    assertEquals("256Gi", spec.getMemory());
    assertEquals("64", spec.getCpu());
  }

  @Test
  void buildSpec_withDefaultParallelism_shouldUseReplicateCount() throws Exception {
    KubernetesConfig config = new KubernetesConfig.Builder()
        .setImage("test:latest")
        .setMemory("8Gi")
        .setParallelism(-1) // default: use replicate count
        .build();

    RunRemoteContext context = buildTestContext(25, config);

    BatchJobSpec spec = strategy.buildSpec(context, "test-id", "test-id/input/");
    assertEquals(25, spec.getMaxParallelism());
  }

  @Test
  void pollUntilTerminal_shouldReturnOnComplete() throws Exception {
    when(mockTarget.pollStatus(anyString()))
        .thenReturn(JobStatus.PENDING)
        .thenReturn(JobStatus.RUNNING)
        .thenReturn(JobStatus.COMPLETE);

    JobStatus result = strategy.pollUntilTerminal("test-job", new OutputOptions());

    assertEquals(JobStatus.COMPLETE, result);
    verify(mockTarget, atLeastOnce()).pollStatus("test-job");
  }

  @Test
  void pollUntilTerminal_shouldReturnOnFailed() throws Exception {
    when(mockTarget.pollStatus(anyString()))
        .thenReturn(JobStatus.RUNNING)
        .thenReturn(JobStatus.FAILED);

    JobStatus result = strategy.pollUntilTerminal("test-job", new OutputOptions());

    assertEquals(JobStatus.FAILED, result);
  }

  @Test
  void pollUntilTerminal_failedStatus_shouldReturnFailed() throws Exception {
    // Verify that a failed poll status is correctly returned
    when(mockTarget.pollStatus(anyString()))
        .thenReturn(JobStatus.PENDING)
        .thenReturn(JobStatus.FAILED);

    JobStatus result = strategy.pollUntilTerminal("test-job", new OutputOptions());

    assertEquals(JobStatus.FAILED, result);
    verify(mockTarget, atLeastOnce()).pollStatus("test-job");
  }

  /**
   * Builds a test RunRemoteContext with a valid Josh file.
   */
  private RunRemoteContext buildTestContext(int replicates) throws Exception {
    KubernetesConfig k8sConfig = new KubernetesConfig.Builder()
        .setImage("ghcr.io/test/joshsim-job:latest")
        .setMemory("256Gi")
        .setCpu("64")
        .setParallelism(10)
        .build();

    return buildTestContext(replicates, k8sConfig);
  }

  /**
   * Builds a test RunRemoteContext with specified config.
   */
  private RunRemoteContext buildTestContext(int replicates, KubernetesConfig k8sConfig)
      throws Exception {
    // Write a valid Josh file with minio export containing {replicate}
    File joshFile = new File(tempDir, "test.josh");
    String joshCode = "start unit year\n"
        + "  alias years\n"
        + "end unit\n"
        + "\n"
        + "start simulation TestSim\n"
        + "  grid.size = 100 m\n"
        + "  grid.low = 0 degrees latitude, 0 degrees longitude\n"
        + "  grid.high = 0.001 degrees latitude, 0.001 degrees longitude\n"
        + "  steps.low = 0 count\n"
        + "  steps.high = 2 count\n"
        + "  exportFiles.patch = \"minio://bucket/results/output_{replicate}.csv\"\n"
        + "end simulation\n"
        + "\n"
        + "start patch Default\n"
        + "  Tree.init = create 1 count of Tree\n"
        + "  export.avgAge.step = mean(Tree.age)\n"
        + "end patch\n"
        + "\n"
        + "start organism Tree\n"
        + "  age.init = 0 year\n"
        + "  age.step = prior.age + 1 year\n"
        + "end organism\n";
    Files.writeString(joshFile.toPath(), joshCode);

    JoshJob job = new JoshJobBuilder().setReplicates(replicates).build();
    SimulationMetadata metadata = new SimulationMetadata(0, 2, 3);
    ProgressCalculator calculator = new ProgressCalculator(3, replicates);

    return new RunRemoteContext(
        joshFile, "TestSim", false,
        null, null, job,
        joshCode, null,
        metadata, calculator,
        new OutputOptions(), new MinioOptions(),
        10,
        "kubernetes", k8sConfig, 1
    );
  }
}
