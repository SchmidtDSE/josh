/**
 * Thread safe export task queue.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.joshsim.lang.export.ExportTask;


/**
 * A thread-safe implementation of the ExportFacadeTaskQueue interface.
 */
public class ThreadSafeExportFacadeTaskQueue implements ExportFacadeTaskQueue {

  private final Queue<ExportTask> queue;

  /**
   * Constructs a ThreadSafeExportFacadeTaskQueue instance with an underlying thread-safe queue.
   */
  public ThreadSafeExportFacadeTaskQueue() {
    queue = new ConcurrentLinkedQueue<>();
  }

  @Override
  public void enqueue(ExportTask task) {
    queue.add(task);
  }

  @Override
  public ExportTask dequeue() {
    return queue.poll();
  }

}
