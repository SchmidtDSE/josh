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
public class EntityBuilder {
  EventHandlerGroup eventHandlerGroup;
  

  /**
   * Add event handlers to the entity being built.
   *
   * @param group the event handler group to add
   * @return this builder for method chaining
   */
  EntityBuilder addEventHandlerGroup(EventHandlerGroup group) {
    this.eventHandlerGroup = group;
    return this;
  }

  /**
   * Build an agent entity.
   *
   * @param parent The entity like Patch that this will be part of.
   * @return A constructed agent entity
   */
  Agent buildAgent(SpatialEntity parent) {
    Agent agent = new Agent(parent);
    return agent;
  }

  /**
   * Build a disturbance entity.
   *
   * @param parent The entity like Patch that this will be part of.
   * @return A constructed disturbance entity
   */
  Disturbance buildDisturbance(SpatialEntity parent){
    Disturbance disturbance = new Disturbance(parent);
    return disturbance;
  }

  /**
   * Build a patch entity.
   *
   * @param geometry The geometry defining the bounds of this Patch.
   * @return A constructed patch entity
   */
  Patch buildPatch(Geometry geometry){
    Patch patch = new Patch(geometry);
    return patch;
  }

  /**
   * Build a simulation instance.
   *
   * @param parent The entity like Patch that this will be part of.
   * @return A constructed simulation instance
   */
  Simulation buildSimulation(SpatialEntity parent){
    Simulation simulation = new Simulation(parent);
    return simulation;
  }
}
