/**
 * Convienence functions to create entities.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.HashMap;
import java.util.Optional;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.EngineValue;

/**
 * Builder to assist in constructing entities.
 *
 * <p>Builder for creating Entity instances, providing methods to add event handlers and build the
 * final entity.
 * </p>
 */
public class EntityBuilder {
  String name;
  Optional<HashMap<EventKey, EventHandlerGroup>> eventHandlerGroups;
  Optional<HashMap<String, EngineValue>> attributes;

  /**
   * Add event handlers to the entity being built.
   *
   * @param eventKey the event key to add the handler to
   * @param group the event handler group to add
   * @return this builder for method chaining
   */
  EntityBuilder addEventHandlerGroup(EventKey eventKey, EventHandlerGroup eventHandlerGroup) {
    if (eventHandlerGroups.isEmpty()) {
      eventHandlerGroups = Optional.of(new HashMap<>());
    }
    eventHandlerGroups.get().put(eventKey, eventHandlerGroup);
    return this;
  }

  /**
   * Add attributes to the entity being built.
   *
   * @param attributes The attributes to add to the entity
   * @return this builder for method chaining
   */
  EntityBuilder addAttributes(HashMap<String, EngineValue> attributes) {
    if (this.attributes.isEmpty()) {
      this.attributes = Optional.of(new HashMap<>());
    }
    this.attributes.get().putAll(attributes);
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
    Simulation simulation = new Simulation();
    return simulation;
  }
}
