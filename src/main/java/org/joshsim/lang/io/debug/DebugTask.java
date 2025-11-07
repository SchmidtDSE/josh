/**
 * Task representing a debug message to be written.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.debug;

/**
 * Task representing a debug message to be written.
 *
 * <p>This class encapsulates all the information needed to write a debug message,
 * including the message itself, the simulation step, entity type, and replicate number.</p>
 */
public class DebugTask {
  private final String message;
  private final long step;
  private final String entityType;
  private final int replicateNumber;

  /**
   * Creates a new debug task.
   *
   * @param message The debug message to write.
   * @param step The simulation step number.
   * @param entityType The entity type name (e.g., "ForeverTree").
   * @param replicateNumber The replicate number.
   */
  public DebugTask(String message, long step, String entityType, int replicateNumber) {
    this.message = message;
    this.step = step;
    this.entityType = entityType;
    this.replicateNumber = replicateNumber;
  }

  /**
   * Gets the debug message.
   *
   * @return The debug message.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Gets the simulation step number.
   *
   * @return The step number.
   */
  public long getStep() {
    return step;
  }

  /**
   * Gets the entity type name.
   *
   * @return The entity type name.
   */
  public String getEntityType() {
    return entityType;
  }

  /**
   * Gets the replicate number.
   *
   * @return The replicate number.
   */
  public int getReplicateNumber() {
    return replicateNumber;
  }
}
