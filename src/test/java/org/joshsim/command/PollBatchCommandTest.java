/**
 * Tests for PollBatchCommand.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joshsim.pipeline.target.JobStatus;
import org.junit.jupiter.api.Test;


/**
 * Unit tests for {@link PollBatchCommand}.
 */
class PollBatchCommandTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void exitCodeForComplete() {
    assertEquals(
        PollBatchCommand.EXIT_COMPLETE,
        PollBatchCommand.exitCodeFor(JobStatus.State.COMPLETE)
    );
  }

  @Test
  void exitCodeForError() {
    assertEquals(
        PollBatchCommand.EXIT_ERROR,
        PollBatchCommand.exitCodeFor(JobStatus.State.ERROR)
    );
  }

  @Test
  void exitCodeForRunning() {
    assertEquals(
        PollBatchCommand.EXIT_RUNNING,
        PollBatchCommand.exitCodeFor(JobStatus.State.RUNNING)
    );
  }

  @Test
  void exitCodeForPending() {
    assertEquals(
        PollBatchCommand.EXIT_RUNNING,
        PollBatchCommand.exitCodeFor(JobStatus.State.PENDING)
    );
  }

  @Test
  void formatJsonComplete() throws Exception {
    PollBatchCommand cmd = buildCommand("job-42");
    JobStatus status = new JobStatus(
        JobStatus.State.COMPLETE,
        null,
        "2026-04-15T10:00:00Z"
    );

    JsonNode node = MAPPER.readTree(cmd.formatJson(status));

    assertEquals("complete", node.get("status").asText());
    assertEquals("job-42", node.get("jobId").asText());
    assertEquals(
        "2026-04-15T10:00:00Z",
        node.get("completedAt").asText()
    );
    assertTrue(node.path("message").isMissingNode());
  }

  @Test
  void formatJsonError() throws Exception {
    PollBatchCommand cmd = buildCommand("job-99");
    JobStatus status = new JobStatus(
        JobStatus.State.ERROR,
        "OOMKilled",
        "2026-04-15T11:00:00Z"
    );

    JsonNode node = MAPPER.readTree(cmd.formatJson(status));

    assertEquals("error", node.get("status").asText());
    assertEquals("job-99", node.get("jobId").asText());
    assertEquals("OOMKilled", node.get("message").asText());
    assertEquals(
        "2026-04-15T11:00:00Z",
        node.get("failedAt").asText()
    );
  }

  @Test
  void formatJsonRunning() throws Exception {
    PollBatchCommand cmd = buildCommand("job-7");
    JobStatus status = new JobStatus(
        JobStatus.State.RUNNING,
        null,
        "2026-04-15T09:00:00Z"
    );

    JsonNode node = MAPPER.readTree(cmd.formatJson(status));

    assertEquals("running", node.get("status").asText());
    assertEquals("job-7", node.get("jobId").asText());
    assertEquals(
        "2026-04-15T09:00:00Z",
        node.get("startedAt").asText()
    );
  }

  @Test
  void formatJsonPendingNoTimestamp() throws Exception {
    PollBatchCommand cmd = buildCommand("job-new");
    JobStatus status = new JobStatus(JobStatus.State.PENDING);

    JsonNode node = MAPPER.readTree(cmd.formatJson(status));

    assertEquals("pending", node.get("status").asText());
    assertEquals("job-new", node.get("jobId").asText());
    assertTrue(node.path("startedAt").isMissingNode());
    assertTrue(node.path("message").isMissingNode());
  }

  /**
   * Creates a PollBatchCommand with the jobId field set via reflection.
   */
  private PollBatchCommand buildCommand(String jobId) {
    try {
      PollBatchCommand cmd = new PollBatchCommand();
      java.lang.reflect.Field field =
          PollBatchCommand.class.getDeclaredField("jobId");
      field.setAccessible(true);
      field.set(cmd, jobId);
      return cmd;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
