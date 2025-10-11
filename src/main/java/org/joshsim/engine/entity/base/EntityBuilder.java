/**
 * Convienence functions to create entities.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.base;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.handler.EventHandler;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.entity.type.Agent;
import org.joshsim.engine.entity.type.Disturbance;
import org.joshsim.engine.entity.type.Patch;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.simulation.Simulation;
import org.joshsim.engine.value.type.EngineValue;

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
  private Map<String, Set<String>> attributesWithoutHandlersBySubstep;
  private Map<EventKey, EventHandlerGroup> immutableEventHandlerGroups;

  /**
   * Create an empty builder.
   */
  public EntityBuilder() {
    name = Optional.empty();
    eventHandlerGroups = new HashMap<>();
    attributes = new HashMap<>();
    attributesWithoutHandlersBySubstep = null; // Computed lazily
    immutableEventHandlerGroups = null; // Computed lazily
  }

  /**
   * Gets or creates the immutable event handler groups map.
   *
   * <p>This method lazily creates an immutable copy of the event handler groups map
   * and caches it for reuse across all entity instances built from this builder.
   * The map is created only once per entity type and shared across all instances,
   * significantly reducing memory allocation during entity construction.</p>
   *
   * <p>The cached map is invalidated (set to null) whenever handlers are added or
   * the builder is cleared, ensuring correctness when the builder is modified.</p>
   *
   * @return An immutable map of event keys to event handler groups, shared across instances
   */
  private Map<EventKey, EventHandlerGroup> getImmutableEventHandlerGroups() {
    if (immutableEventHandlerGroups == null) {
      immutableEventHandlerGroups = Collections.unmodifiableMap(
        new HashMap<>(eventHandlerGroups)
      );
    }
    return immutableEventHandlerGroups;
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
    immutableEventHandlerGroups = null; // Invalidate cache
    attributesWithoutHandlersBySubstep = null; // Invalidate cache
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
    immutableEventHandlerGroups = null; // Invalidate cache
    attributesWithoutHandlersBySubstep = null; // Invalidate cache
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
    attributesWithoutHandlersBySubstep = null; // Invalidate cache
    return this;
  }

  /**
   * Compute the set of attributes that lack handlers for each substep.
   *
   * <p>This method analyzes event handlers to determine which attributes have no handlers
   * for specific substeps (init, step, start, end, constant). This enables fast-path
   * optimization in ShadowingEntity by skipping expensive handler lookups when we know
   * an attribute has no handlers for the current substep.</p>
   *
   * <p>The method is conservative: only marks attribute as "no handlers" if it appears
   * in the initial attributes map but has no event handler for that specific substep.
   * This prevents false negatives (incorrectly skipping handler execution).</p>
   *
   * <p>This computation is done ONCE per entity type in the builder, rather than once
   * per entity instance, providing significant performance improvement.</p>
   *
   * @return Immutable map from substep name to set of attributes without handlers for that substep
   */
  private Map<String, Set<String>> computeAttributesWithoutHandlersBySubstep() {
    // Use cached value if available
    if (attributesWithoutHandlersBySubstep != null) {
      return attributesWithoutHandlersBySubstep;
    }

    Map<String, Set<String>> result = new HashMap<>();

    // Common substeps in Josh DSL
    List<String> substeps = List.of("init", "step", "start", "end", "constant");

    for (String substep : substeps) {
      // Find all attributes WITH handlers for this specific substep
      Set<String> attrsWithHandlersInSubstep = new HashSet<>();

      for (EventHandlerGroup group : eventHandlerGroups.values()) {
        if (group == null) {
          continue;
        }

        EventKey key = group.getEventKey();
        if (key == null) {
          continue;
        }

        // Check if this event handler group is for the current substep
        if (key.getEvent().equals(substep)) {
          // Add all attributes from handlers in this group
          for (EventHandler handler : group.getEventHandlers()) {
            attrsWithHandlersInSubstep.add(handler.getAttributeName());
          }
        }
      }

      // Attributes WITHOUT handlers for this substep =
      // (initial attributes) - (attributes with handlers in this substep)
      Set<String> attrsWithoutHandlers = new HashSet<>(attributes.keySet());
      attrsWithoutHandlers.removeAll(attrsWithHandlersInSubstep);

      result.put(substep, Collections.unmodifiableSet(attrsWithoutHandlers));
    }

    // Cache the result for future calls
    attributesWithoutHandlersBySubstep = Collections.unmodifiableMap(result);
    return attributesWithoutHandlersBySubstep;
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
        getImmutableEventHandlerGroups(),
        createImmutableAttributesCopy(),
        computeAttributesWithoutHandlersBySubstep());
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
        getImmutableEventHandlerGroups(),
        createImmutableAttributesCopy(),
        computeAttributesWithoutHandlersBySubstep());
    return disturbance;
  }

  /**
   * Build a patch entity.
   *
   * @param geometry The geometry defining the bounds of this Patch.
   * @return A constructed patch entity
   */
  public Patch buildPatch(EngineGeometry geometry) {
    Patch patch = new Patch(
        geometry,
        getName(),
        getImmutableEventHandlerGroups(),
        createImmutableAttributesCopy(),
        computeAttributesWithoutHandlersBySubstep());
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
        getImmutableEventHandlerGroups(),
        createImmutableAttributesCopy(),
        computeAttributesWithoutHandlersBySubstep());
    return simulation;
  }

}
