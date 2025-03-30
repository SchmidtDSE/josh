/**
 * Structures describing an individual timestep within a replicate.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.simulation;

import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.geometry.Geometry;


/**
 * Structure representing a discrete time step within a simulation.
 *
 * <p>Provides methods to retrieve entities at a specific point in time.
 * </p>
 */
public interface TimeStep {

  // TODO

  /**
   * Get the time step number.
   *
   * @return the integer time step number
   */
  long getTimeStep();

  /**
   * Get entities within the specified geometry at this time step.
   *
   * @param geometry the spatial bounds to query
   * @return an iterable of entities within the geometry
   */
  Iterable<Entity> getEntities(Geometry geometry);

  /**
   * Get entities with the specified name within the geometry at this time step.
   *
   * @param geometry the spatial bounds to query
   * @param name the entity name to filter by
   * @return an iterable of matching entities
   */
  Iterable<Entity> getEntities(Geometry geometry, String name);

  /**
   * Get all entities at this time step.
   *
   * @return an iterable of all entities
   */
  Iterable<Entity> getEntities();
}
