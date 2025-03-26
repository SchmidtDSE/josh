/**
 * Convienence functions to create entities.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import org.joshsim.engine.geometry.Geometry;


/**
 * Builder to assist in constructing entities.
 *
 * <p>Builder for creating Entity instances, providing methods to add event handlers and build the
 * final entity.
 * </p>
 */
public interface EntityBuilder {
  /**
   * Add event handlers to the entity being built.
   *
   * @param group the event handler group to add
   * @return this builder for method chaining
   */
  EntityBuilder addEventHandlerGroup(EventHandlerGroup group);
  
  /**
   * Build an agent entity.
   *
   * @param parent The entity like Patch that this will be part of.
   * @return A constructed agent entity
   */
  Agent buildAgent(SpatialEntity parent);

  /**
   * Build a disturbance entity.
   *
   * @param parent The entity like Patch that this will be part of.
   * @return A constructed disturbance entity
   */
  Disturbance buildDisturbance(SpatialEntity parent);

  /**
   * Build a patch entity.
   *
   * @param geometry The geometry defining the bounds of this Patch.
   * @return A constructed patch entity
   */
  Patch buildPatch(Geometry geometry);

  /**
   * Build a simulation instance.
   *
   * @param parent The entity like Patch that this will be part of.
   * @return A constructed simulation instance
   */
  Simulation buildSimulation(SpatialEntity parent);
}
