/**
 * Tests for BatchJobStrategy.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.joshsim.util.OutputOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;


/**
 * Unit tests for {@link BatchJobStrategy}.
 */
class BatchJobStrategyTest {

  private RemoteBatchTarget target;
  private BatchPollingStrategy poller;
  private OutputOptions output;

  @BeforeEach
  void setUp() {
    target = mock(RemoteBatchTarget.class);
    poller = mock(BatchPollingStrategy.class);
    output = new OutputOptions();
  }

  @Test
  void executeDispatchesAndPolls() throws Exception {
    when(poller.poll(anyString()))
        .thenReturn(new JobStatus(JobStatus.State.RUNNING))
        .thenReturn(new JobStatus(JobStatus.State.COMPLETE, null, "2026-04-14T12:00:00Z"));

    BatchJobStrategy strategy = new BatchJobStrategy(target, poller, output, 10, 60000);

    JobStatus result = strategy.execute("batch-jobs/foo/inputs/", "Test", 1);

    assertEquals(JobStatus.State.COMPLETE, result.getState());
    verify(target).dispatch(anyString(), eq("batch-jobs/foo/inputs/"), eq("Test"), eq(1));
  }

  @Test
  void executeNormalizesTrailingSlash() throws Exception {
    when(poller.poll(anyString())).thenReturn(new JobStatus(JobStatus.State.COMPLETE));

    BatchJobStrategy strategy = new BatchJobStrategy(target, poller, output, 10, 60000);

    strategy.execute("batch-jobs/foo/inputs", "Test", 1);

    ArgumentCaptor<String> prefixCaptor = ArgumentCaptor.forClass(String.class);
    verify(target).dispatch(anyString(), prefixCaptor.capture(), eq("Test"), eq(1));
    assertEquals("batch-jobs/foo/inputs/", prefixCaptor.getValue());
  }

  @Test
  void executePassesReplicatesToTarget() throws Exception {
    when(poller.poll(anyString()))
        .thenReturn(new JobStatus(JobStatus.State.COMPLETE));

    BatchJobStrategy strategy = new BatchJobStrategy(target, poller, output, 10, 60000);

    strategy.execute("batch-jobs/foo/inputs/", "Main", 10);

    verify(target).dispatch(anyString(), anyString(), eq("Main"), eq(10));
  }

  @Test
  void executeReturnsErrorOnPollError() throws Exception {
    when(poller.poll(anyString()))
        .thenReturn(new JobStatus(JobStatus.State.ERROR, "Simulation not found", null));

    BatchJobStrategy strategy = new BatchJobStrategy(target, poller, output, 10, 60000);

    JobStatus result = strategy.execute("batch-jobs/foo/inputs/", "BadSim", 1);

    assertEquals(JobStatus.State.ERROR, result.getState());
    assertEquals("Simulation not found", result.getMessage().get());
  }

  @Test
  void executeTimesOut() throws Exception {
    when(poller.poll(anyString())).thenReturn(new JobStatus(JobStatus.State.RUNNING));

    BatchJobStrategy strategy = new BatchJobStrategy(target, poller, output, 10, 50);

    JobStatus result = strategy.execute("batch-jobs/foo/inputs/", "SlowSim", 1);

    assertEquals(JobStatus.State.ERROR, result.getState());
    assertTrue(result.getMessage().get().contains("timed out"));
  }

  @Test
  void executeNoWaitSkipsPolling() throws Exception {
    BatchJobStrategy strategy = new BatchJobStrategy(target, poller, output, 10, 60000);

    String jobId = strategy.executeNoWait("batch-jobs/foo/inputs/", "Test", 5);

    assertTrue(jobId != null && !jobId.isEmpty());
    verify(target).dispatch(anyString(), eq("batch-jobs/foo/inputs/"), eq("Test"), eq(5));
    verify(poller, never()).poll(anyString());
  }

  @Test
  void executeThrowsOnDispatchFailure() throws Exception {
    doThrow(new RuntimeException("Connection refused"))
        .when(target).dispatch(anyString(), anyString(), anyString(), eq(1));

    BatchJobStrategy strategy = new BatchJobStrategy(target, poller, output, 10, 60000);

    assertThrows(RuntimeException.class,
        () -> strategy.execute("batch-jobs/foo/inputs/", "Test", 1));
  }
}
