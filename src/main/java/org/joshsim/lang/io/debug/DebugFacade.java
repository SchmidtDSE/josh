/**
 * Structures describing high level strategies to write debug messages to persistence.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.debug;

/**
 * Strategy to write debug messages to persistence in a separate writer thread.
 *
 * <p>Unlike ExportFacade which serializes entire entities, DebugFacade writes formatted
 * debug messages with contextual information like step number and entity type.</p>
 */
public interface DebugFacade {

  /**
   * Starts the debug process in a dedicated writer thread.
   *
   * <p>Begin processing debug messages to be written to the output target in a writer thread
   * which operates in the background, writing messages to the output location as they are
   * added to the queue. If the debug process is already active, calling this method has no
   * effect. For some implementations (like stdout or memory), this may be a no-op.</p>
   */
  void start();

  /**
   * Stops the debug process and waits for completion of the writer thread.
   *
   * <p>Ensure that all pending messages in the queue are processed and written to the output
   * before shutting down the writer thread, blocking until the writer thread is fully terminated
   * which occurs when its queue is empty. For some implementations (like stdout or memory),
   * this may be a no-op.</p>
   */
  void join();

  /**
   * Writes a debug message to the output target.
   *
   * <p>The message is formatted with step number and entity type information and written
   * to the configured output target.</p>
   *
   * @param message The debug message to write.
   * @param step The step number from the simulation.
   * @param entityType The entity type name (e.g., "ForeverTree", "patch").
   * @param replicateNumber The replicate number.
   */
  void write(String message, long step, String entityType, int replicateNumber);

  /**
   * Writes a debug message with default replicate number.
   *
   * @param message The debug message to write.
   * @param step The step number from the simulation.
   * @param entityType The entity type name.
   */
  void write(String message, long step, String entityType);
}
