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
  Optional<String> name;
  HashMap<EventKey, EventHandlerGroup> eventHandlerGroups = new HashMap<>();
  HashMap<String, EngineValue> attributes = new HashMap<>();

  /**
   * Set the name of the entity being built.
   *
   * @param name the name of the entity
   * @return this builder for method chaining
   */
  EntityBuilder setName(String name) {
    this.name = Optional.of(name);
    return this;
  }

  /**
   * Get the name of the entity being built.
   *
   * @return the name of the entity
   */
  String getName() {
    return name.orElseThrow(() -> new IllegalStateException("Name not set"));
  }

  /**
   * Clears the current state of the builder, resetting all fields to their default values.
   */
  public void clear() {
    name = Optional.empty();
    eventHandlerGroups.clear();
    attributes.clear();
  }

  /**
   * Add event handlers to the entity being built.
   *
   * @param eventKey the event key to add the handler to
   * @param group the event handler group to add
   * @return this builder for method chaining
   */
  EntityBuilder addEventHandlerGroup(EventKey eventKey, EventHandlerGroup eventHandlerGroup) {
    eventHandlerGroups.put(eventKey, eventHandlerGroup);
    return this;
  }

  /**
   * Add attribute to the entity being built.
   *
   * @param attribute the attribute to add
   * @param value the value of the attribute
   * @return this builder for method chaining
   */
  EntityBuilder addAttribute(String attribute, EngineValue value) {
    attributes.put(attribute, value);
    return this;
  }

  /**
   * Build an agent entity.
   *
   * @param parent The entity like Patch that this will be part of.
   * @return A constructed agent entity
   */
  Agent buildAgent(SpatialEntity parent) {
    Agent agent = new Agent(parent, getName(), eventHandlerGroups, attributes);
    return agent;
  }

  /**
   * Build a disturbance entity.
   *
   * @param parent The entity like Patch that this will be part of.
   * @return A constructed disturbance entity
   */
  Disturbance buildDisturbance(SpatialEntity parent) {
    Disturbance disturbance = new Disturbance(parent, getName(), eventHandlerGroups, attributes);
    return disturbance;
  }

  /**
   * Build a patch entity.
   *
   * @param geometry The geometry defining the bounds of this Patch.
   * @return A constructed patch entity
   */
  Patch buildPatch(Geometry geometry) {
    Patch patch = new Patch(geometry, getName(), eventHandlerGroups, attributes);
    return patch;
  }

  /**
   * Build a simulation instance.
   *
   * @param parent The entity like Patch that this will be part of.
   * @return A constructed simulation instance
   */
  Simulation buildSimulation() {
    Simulation simulation = new Simulation(getName(), eventHandlerGroups, attributes);
    return simulation;
  }
}
