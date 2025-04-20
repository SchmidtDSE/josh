/**
 * Interface for a cross-VM compatible queue service.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;

/**
 * Platform-independent interface for queue-based task processing.
 *
 * <p>This interface provides a common abstraction for queue processing across different platforms
 * (JVM and WebAssembly). It supports basic queue operations for task management and is implemented
 * by both JvmQueueService for standard JVM environments and EmulatedQueueService for WebAssembly
 * environments.</p>
 */
public interface QueueService {

  /**
   * Starts the queue service and begins processing tasks.
   *
   * <p>This method initializes the queue service and prepares it to receive and process tasks.
   * Once started, the service will begin executing any queued tasks.</p>
   */
  void start();

  /**
   * Waits for all queued tasks to complete and then terminates the service.
   *
   * <p>This method blocks until all currently queued tasks have been processed, after which
   * the service will be shut down.</p>
   */
  void join();

  /**
   * Adds a new task to the queue for processing.
   *
   * @param task The task object to be queued for processing
   * @throws IllegalStateException If the service is not active when this method is called
   */
  void add(Object task);

}
