/**
 * Simple non-thread safe alternative to ExportFacadeTaskQueue.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.export;

import java.util.LinkedList;
import java.util.Queue;


/**
 * A simple ExportFacadeTaskQueue interface that provides a non-thread-safe queue for JS clients.
 */
public class SimpleExportFacadeTaskQueue implements ExportFacadeTaskQueue {

  private final Queue<ExportTask> queue;

  /**
   * Constructs a queue without thread safety.
   */
  public SimpleExportFacadeTaskQueue() {
    queue = new LinkedList<>();
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
