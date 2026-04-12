/**
 * Concurrency tests for ProgressCalculator.
 *
 * <p>Verifies that synchronized methods prevent state corruption when
 * multiple threads call updateStep, updateReplicateCompleted, and
 * resetForNextReplicate concurrently.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Tests that ProgressCalculator's synchronized methods do not corrupt state
 * under concurrent access from multiple worker threads.
 */
public class ProgressCalculatorConcurrencyTest {

  @Test
  public void testConcurrentUpdateStepDoesNotCorruptState() throws Exception {
    // Arrange: 1000 steps, 10 replicates — shared across 10 threads
    ProgressCalculator calc = new ProgressCalculator(1000, 10);
    int threadCount = 10;
    int stepsPerThread = 100;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    List<Future<?>> futures = new ArrayList<>();

    // Act: all threads call updateStep concurrently
    for (int t = 0; t < threadCount; t++) {
      final int threadId = t;
      futures.add(executor.submit(() -> {
        try {
          startLatch.await(); // Synchronize start
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
        for (int step = 0; step < stepsPerThread; step++) {
          ProgressUpdate update = calc.updateStep(threadId * stepsPerThread + step);
          assertNotNull(update);
        }
      }));
    }

    startLatch.countDown(); // Release all threads simultaneously

    // Assert: no exceptions
    for (Future<?> future : futures) {
      assertDoesNotThrow(() -> future.get(10, TimeUnit.SECONDS));
    }

    executor.shutdown();
  }

  @Test
  public void testConcurrentResetAndUpdateDoNotCorruptState() throws Exception {
    // Simulates the pattern where END responses trigger resetForNextReplicate
    // while other threads are still calling updateStep
    ProgressCalculator calc = new ProgressCalculator(100, 5);
    int threadCount = 5;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    List<Future<?>> futures = new ArrayList<>();

    // Mix of updateStep and replicate-completion calls
    for (int t = 0; t < threadCount; t++) {
      final int threadId = t;
      futures.add(executor.submit(() -> {
        try {
          startLatch.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
        for (int i = 0; i < 50; i++) {
          ProgressUpdate update = calc.updateStep(i * 2);
          assertNotNull(update);
        }
        ProgressUpdate endUpdate = calc.updateReplicateCompleted(threadId + 1);
        assertNotNull(endUpdate);
      }));
    }

    startLatch.countDown();

    for (Future<?> future : futures) {
      assertDoesNotThrow(() -> future.get(10, TimeUnit.SECONDS));
    }

    executor.shutdown();
  }

  @Test
  public void testConcurrentResetForNextReplicate() throws Exception {
    // Multiple threads competing to reset — should not throw or corrupt
    ProgressCalculator calc = new ProgressCalculator(100, 100);
    int threadCount = 10;

    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    List<Future<?>> futures = new ArrayList<>();

    for (int t = 0; t < threadCount; t++) {
      final int threadId = t;
      futures.add(executor.submit(() -> {
        try {
          startLatch.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
        // Each thread cycles through resets and updates
        for (int rep = 1; rep <= 10; rep++) {
          calc.resetForNextReplicate(((threadId * 10 + rep - 1) % 100) + 1);
          for (int step = 0; step < 10; step++) {
            calc.updateStep(step * 10);
          }
        }
      }));
    }

    startLatch.countDown();

    for (Future<?> future : futures) {
      assertDoesNotThrow(() -> future.get(10, TimeUnit.SECONDS));
    }

    executor.shutdown();
  }
}
