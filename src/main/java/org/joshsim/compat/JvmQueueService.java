package org.joshsim.compat;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;


public class JvmQueueService implements QueueService {

  private final Queue<Object> taskQueue = new ConcurrentLinkedQueue<>();
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final AtomicBoolean active = new AtomicBoolean(false);
  private final QueueServiceCallback callback;

  public JvmQueueService(QueueServiceCallback callback) {
    this.callback = callback;
  }

  @Override
  public void start() {
    if (active.compareAndSet(false, true)) {
      executorService.submit(() -> {
        callback.onStart();

        while (active.get() || !taskQueue.isEmpty()) {
          Object task = taskQueue.poll();
          if (task == null) {
            callback.onTask(Optional.empty());
            trySleep();
          } else {
            callback.onTask(Optional.of(task));
          }
        }

        callback.onEnd();
      });
    }
  }

  @Override
  public void join() {
    active.set(false);
    executorService.shutdown();
    while (!executorService.isTerminated()) {
      trySleep();
    }
  }

  @Override
  public void add(Object task) {
    if (!active.get()) {
      throw new IllegalStateException("Service is not active. Cannot write entities.");
    }

    taskQueue.add(task);
  }

  /**
   * Causes the executing thread to sleep to avoid thrashing.
   *
   * @throws RuntimeException If the thread is interrupted during the sleep operation.
   */
  private void trySleep() {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while sleeping", e);
    }
  }

}
