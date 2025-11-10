/**
 * Generic task representing data to be written to output.
 *
 * <p>This class provides a unified representation for queued write operations that work with
 * any data type. It encapsulates the data item along with metadata like step number, entity
 * type, and replicate number needed for proper output formatting and routing.</p>
 *
 * <p>WriteTask is used internally by OutputWriter implementations to queue data items for
 * asynchronous writing. The generic type parameter T allows the same task structure to be
 * used for both debug output (String) and export output (DataRow) or any other data type.</p>
 *
 * <p>This class replaces the separate DebugTask and export queue structures with a unified
 * generic implementation that works for all output types.</p>
 *
 * @param <T> The type of data being written (String for debug, DataRow for export, etc.)
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

/**
 * Generic task representing data to be written.
 *
 * <p>This class encapsulates all the information needed to write data, including the data
 * itself, the simulation step, entity type (for routing), and replicate number (for path
 * resolution).</p>
 *
 * @param <T> The type of data being written
 */
public class WriteTask<T> {

  private final T data;
  private final long step;
  private final String entityType;
  private final int replicateNumber;

  /**
   * Creates a new write task with all metadata.
   *
   * <p>This constructor includes all metadata fields for maximum flexibility. Entity type
   * is used by combined writers to route data to the correct per-entity-type writer, and
   * replicate number is used for path template resolution.</p>
   *
   * @param data The data item to write (String for debug, DataRow for export, etc.)
   * @param step The simulation step number
   * @param entityType The entity type name (e.g., "ForeverTree", "patch") for routing
   * @param replicateNumber The replicate number for path template resolution
   */
  public WriteTask(T data, long step, String entityType, int replicateNumber) {
    this.data = data;
    this.step = step;
    this.entityType = entityType;
    this.replicateNumber = replicateNumber;
  }

  /**
   * Creates a new write task with default entity type.
   *
   * <p>Convenience constructor for cases where entity type routing is not needed. The entity
   * type will be set to an empty string.</p>
   *
   * @param data The data item to write
   * @param step The simulation step number
   * @param replicateNumber The replicate number for path template resolution
   */
  public WriteTask(T data, long step, int replicateNumber) {
    this(data, step, "", replicateNumber);
  }

  /**
   * Creates a new write task with default replicate number.
   *
   * <p>Convenience constructor for single-replicate simulations. The replicate number
   * will be set to 0.</p>
   *
   * @param data The data item to write
   * @param step The simulation step number
   * @param entityType The entity type name for routing
   */
  public WriteTask(T data, long step, String entityType) {
    this(data, step, entityType, 0);
  }

  /**
   * Creates a new write task with minimal metadata.
   *
   * <p>Convenience constructor for simple cases where only data and step are needed.
   * Entity type will be empty and replicate number will be 0.</p>
   *
   * @param data The data item to write
   * @param step The simulation step number
   */
  public WriteTask(T data, long step) {
    this(data, step, "", 0);
  }

  /**
   * Gets the data item to be written.
   *
   * @return The data item (String for debug, DataRow for export, etc.)
   */
  public T getData() {
    return data;
  }

  /**
   * Gets the simulation step number.
   *
   * @return The step number
   */
  public long getStep() {
    return step;
  }

  /**
   * Gets the entity type name.
   *
   * <p>This is used by combined writers to route data to the correct per-entity-type writer.
   * For example, a CombinedTextWriter might route debug messages to different files based
   * on whether they come from "ForeverTree" or "patch" entities.</p>
   *
   * @return The entity type name, or empty string if not specified
   */
  public String getEntityType() {
    return entityType;
  }

  /**
   * Gets the replicate number.
   *
   * <p>This is used for path template resolution, allowing output paths to include the
   * replicate number via the {replicate} template variable.</p>
   *
   * @return The replicate number
   */
  public int getReplicateNumber() {
    return replicateNumber;
  }

  /**
   * Checks if this task has an entity type specified.
   *
   * @return true if entity type is non-empty, false otherwise
   */
  public boolean hasEntityType() {
    return entityType != null && !entityType.isEmpty();
  }

  @Override
  public String toString() {
    return String.format("WriteTask{data=%s, step=%d, entityType='%s', replicate=%d}",
        data, step, entityType, replicateNumber);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    WriteTask<?> writeTask = (WriteTask<?>) o;

    if (step != writeTask.step) {
      return false;
    }
    if (replicateNumber != writeTask.replicateNumber) {
      return false;
    }
    if (data != null ? !data.equals(writeTask.data) : writeTask.data != null) {
      return false;
    }
    return entityType != null
        ? entityType.equals(writeTask.entityType)
        : writeTask.entityType == null;
  }

  @Override
  public int hashCode() {
    int result = data != null ? data.hashCode() : 0;
    result = 31 * result + (int) (step ^ (step >>> 32));
    result = 31 * result + (entityType != null ? entityType.hashCode() : 0);
    result = 31 * result + replicateNumber;
    return result;
  }
}
