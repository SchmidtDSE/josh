/**
 * Structures describing an individual timestep within a replicate.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.simulation;

import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.geometry.Geometry;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Structure representing a discrete time step within a simulation.
 *
 * <p>Provides methods to retrieve entities at a specific point in time.
 * </p>
 */
public class TimeStep {
  private long timeStep;
  private List<Entity> entities;

  /**
   * Create a new time step.
   *
   * @param timeStep the integer time step number
   * @param entities the entities at this time step
   */
  TimeStep(long timeStep, List<Entity> entities) {
    this.timeStep = timeStep;
    this.entities = entities;
  }

  /**
   * Get the time step number.
   *
   * @return the integer time step number
   */
  long getTimeStep() {
    return timeStep;
  }

  /**
   * Get entities within the specified geometry at this time step.
   *
   * @param geometry the spatial bounds to query
   * @return an iterable of entities within the geometry
   */
  Iterable<Entity> getEntities(Geometry geometry) {
    List<Entity> selectedEntities = entities.stream()
        .filter(entity -> entity.getGeometry()
                  .map(geo -> geo.intersects(geometry))
                  .orElse(false))
        .collect(Collectors.toList());
    return selectedEntities;
  }

  /**
   * Get entities with the specified name within the geometry at this time step.
   *
   * @param geometry the spatial bounds to query
   * @param name the entity name to filter by
   * @return an iterable of matching entities
   */
  Iterable<Entity> getEntities(Geometry geometry, String name) {
    List<Entity> selectedEntities = entities.stream()
        .filter(entity -> entity.getName().equals(name))
        .filter(entity -> entity.getGeometry()
                  .map(geo -> geo.intersects(geometry))
                  .orElse(false))
        .collect(Collectors.toList());
    return selectedEntities;
  }

  /**
   * Get all entities at this time step.
   *
   * @return an iterable of all entities
   */
  Iterable<Entity> getEntities() {
    return entities;
  }
}
