
/**
 * Interface for queue service event callbacks.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;

import java.util.Optional;

/**
 * Callback interface for handling queue service lifecycle and task processing events.
 *
 * <p>This interface defines methods that are called by the queue service to notify
 * implementers about various events in the queue service lifecycle, including service
 * startup, task processing, and service shutdown.</p>
 */
public interface QueueServiceCallback {

  /**
   * Called when the queue service starts processing tasks.
   *
   * <p>This method is invoked once when the queue service begins operation,
   * before any tasks are processed.</p>
   */
  void onStart();

  /**
   * Called when the queue service processes a task.
   *
   * <p>This method is invoked for each task in the queue. An empty Optional indicates
   * that no task was available for processing at the time.</p>
   *
   * @param task An Optional containing the task to be processed, or empty if no task is available
   */
  void onTask(Optional<Object> task);

  /**
   * Called when the queue service completes all tasks and terminates.
   *
   * <p>This method is invoked once when the queue service finishes processing all tasks
   * and is shutting down.</p>
   */
  void onEnd();

}
