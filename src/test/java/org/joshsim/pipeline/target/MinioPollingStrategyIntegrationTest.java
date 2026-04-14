/**
 * Integration tests for MinioPollingStrategy against a real MinIO instance.
 *
 * <p>Requires MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, and MINIO_BUCKET
 * environment variables pointing to a running MinIO service. In CI, these are
 * provided by the MinIO service container in test-minio.yaml.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.joshsim.util.MinioHandler;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


/**
 * Integration tests that seed status files into MinIO and poll them back
 * via MinioPollingStrategy.
 *
 * <p>Tests all three lifecycle states (running, complete, error) plus the
 * nonexistent-job case. Each test writes a known status.json to MinIO via
 * {@code MinioHandler.putBytes()}, then reads it back through the polling
 * strategy to verify end-to-end correctness.</p>
 */
class MinioPollingStrategyIntegrationTest {

  private static MinioHandler minioHandler;
  private static MinioPollingStrategy strategy;

  @BeforeAll
  static void setUp() throws Exception {
    MinioOptions options = new MinioOptions();
    OutputOptions output = new OutputOptions();
    minioHandler = new MinioHandler(options, output);
    strategy = new MinioPollingStrategy(minioHandler);

    // Seed status files for each lifecycle state
    seedStatus("integ-complete-001",
        "{\"status\":\"complete\",\"jobId\":\"integ-complete-001\","
            + "\"completedAt\":\"2026-04-14T12:00:00Z\"}");
    seedStatus("integ-running-001",
        "{\"status\":\"running\",\"jobId\":\"integ-running-001\","
            + "\"startedAt\":\"2026-04-14T11:00:00Z\"}");
    seedStatus("integ-error-001",
        "{\"status\":\"error\",\"jobId\":\"integ-error-001\","
            + "\"message\":\"Simulation not found: BadSim\","
            + "\"failedAt\":\"2026-04-14T11:30:00Z\"}");
  }

  private static void seedStatus(String jobId, String json) throws Exception {
    String path = "batch-status/" + jobId + "/status.json";
    minioHandler.putBytes(json.getBytes(StandardCharsets.UTF_8), path, "application/json");
  }

  @Test
  void pollsCompletedJob() throws Exception {
    JobStatus status = strategy.poll("integ-complete-001");

    assertEquals(JobStatus.State.COMPLETE, status.getState());
    assertTrue(status.isTerminal());
    assertTrue(status.getMessage().isEmpty());
    assertEquals("2026-04-14T12:00:00Z", status.getTimestamp().get());
  }

  @Test
  void pollsRunningJob() throws Exception {
    JobStatus status = strategy.poll("integ-running-001");

    assertEquals(JobStatus.State.RUNNING, status.getState());
    assertFalse(status.isTerminal());
    assertEquals("2026-04-14T11:00:00Z", status.getTimestamp().get());
  }

  @Test
  void pollsErrorJob() throws Exception {
    JobStatus status = strategy.poll("integ-error-001");

    assertEquals(JobStatus.State.ERROR, status.getState());
    assertTrue(status.isTerminal());
    assertEquals("Simulation not found: BadSim", status.getMessage().get());
    assertEquals("2026-04-14T11:30:00Z", status.getTimestamp().get());
  }

  @Test
  void pollsNonexistentJobReturnsPending() throws Exception {
    JobStatus status = strategy.poll("nonexistent-job-xyz-999");

    assertEquals(JobStatus.State.PENDING, status.getState());
    assertFalse(status.isTerminal());
  }
}
