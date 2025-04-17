package org.joshsim.lang.export;

import org.joshsim.engine.entity.base.Entity;


/**
 * Task which describes an entity which should be exported on a given step.
 */
public class ExportTask {

  private final Entity entity;

  private final long step;

  /**
   * Constructs a new Task with the specified entity and step value.
   *
   * @param entity The entity associated with this task.
   * @param step   The step value representing additional metadata for this task.
   */
  public ExportTask(Entity entity, long step) {
    this.entity = entity;
    this.step = step;
  }

  /**
   * Get the entity associated with this task.
   *
   * @return The entity object.
   */
  public Entity getEntity() {
    return entity;
  }

  /**
   * Get the step value for this task.
   *
   * @return The step value as a long.
   */
  public long getStep() {
    return step;
  }

}
