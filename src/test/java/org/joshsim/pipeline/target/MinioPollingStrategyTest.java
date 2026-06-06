/**
 * Tests for MinioPollingStrategy.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.joshsim.util.MinioHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Unit tests for {@link MinioPollingStrategy}.
 */
class MinioPollingStrategyTest {

  private MinioHandler minioHandler;
  private MinioPollingStrategy strategy;

  @BeforeEach
  void setUp() {
    minioHandler = mock(MinioHandler.class);
    strategy = new MinioPollingStrategy(minioHandler);
  }

  @Test
  void parsesRunningStatus() throws Exception {
    JobStatus status = strategy.parseStatusJson(
        "{\"status\":\"running\",\"jobId\":\"job-1\",\"startedAt\":\"2026-04-14T10:00:00Z\"}"
    );
    assertEquals(JobStatus.State.RUNNING, status.getState());
    assertTrue(status.getMessage().isEmpty());
    assertEquals("2026-04-14T10:00:00Z", status.getTimestamp().get());
  }

  @Test
  void parsesCompleteStatus() throws Exception {
    JobStatus status = strategy.parseStatusJson(
        "{\"status\":\"complete\",\"jobId\":\"job-1\",\"completedAt\":\"2026-04-14T11:00:00Z\"}"
    );
    assertEquals(JobStatus.State.COMPLETE, status.getState());
    assertTrue(status.getMessage().isEmpty());
    assertEquals("2026-04-14T11:00:00Z", status.getTimestamp().get());
  }

  @Test
  void parsesErrorStatus() throws Exception {
    JobStatus status = strategy.parseStatusJson(
        "{\"status\":\"error\",\"jobId\":\"job-1\","
            + "\"message\":\"Simulation not found: BadSim\","
            + "\"failedAt\":\"2026-04-14T11:30:00Z\"}"
    );
    assertEquals(JobStatus.State.ERROR, status.getState());
    assertEquals("Simulation not found: BadSim", status.getMessage().get());
    assertEquals("2026-04-14T11:30:00Z", status.getTimestamp().get());
  }

  @Test
  void unknownStatusMapsToPending() throws Exception {
    JobStatus status = strategy.parseStatusJson(
        "{\"status\":\"initializing\",\"jobId\":\"job-1\"}"
    );
    assertEquals(JobStatus.State.PENDING, status.getState());
  }

  @Test
  void missingStatusFieldMapsToPending() throws Exception {
    JobStatus status = strategy.parseStatusJson("{\"jobId\":\"job-1\"}");
    assertEquals(JobStatus.State.PENDING, status.getState());
  }

  @Test
  void pollReadsFromMinio() throws Exception {
    String json = "{\"status\":\"complete\",\"jobId\":\"job-42\","
        + "\"completedAt\":\"2026-04-14T12:00:00Z\"}";
    InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    when(minioHandler.downloadStream("batch-status/job-42/status.json")).thenReturn(stream);

    JobStatus status = strategy.poll("job-42");

    assertEquals(JobStatus.State.COMPLETE, status.getState());
    assertEquals("2026-04-14T12:00:00Z", status.getTimestamp().get());
  }

  @Test
  void pollReturnsPendingWhenStatusFileNotFound() throws Exception {
    when(minioHandler.downloadStream(anyString()))
        .thenThrow(new Exception("The specified key does not exist"));

    JobStatus status = strategy.poll("job-new");

    assertEquals(JobStatus.State.PENDING, status.getState());
  }

  @Test
  void pollReturnsPendingForNoSuchKeyError() throws Exception {
    when(minioHandler.downloadStream(anyString()))
        .thenThrow(new Exception("NoSuchKey: the object does not exist"));

    JobStatus status = strategy.poll("job-missing");

    assertEquals(JobStatus.State.PENDING, status.getState());
  }

  @Test
  void pollPropagatesNonNotFoundErrors() throws Exception {
    when(minioHandler.downloadStream(anyString()))
        .thenThrow(new Exception("Connection refused"));

    assertThrows(Exception.class, () -> strategy.poll("job-fail"));
  }

  @Test
  void pollPropagatesNullMessageErrors() throws Exception {
    when(minioHandler.downloadStream(anyString()))
        .thenThrow(new Exception((String) null));

    assertThrows(Exception.class, () -> strategy.poll("job-null"));
  }
}
