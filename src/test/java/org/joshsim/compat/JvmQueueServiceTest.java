package org.joshsim.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
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

}
