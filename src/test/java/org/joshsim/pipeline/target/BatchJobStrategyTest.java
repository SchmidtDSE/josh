/**
 * Tests for BatchJobStrategy.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.joshsim.util.MinioHandler;
import org.joshsim.util.OutputOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/**
 * Unit tests for {@link BatchJobStrategy}.
 */
class BatchJobStrategyTest {

  private RemoteBatchTarget target;
  private BatchPollingStrategy poller;
  private MinioHandler minioHandler;
  private OutputOptions output;

  @TempDir
  File tempDir;

  @BeforeEach
  void setUp() throws IOException {
    target = mock(RemoteBatchTarget.class);
    poller = mock(BatchPollingStrategy.class);
    minioHandler = mock(MinioHandler.class);
    output = new OutputOptions();

    // Create a test file in the temp directory
    Files.writeString(new File(tempDir, "test.josh").toPath(), "start simulation Test end");

    // Make uploads succeed by default
    when(minioHandler.uploadFile(any(File.class), anyString())).thenReturn(true);
  }

  @Test
  void executeStagesDispatchesAndPolls() throws Exception {
    when(poller.poll(anyString()))
        .thenReturn(new JobStatus(JobStatus.State.RUNNING))
        .thenReturn(new JobStatus(JobStatus.State.COMPLETE, null, "2026-04-14T12:00:00Z"));

    BatchJobStrategy strategy = new BatchJobStrategy(
        target, poller, minioHandler, output, 10, 60000
    );

    JobStatus result = strategy.execute(tempDir, "Test", 1);

    assertEquals(JobStatus.State.COMPLETE, result.getState());
    verify(minioHandler).uploadFile(any(File.class), anyString());
    verify(target).dispatch(anyString(), anyString(), eq("Test"), eq(1));
  }

  @Test
  void executePassesReplicatesToTarget() throws Exception {
    when(poller.poll(anyString()))
        .thenReturn(new JobStatus(JobStatus.State.COMPLETE));

    BatchJobStrategy strategy = new BatchJobStrategy(
        target, poller, minioHandler, output, 10, 60000
    );

    strategy.execute(tempDir, "Main", 10);

    verify(target).dispatch(anyString(), anyString(), eq("Main"), eq(10));
  }

  @Test
  void executeReturnsErrorOnPollError() throws Exception {
    when(poller.poll(anyString()))
        .thenReturn(new JobStatus(JobStatus.State.ERROR, "Simulation not found", null));

    BatchJobStrategy strategy = new BatchJobStrategy(
        target, poller, minioHandler, output, 10, 60000
    );

    JobStatus result = strategy.execute(tempDir, "BadSim", 1);

    assertEquals(JobStatus.State.ERROR, result.getState());
    assertEquals("Simulation not found", result.getMessage().get());
  }

  @Test
  void executeTimesOut() throws Exception {
    when(poller.poll(anyString()))
        .thenReturn(new JobStatus(JobStatus.State.RUNNING));

    // Very short timeout
    BatchJobStrategy strategy = new BatchJobStrategy(
        target, poller, minioHandler, output, 10, 50
    );

    JobStatus result = strategy.execute(tempDir, "SlowSim", 1);

    assertEquals(JobStatus.State.ERROR, result.getState());
    assertTrue(result.getMessage().get().contains("timed out"));
  }

  @Test
  void executeNoWaitSkipsPolling() throws Exception {
    BatchJobStrategy strategy = new BatchJobStrategy(
        target, poller, minioHandler, output, 10, 60000
    );

    String jobId = strategy.executeNoWait(tempDir, "Test", 5);

    assertTrue(jobId != null && !jobId.isEmpty());
    verify(target).dispatch(anyString(), anyString(), eq("Test"), eq(5));
    verify(poller, never()).poll(anyString());
  }

  @Test
  void executeThrowsOnStagingFailure() throws Exception {
    when(minioHandler.uploadFile(any(File.class), anyString())).thenReturn(false);

    BatchJobStrategy strategy = new BatchJobStrategy(
        target, poller, minioHandler, output, 10, 60000
    );

    assertThrows(IOException.class, () -> strategy.execute(tempDir, "Test", 1));
  }

  @Test
  void executeThrowsOnDispatchFailure() throws Exception {
    doThrow(new RuntimeException("Connection refused"))
        .when(target).dispatch(anyString(), anyString(), anyString(), eq(1));

    BatchJobStrategy strategy = new BatchJobStrategy(
        target, poller, minioHandler, output, 10, 60000
    );

    assertThrows(RuntimeException.class, () -> strategy.execute(tempDir, "Test", 1));
  }
}
