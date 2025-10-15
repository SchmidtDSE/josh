package org.joshsim.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;


class JvmQueueServiceTest {

  @Test
  void testNoTasks() throws InterruptedException {
    TestCallback callback = new TestCallback();
    JvmQueueService jvmQueueService = new JvmQueueService(callback);

    jvmQueueService.start();
    jvmQueueService.join();

    assertEquals(callback.getStartCalls(), 1);
    assertEquals(callback.getEndCalls(), 1);
  }

  @Test
  void testProcessTasks() throws InterruptedException {
    TestCallback callback = new TestCallback();
    JvmQueueService jvmQueueService = new JvmQueueService(callback);

    jvmQueueService.start();
    jvmQueueService.add("Task1");
    jvmQueueService.add("Task2");
    jvmQueueService.join(); // Allow some time for tasks to process

    assertEquals(callback.getStartCalls(), 1);
    assertTrue(callback.getTaskCalls() >= 2);
    assertEquals(callback.getEndCalls(), 1);
  }

  @Test
  void testCapacityConstructor() throws InterruptedException {
    TestCallback callback = new TestCallback();
    JvmQueueService jvmQueueService = new JvmQueueService(callback, 10);

    jvmQueueService.start();
    jvmQueueService.add("Task1");
    jvmQueueService.add("Task2");
    jvmQueueService.join();

    assertEquals(callback.getStartCalls(), 1);
    assertTrue(callback.getTaskCalls() >= 2);
    assertEquals(callback.getEndCalls(), 1);
  }

  @Test
  void testBoundedQueueWithCapacity() throws InterruptedException {
    // Test that we can successfully create and use a queue with specific capacity
    // The actual blocking behavior is integration-level and harder to test reliably
    // in unit tests due to timing issues
    CountingCallback callback = new CountingCallback();
    JvmQueueService jvmQueueService = new JvmQueueService(callback, 100);

    jvmQueueService.start();

    // Add tasks up to capacity
    for (int i = 0; i < 50; i++) {
      jvmQueueService.add("Task" + i);
    }

    jvmQueueService.join();

    // All tasks should have been processed
    assertEquals(50, callback.getProcessedTaskCount());
  }

  @Test
  void testAllTasksProcessedWithBoundedQueue() throws InterruptedException {
    CountingCallback callback = new CountingCallback();
    JvmQueueService jvmQueueService = new JvmQueueService(callback, 10);

    jvmQueueService.start();

    // Add more tasks than capacity
    int taskCount = 50;
    for (int i = 0; i < taskCount; i++) {
      jvmQueueService.add("Task" + i);
    }

    jvmQueueService.join();

    // All tasks should have been processed
    assertEquals(taskCount, callback.getProcessedTaskCount());
  }

  @Test
  void testCannotAddWhenNotActive() {
    TestCallback callback = new TestCallback();
    JvmQueueService jvmQueueService = new JvmQueueService(callback, 10);

    // Don't start the service
    assertThrows(IllegalStateException.class, () -> {
      jvmQueueService.add("Task1");
    });
  }

  private class TestCallback implements QueueServiceCallback {

    private int startCalls;
    private int taskCalls;
    private int endCalls;

    public TestCallback() {
      startCalls = 0;
      taskCalls = 0;
      endCalls = 0;
    }

    @Override
    public void onStart() {
      startCalls++;
    }

    @Override
    public void onTask(Optional<Object> task) {
      taskCalls++;
    }

    @Override
    public void onEnd() {
      endCalls++;
    }

    public int getStartCalls() {
      return startCalls;
    }

    public int getTaskCalls() {
      return taskCalls;
    }

    public int getEndCalls() {
      return endCalls;
    }
  }

  private class CountingCallback implements QueueServiceCallback {

    private final AtomicInteger processedCount = new AtomicInteger(0);

    @Override
    public void onStart() {
      // No-op
    }

    @Override
    public void onTask(Optional<Object> task) {
      if (task.isPresent()) {
        processedCount.incrementAndGet();
      }
    }

    @Override
    public void onEnd() {
      // No-op
    }

    public int getProcessedTaskCount() {
      return processedCount.get();
    }
  }

}
