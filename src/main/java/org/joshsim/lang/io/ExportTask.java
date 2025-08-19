/**
 * Shared base class for export tasks containing either Entity or NamedMap data.
 *
 * <p>This class provides a unified representation for export operations that can work with
 * either Entity objects (traditional export path) or NamedMap objects (wire format path).
 * The class uses Optional fields to ensure only one type of data is present per task.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;

/**
 * Base class for export tasks that can contain either Entity or NamedMap data.
 *
 * <p>This class serves as a common base for all export tasks across different ExportFacade
 * implementations. It allows export operations to work with either traditional Entity objects
 * or pre-serialized NamedMap objects, enabling wire format deserialization to bypass the
 * Entity conversion step for improved performance.</p>
 */
public class ExportTask {

  private final Optional<Entity> entity;
  private final Optional<NamedMap> namedMap;
  private final long step;

  /**
   * Creates a new ExportTask containing an Entity.
   *
   * <p>This constructor is used for traditional export operations where an Entity needs to be
   * serialized and written to output. The namedMap field will be empty in this case.</p>
   *
   * @param entity The entity to be serialized and exported
   * @param step The simulation step number associated with this task
   * @throws IllegalArgumentException if entity is null
   */
  public ExportTask(Entity entity, long step) {
    if (entity == null) {
      throw new IllegalArgumentException("Entity cannot be null");
    }
    this.entity = Optional.of(entity);
    this.namedMap = Optional.empty();
    this.step = step;
  }

  /**
   * Creates a new ExportTask containing a NamedMap.
   *
   * <p>This constructor is used for wire format export operations where a NamedMap contains
   * pre-serialized data that can be written directly to output without Entity serialization.
   * The entity field will be empty in this case.</p>
   *
   * @param namedMap The pre-serialized named map to be exported
   * @param step The simulation step number associated with this task
   * @throws IllegalArgumentException if namedMap is null
   */
  public ExportTask(NamedMap namedMap, long step) {
    if (namedMap == null) {
      throw new IllegalArgumentException("NamedMap cannot be null");
    }
    this.entity = Optional.empty();
    this.namedMap = Optional.of(namedMap);
    this.step = step;
  }

  /**
   * Gets the Entity associated with this task, if present.
   *
   * @return Optional containing the Entity, or empty if this task contains a NamedMap
   */
  public Optional<Entity> getEntity() {
    return entity;
  }

  /**
   * Gets the NamedMap associated with this task, if present.
   *
   * @return Optional containing the NamedMap, or empty if this task contains an Entity
   */
  public Optional<NamedMap> getNamedMap() {
    return namedMap;
  }

  /**
   * Gets the simulation step number for this task.
   *
   * @return The step number as a long value
   */
  public long getStep() {
    return step;
  }

  /**
   * Checks if this task contains an Entity.
   *
   * @return true if this task contains an Entity, false if it contains a NamedMap
   */
  public boolean hasEntity() {
    return entity.isPresent();
  }

  /**
   * Checks if this task contains a NamedMap.
   *
   * @return true if this task contains a NamedMap, false if it contains an Entity
   */
  public boolean hasNamedMap() {
    return namedMap.isPresent();
  }

  @Override
  public String toString() {
    if (hasEntity()) {
      return String.format("ExportTask{entity=%s, step=%d}", entity.get(), step);
    } else {
      return String.format("ExportTask{namedMap=%s, step=%d}", namedMap.get(), step);
    }
  }
}