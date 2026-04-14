/**
 * Tests for JobStatus.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


/**
 * Unit tests for {@link JobStatus}.
 */
class JobStatusTest {

  @Test
  void stateOnlyConstructor() {
    JobStatus status = new JobStatus(JobStatus.State.RUNNING);
    assertEquals(JobStatus.State.RUNNING, status.getState());
    assertTrue(status.getMessage().isEmpty());
    assertTrue(status.getTimestamp().isEmpty());
  }

  @Test
  void fullConstructor() {
    JobStatus status = new JobStatus(
        JobStatus.State.ERROR, "OOMKilled", "2026-04-14T12:00:00Z"
    );
    assertEquals(JobStatus.State.ERROR, status.getState());
    assertEquals("OOMKilled", status.getMessage().get());
    assertEquals("2026-04-14T12:00:00Z", status.getTimestamp().get());
  }

  @Test
  void completeIsTerminal() {
    assertTrue(new JobStatus(JobStatus.State.COMPLETE).isTerminal());
  }

  @Test
  void errorIsTerminal() {
    assertTrue(new JobStatus(JobStatus.State.ERROR).isTerminal());
  }

  @Test
  void runningIsNotTerminal() {
    assertFalse(new JobStatus(JobStatus.State.RUNNING).isTerminal());
  }

  @Test
  void pendingIsNotTerminal() {
    assertFalse(new JobStatus(JobStatus.State.PENDING).isTerminal());
  }

  @Test
  void nullMessageReturnsEmpty() {
    JobStatus status = new JobStatus(JobStatus.State.RUNNING, null, null);
    assertTrue(status.getMessage().isEmpty());
  }

  @Test
  void nullTimestampReturnsEmpty() {
    JobStatus status = new JobStatus(JobStatus.State.RUNNING, null, null);
    assertTrue(status.getTimestamp().isEmpty());
  }
}
