/**
 * Convienence functions to create entities.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.base;

import java.util.ArrayList;
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
  private Map<String, List<EventHandlerGroup>> commonHandlerCache;
  private Map<String, Integer> attributeNameToIndex;
  private String[] indexToAttributeName;
  private Set<String> sharedAttributeNames;

  /**
   * Create an empty builder.
   */
  public EntityBuilder() {
    name = Optional.empty();
    eventHandlerGroups = new HashMap<>();
    attributes = new HashMap<>();
    attributesWithoutHandlersBySubstep = null; // Computed lazily
    immutableEventHandlerGroups = null; // Computed lazily
    commonHandlerCache = null; // Computed lazily
    sharedAttributeNames = null; // Computed lazily
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
    commonHandlerCache = null; // Invalidate cache
    attributeNameToIndex = null; // Invalidate cache
    indexToAttributeName = null; // Invalidate cache
    sharedAttributeNames = null; // Invalidate cache
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
    commonHandlerCache = null; // Invalidate cache
    sharedAttributeNames = null; // Invalidate cache
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
    attributeNameToIndex = null; // Invalidate cache
    indexToAttributeName = null; // Invalidate cache
    return this;
  }

  /**
   * Compute the set of attributes that lack handlers for each substep.
   *
   * <p>This method analyzes event handlers to determine which attributes have no handlers
   * for specific substeps (init, step, start, end, constant). This enables fast-path
   * checking in ShadowingEntity by skipping handler lookups when we know an attribute
   * has no handlers for the current substep.</p>
   *
   * <p>The method is conservative: only marks attribute as "no handlers" if it appears
   * in the initial attributes map but has no event handler for that specific substep.
   * This prevents false negatives (incorrectly skipping handler execution).</p>
   *
   * <p>This computation is done once per entity type in the builder and shared across
   * all entity instances of that type.</p>
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
   * Compute the pre-computed handler cache for all attributes, substeps, and states.
   *
   * <p>This method pre-computes all possible handler lookups by examining all event keys
   * in the entity's event handler groups and creating a cache keyed by
   * "attribute:substep" or "attribute:substep:state" strings.</p>
   *
   * <p>The computation is done once per entity type in the builder and shared
   * across all entity instances of that type.</p>
   *
   * @return Immutable map from cache key string to list of matching EventHandlerGroups
   */
  private Map<String, List<EventHandlerGroup>> computeCommonHandlerCache() {
    // Use cached value if available
    if (commonHandlerCache != null) {
      return commonHandlerCache;
    }

    // Collect all unique attribute names from handlers
    Set<String> allAttributes = new HashSet<>();
    for (EventHandlerGroup group : eventHandlerGroups.values()) {
      if (group == null) {
        continue;
      }
      for (EventHandler handler : group.getEventHandlers()) {
        allAttributes.add(handler.getAttributeName());
      }
    }

    // Collect all unique states from EventKeys
    Set<String> allStates = new HashSet<>();
    allStates.add(""); // Empty state for non-state-specific lookups

    for (EventKey key : eventHandlerGroups.keySet()) {
      if (key == null) {
        continue;
      }
      String state = key.getState();
      if (state != null && !state.isEmpty()) {
        allStates.add(state);
      }
    }

    // Common substeps in Josh DSL
    List<String> substeps = List.of("init", "step", "start", "end", "constant");

    // Pre-compute all (attribute × substep × state) combinations
    Map<String, List<EventHandlerGroup>> result = new HashMap<>();
    for (String attribute : allAttributes) {
      for (String substep : substeps) {
        for (String state : allStates) {
          // Build cache key
          String cacheKey;
          if (state.isEmpty()) {
            cacheKey = attribute + ":" + substep;
          } else {
            cacheKey = attribute + ":" + substep + ":" + state;
          }

          // Find matching handler groups
          List<EventHandlerGroup> matching = new ArrayList<>();

          // Check for handler without state
          EventKey keyWithoutState = EventKey.of(attribute, substep);
          EventHandlerGroup groupWithoutState = eventHandlerGroups.get(keyWithoutState);
          if (groupWithoutState != null) {
            matching.add(groupWithoutState);
          }

          // Check for handler with state (if state is not empty)
          if (!state.isEmpty()) {
            EventKey keyWithState = EventKey.of(state, attribute, substep);
            EventHandlerGroup groupWithState = eventHandlerGroups.get(keyWithState);
            if (groupWithState != null) {
              matching.add(groupWithState);
            }
          }

          // Only cache if we found matching handlers
          if (!matching.isEmpty()) {
            result.put(cacheKey, Collections.unmodifiableList(matching));
          }
        }
      }
    }

    // Cache the result for future calls
    commonHandlerCache = Collections.unmodifiableMap(result);
    return commonHandlerCache;
  }

  /**
   * Compute the shared set of attribute names for this entity type.
   *
   * <p>This method extracts all unique attribute names from event handlers defined
   * for this entity type. The computation is done once per entity type in the builder
   * and the resulting immutable set is shared across all entity instances of that type.</p>
   *
   * <p>The returned set is immutable and thread-safe for concurrent reads, making
   * it safe to share across entity instances that may be accessed in parallel
   * contexts during simulation execution.</p>
   *
   * @return Immutable set of attribute names, shared across all instances of this entity type
   */
  private Set<String> computeAttributeNames() {
    // Use cached value if available
    if (sharedAttributeNames != null) {
      return sharedAttributeNames;
    }

    // Collect all unique attribute names from handlers
    Set<String> attributeNames = new HashSet<>();
    for (EventHandlerGroup group : eventHandlerGroups.values()) {
      if (group == null) {
        continue;
      }
      for (EventHandler handler : group.getEventHandlers()) {
        attributeNames.add(handler.getAttributeName());
      }
    }

    // Cache immutable set for reuse
    sharedAttributeNames = Collections.unmodifiableSet(attributeNames);
    return sharedAttributeNames;
  }

  /**
   * Compute the shared attribute name to index map for array-based storage.
   *
   * <p>This method creates a sorted mapping from attribute names to array indices,
   * ensuring deterministic ordering across all entity instances. Attributes are
   * sorted alphabetically to guarantee consistent indices.</p>
   *
   * <p>This map is computed once per entity type in the builder and shared across
   * all entity instances of that type.</p>
   *
   * @return Immutable map from attribute name to array index
   */
  private Map<String, Integer> computeAttributeNameToIndex() {
    // Use cached value if available
    if (attributeNameToIndex != null) {
      return attributeNameToIndex;
    }

    // Collect all unique attribute names
    Set<String> allAttributeNames = new HashSet<>(attributes.keySet());

    // Also collect attributes from event handlers
    for (EventHandlerGroup group : eventHandlerGroups.values()) {
      if (group == null) {
        continue;
      }
      for (EventHandler handler : group.getEventHandlers()) {
        allAttributeNames.add(handler.getAttributeName());
      }
    }

    // Sort alphabetically for deterministic ordering
    List<String> sortedNames = new ArrayList<>(allAttributeNames);
    Collections.sort(sortedNames);

    // Build index map
    Map<String, Integer> result = new HashMap<>();
    for (int i = 0; i < sortedNames.size(); i++) {
      result.put(sortedNames.get(i), i);
    }

    // Cache immutable map
    attributeNameToIndex = Collections.unmodifiableMap(result);
    return attributeNameToIndex;
  }

  /**
   * Compute the shared index-to-name array for reverse lookup.
   *
   * <p>This method creates an array where indexToAttributeName[i] = name for the
   * attribute at index i. This enables direct lookup when converting from index to name.</p>
   *
   * <p>The array is computed once per entity type in the builder and shared across
   * all entity instances of that type.</p>
   *
   * @return Array where array[index] = attribute name for that index
   */
  private String[] computeIndexToAttributeName() {
    // Use cached value if available
    if (indexToAttributeName != null) {
      return indexToAttributeName;
    }

    // Get the index map (creates it if needed)
    Map<String, Integer> indexMap = computeAttributeNameToIndex();

    // Create array sized to hold all attributes
    String[] result = new String[indexMap.size()];

    // Populate array: for each (name, index) pair, set result[index] = name
    for (Map.Entry<String, Integer> entry : indexMap.entrySet()) {
      String name = entry.getKey();
      int index = entry.getValue();
      result[index] = name;
    }

    // Cache immutable array
    indexToAttributeName = result;
    return indexToAttributeName;
  }

  /**
   * Convert attributes map to array using the computed index map.
   *
   * <p>This creates an EngineValue array where each attribute is placed at
   * the index specified by attributeNameToIndex. Attributes not in the
   * initial attributes map are left as null in the array.</p>
   *
   * @return Array of EngineValue objects indexed by attributeNameToIndex
   */
  private EngineValue[] createAttributesArray() {
    Map<String, Integer> indexMap = computeAttributeNameToIndex();
    EngineValue[] result = new EngineValue[indexMap.size()];

    // Copy values from map to array
    for (Map.Entry<String, EngineValue> entry : attributes.entrySet()) {
      Integer index = indexMap.get(entry.getKey());
      if (index != null) {
        result[index] = entry.getValue();
      }
    }

    return result;
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
        createAttributesArray(),
        computeAttributeNameToIndex(),
        computeIndexToAttributeName(),
        computeAttributesWithoutHandlersBySubstep(),
        computeCommonHandlerCache(),
        computeAttributeNames());
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
        createAttributesArray(),
        computeAttributeNameToIndex(),
        computeIndexToAttributeName(),
        computeAttributesWithoutHandlersBySubstep(),
        computeCommonHandlerCache(),
        computeAttributeNames());
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
        createAttributesArray(),
        computeAttributeNameToIndex(),
        computeIndexToAttributeName(),
        computeAttributesWithoutHandlersBySubstep(),
        computeCommonHandlerCache(),
        computeAttributeNames());
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
        createAttributesArray(),
        computeAttributeNameToIndex(),
        computeIndexToAttributeName(),
        computeAttributesWithoutHandlersBySubstep(),
        computeCommonHandlerCache(),
        computeAttributeNames());
    return simulation;
  }

}
