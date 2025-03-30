/**
 * Convienence functions to create entities.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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
  private Optional<String> name;
  private Map<EventKey, EventHandlerGroup> eventHandlerGroups;
  private Map<String, EngineValue> attributes;

  /**
   * Create an empty builder.
   */
  public EntityBuilder() {
    name = Optional.empty();
    eventHandlerGroups = new HashMap<>();
    attributes = new HashMap<>();
  }

  /**
   * Creates an immutable copy of the event handler groups map.
   *
   * @return an immutable copy of the event handler groups
   */
  private Map<EventKey, EventHandlerGroup> createImmutableEventHandlerGroupsCopy() {
    Map<EventKey, EventHandlerGroup> copy = new HashMap<>(eventHandlerGroups);
    return Collections.unmodifiableMap(copy);
  }

  /**
   * Creates an immutable copy of the attributes map.
   *
   * @return an immutable copy of the attributes
   */
  private Map<String, EngineValue> createImmutableAttributesCopy() {
    Map<String, EngineValue> copy = new HashMap<>(attributes);
    return Collections.unmodifiableMap(copy);
  }

  /**
   * Set the name of the entity being built.
   *
   * @param name the name of the entity
   * @return this builder for method chaining
   */
  public EntityBuilder setName(String name) {
    this.name = Optional.of(name);
    return this;
  }

  /**
   * Get the name of the entity being built.
   *
   * @return the name of the entity
   */
  private String getName() {
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
  public EntityBuilder addEventHandlerGroup(EventKey eventKey, EventHandlerGroup group) {
    eventHandlerGroups.put(eventKey, group);
    return this;
  }

  /**
   * Add attribute to the entity being built.
   *
   * @param attribute the attribute to add
   * @param value the value of the attribute
   * @return this builder for method chaining
   */
  public EntityBuilder addAttribute(String attribute, EngineValue value) {
    attributes.put(attribute, value);
    return this;
  }

  /**
   * Build an agent entity.
   *
   * @param parent The entity like Patch that this will be part of.
   * @return A constructed agent entity
   */
  public Agent buildAgent(Entity parent) {
    Agent agent = new Agent(
        parent,
        getName(),
        createImmutableEventHandlerGroupsCopy(),
        createImmutableAttributesCopy());
    return agent;
  }

  /**
   * Build a disturbance entity.
   *
   * @param parent The entity like Patch that this will be part of.
   * @return A constructed disturbance entity
   */
  public Disturbance buildDisturbance(Entity parent) {
    Disturbance disturbance = new Disturbance(
        parent,
        getName(),
        createImmutableEventHandlerGroupsCopy(),
        createImmutableAttributesCopy());
    return disturbance;
  }

  /**
   * Build a patch entity.
   *
   * @param geometry The geometry defining the bounds of this Patch.
   * @return A constructed patch entity
   */
  public Patch buildPatch(Geometry geometry) {
    Patch patch = new Patch(
        geometry,
        getName(),
        createImmutableEventHandlerGroupsCopy(),
        createImmutableAttributesCopy());
    return patch;
  }

  /**
   * Build a simulation instance.
   *
   * @return A constructed simulation instance
   */
  public Simulation buildSimulation() {
    Simulation simulation = new Simulation(
        getName(),
        createImmutableEventHandlerGroupsCopy(),
        createImmutableAttributesCopy());
    return simulation;
  }

}
