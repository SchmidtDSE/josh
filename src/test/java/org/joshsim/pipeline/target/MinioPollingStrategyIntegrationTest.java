/**
 * Integration tests for MinioPollingStrategy against live MinIO/GCS.
 *
 * <p>Requires MINIO_ENDPOINT, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, and MINIO_BUCKET
 * environment variables. Skipped if credentials are not available.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.joshsim.util.MinioHandler;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


/**
 * Integration tests that poll real status files in MinIO written by /runBatch.
 *
 * <p>These tests read status.json files from previous /runBatch executions
 * that exist in the josh-batch-storage bucket. They validate that
 * MinioPollingStrategy correctly parses all three lifecycle states
 * (running, complete, error) from real server output.</p>
 */
class MinioPollingStrategyIntegrationTest {

  private static MinioHandler minioHandler;
  private static boolean credentialsAvailable;

  @BeforeAll
  static void setUp() {
    try {
      MinioOptions options = new MinioOptions();
      OutputOptions output = new OutputOptions();
      minioHandler = new MinioHandler(options, output);
      credentialsAvailable = true;
    } catch (Exception e) {
      credentialsAvailable = false;
    }
  }

  @Test
  void pollsCompletedJobFromMinio() throws Exception {
    assumeTrue(credentialsAvailable, "MinIO credentials not available");

    MinioPollingStrategy strategy = new MinioPollingStrategy(minioHandler);
    JobStatus status = strategy.poll("local-test-001");

    assertEquals(JobStatus.State.COMPLETE, status.getState());
    assertTrue(status.isTerminal());
    assertTrue(status.getMessage().isEmpty());
    assertTrue(status.getTimestamp().isPresent());
    assertTrue(status.getTimestamp().get().startsWith("2026-"));
  }

  @Test
  void pollsErrorJobFromMinio() throws Exception {
    assumeTrue(credentialsAvailable, "MinIO credentials not available");

    MinioPollingStrategy strategy = new MinioPollingStrategy(minioHandler);
    JobStatus status = strategy.poll("err-006");

    assertEquals(JobStatus.State.ERROR, status.getState());
    assertTrue(status.isTerminal());
    assertTrue(status.getMessage().isPresent());
    assertTrue(status.getMessage().get().contains("Simulation not found"));
    assertTrue(status.getTimestamp().isPresent());
  }

  @Test
  void pollsRunningJobFromMinio() throws Exception {
    assumeTrue(credentialsAvailable, "MinIO credentials not available");

    MinioPollingStrategy strategy = new MinioPollingStrategy(minioHandler);
    JobStatus status = strategy.poll("slow-test-001");

    assertEquals(JobStatus.State.RUNNING, status.getState());
    assertFalse(status.isTerminal());
    assertTrue(status.getTimestamp().isPresent());
    assertTrue(status.getTimestamp().get().contains("2026-"));
  }

  @Test
  void pollsNonexistentJobReturnsPending() throws Exception {
    assumeTrue(credentialsAvailable, "MinIO credentials not available");

    MinioPollingStrategy strategy = new MinioPollingStrategy(minioHandler);
    JobStatus status = strategy.poll("nonexistent-job-xyz-999");

    assertEquals(JobStatus.State.PENDING, status.getState());
    assertFalse(status.isTerminal());
  }
}
