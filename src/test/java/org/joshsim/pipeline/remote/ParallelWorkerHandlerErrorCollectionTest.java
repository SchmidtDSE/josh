/**
 * Tests for ParallelWorkerHandler error-collection behavior.
 *
 * <p>Verifies that when some workers fail, all other workers are still allowed to complete
 * before the method throws. This prevents cascading ClosedChannelExceptions.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.undertow.Undertow;
import io.undertow.util.Headers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that ParallelWorkerHandler collects errors from all workers instead of
 * failing fast on the first error (which would abandon still-running workers).
 */
public class ParallelWorkerHandlerErrorCollectionTest {

  private Undertow server;

  /**
   * Stops the local Undertow server after each test to release any bound ports
   * and clean up resources.
   */
  @AfterEach
  public void tearDown() {
    if (server != null) {
      server.stop();
    }
  }

  /**
   * Starts a local Undertow server that returns HTTP 500 for specific replicate numbers
   * and successful streaming responses for others.
   */
  private int startMockWorkerServer(Set<Integer> failReplicates,
      Set<Integer> completedReplicates) {
    server = Undertow.builder()
        .addHttpListener(0, "127.0.0.1")
        .setHandler(exchange -> {
          exchange.getRequestReceiver().receiveFullString((ex, body) -> {
            // We use the 'name' param to encode replicate number for test control
            String name = "";
            for (String param : body.split("&")) {
              if (param.startsWith("name=")) {
                name = java.net.URLDecoder.decode(
                    param.substring(5), java.nio.charset.StandardCharsets.UTF_8);
              }
            }

            int replicateNum = -1;
            try {
              replicateNum = Integer.parseInt(name);
            } catch (NumberFormatException e) {
              // not a number, treat as normal
            }

            if (failReplicates.contains(replicateNum)) {
              ex.setStatusCode(500);
              ex.getResponseSender().send("Internal Server Error");
            } else {
              completedReplicates.add(replicateNum);
              ex.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
              ex.setStatusCode(200);
              // Send a minimal valid streaming response (progress + end)
              ex.getResponseSender().send(
                  "[progress 1]\n[end " + replicateNum + "]\n");
            }
          });
        })
        .build();
    server.start();

    // Extract the actual bound port
    return ((java.net.InetSocketAddress)
        server.getListenerInfo().get(0).getAddress()).getPort();
  }

  @Test
  public void testAllWorkersCompleteWhenSomeFail() {
    // Arrange: 5 tasks, replicates 2 and 4 will fail with HTTP 500
    Set<Integer> failReplicates = ConcurrentHashMap.newKeySet();
    failReplicates.add(2);
    failReplicates.add(4);
    Set<Integer> completedReplicates = ConcurrentHashMap.newKeySet();

    int port = startMockWorkerServer(failReplicates, completedReplicates);
    String workerUrl = "http://127.0.0.1:" + port;

    AtomicInteger cumulativeStepCount = new AtomicInteger(0);
    ParallelWorkerHandler handler = new ParallelWorkerHandler(workerUrl, 5, cumulativeStepCount);

    List<WorkerTask> tasks = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      // Use simulationName to encode replicate number for server-side routing
      tasks.add(new WorkerTask("code", String.valueOf(i), "key", "", false, i, "", false));
    }

    // Act: execute — should throw aggregate exception
    RuntimeException exception = assertThrows(RuntimeException.class, () ->
        handler.executeInParallel(tasks, null, (line, rep, ex, counter) -> {
          // no-op response handler for this test
        })
    );

    // Assert: aggregate exception mentions 2 failures out of 5
    assertTrue(exception.getMessage().contains("2 of 5 worker task(s) failed"),
        "Expected aggregate message, got: " + exception.getMessage());
    assertEquals(2, exception.getSuppressed().length,
        "Expected 2 suppressed exceptions");

    // Assert: the 3 non-failing workers completed successfully
    assertTrue(completedReplicates.contains(1), "Replicate 1 should have completed");
    assertTrue(completedReplicates.contains(3), "Replicate 3 should have completed");
    assertTrue(completedReplicates.contains(5), "Replicate 5 should have completed");
  }

  @Test
  public void testAllWorkersSucceed() {
    // Arrange: no failures
    Set<Integer> failReplicates = ConcurrentHashMap.newKeySet();
    Set<Integer> completedReplicates = ConcurrentHashMap.newKeySet();

    int port = startMockWorkerServer(failReplicates, completedReplicates);
    String workerUrl = "http://127.0.0.1:" + port;

    AtomicInteger cumulativeStepCount = new AtomicInteger(0);
    ParallelWorkerHandler handler = new ParallelWorkerHandler(workerUrl, 3, cumulativeStepCount);

    List<WorkerTask> tasks = new ArrayList<>();
    for (int i = 1; i <= 3; i++) {
      tasks.add(new WorkerTask("code", String.valueOf(i), "key", "", false, i, "", false));
    }

    // Act: should not throw
    handler.executeInParallel(tasks, null, (line, rep, ex, counter) -> {
      // no-op
    });

    // Assert: all 3 completed
    assertEquals(3, completedReplicates.size());
  }

  @Test
  public void testAllWorkersFail() {
    // Arrange: all fail
    Set<Integer> failReplicates = ConcurrentHashMap.newKeySet();
    failReplicates.add(1);
    failReplicates.add(2);
    Set<Integer> completedReplicates = ConcurrentHashMap.newKeySet();

    int port = startMockWorkerServer(failReplicates, completedReplicates);
    String workerUrl = "http://127.0.0.1:" + port;

    AtomicInteger cumulativeStepCount = new AtomicInteger(0);
    ParallelWorkerHandler handler = new ParallelWorkerHandler(workerUrl, 2, cumulativeStepCount);

    List<WorkerTask> tasks = new ArrayList<>();
    for (int i = 1; i <= 2; i++) {
      tasks.add(new WorkerTask("code", String.valueOf(i), "key", "", false, i, "", false));
    }

    // Act
    RuntimeException exception = assertThrows(RuntimeException.class, () ->
        handler.executeInParallel(tasks, null, (line, rep, ex, counter) -> {})
    );

    // Assert
    assertTrue(exception.getMessage().contains("2 of 2 worker task(s) failed"));
    assertEquals(2, exception.getSuppressed().length);
    assertTrue(completedReplicates.isEmpty(), "No replicates should have completed");
  }

  @Test
  public void testEmptyTaskList() {
    // Arrange
    Set<Integer> failReplicates = ConcurrentHashMap.newKeySet();
    Set<Integer> completedReplicates = ConcurrentHashMap.newKeySet();

    int port = startMockWorkerServer(failReplicates, completedReplicates);
    String workerUrl = "http://127.0.0.1:" + port;

    AtomicInteger cumulativeStepCount = new AtomicInteger(0);
    ParallelWorkerHandler handler = new ParallelWorkerHandler(workerUrl, 5, cumulativeStepCount);

    // Act: empty list should return immediately without error
    handler.executeInParallel(Collections.emptyList(), null, (line, rep, ex, counter) -> {});

    // Assert
    assertTrue(completedReplicates.isEmpty());
  }
}
