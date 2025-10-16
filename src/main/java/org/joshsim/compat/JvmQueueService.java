/**
 * JVM-compatible queue service implementation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A JVM-compatible implementation of QueueService.
 *
 * <p>This implementation provides a full-featured queue service for JVM environments using
 * concurrent data structures and thread management for asynchronous task processing.</p>
 */
public class JvmQueueService implements QueueService {

  private final BlockingQueue<Object> taskQueue;
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final AtomicBoolean active = new AtomicBoolean(false);
  private final QueueServiceCallback callback;

  /**
   * Creates a new JvmQueueService with the specified callback handler and capacity.
   *
   * @param callback The callback handler that will process queue events
   * @param capacity The maximum capacity for the queue
   */
  public JvmQueueService(QueueServiceCallback callback, int capacity) {
    this.callback = callback;
    this.taskQueue = new LinkedBlockingQueue<>(capacity);
  }

  /**
   * Creates a new JvmQueueService with the specified callback handler and default capacity.
   *
   * @param callback The callback handler that will process queue events
   */
  public JvmQueueService(QueueServiceCallback callback) {
    this(callback, 1000000);
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

    try {
      taskQueue.put(task);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while adding task to queue", e);
    }
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
