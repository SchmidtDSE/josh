/**
 * Interface for task queues for exporting.
 */

package org.joshsim.compat;


import org.joshsim.lang.export.ExportTask;

/**
 * Interface representing a task queue for managing ExportTasks.
 */
public interface ExportFacadeTaskQueue {

  /**
   * Adds a new export task to the task queue for processing.
   *
   * @param task the export task to be added to the queue
   */
  void enqueue(ExportTask task);

  /**
   * Removes and retrieves the next export task from the task queue for processing.
   *
   * @return the next export task in the queue, or null if the queue is empty
   */
  ExportTask dequeue();

}
