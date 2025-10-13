/**
 * Base entity for a mutable entity using a re-entrant lock.
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
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.CompatibleLock;
import org.joshsim.engine.entity.handler.EventHandler;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Represents a base entity that is mutable with a re-entrant lock.
 */
public abstract class DirectLockMutableEntity implements MutableEntity {

  private final String name;
  private final Map<EventKey, EventHandlerGroup> eventHandlerGroups;
  private final CompatibleLock lock;

  private EngineValue[] attributes;
  private EngineValue[] priorAttributes;
  private final Map<String, Integer> attributeNameToIndex;
  private Set<String> onlyOnPrior;

  private Optional<String> substep;
  private Set<String> attributeNames;
  private Map<String, Set<String>> attributesWithoutHandlersBySubstep;
  private Map<String, List<EventHandlerGroup>> commonHandlerCache;

  /**
   * Constructor for Entity.
   *
   * @param name Name of the entity.
   * @param eventHandlerGroups An immutable map of event keys to their corresponding
   *     EventHandlerGroups. This map MUST be immutable (e.g., created via
   *     Collections.unmodifiableMap) as it will be shared across multiple entity
   *     instances for performance.
   * @param attributes An array of EngineValue objects indexed by attributeNameToIndex.
   * @param attributeNameToIndex Shared immutable map from attribute name to array index.
   * @param attributesWithoutHandlersBySubstep Precomputed map of attributes without
   *     handlers per substep.
   * @param commonHandlerCache Precomputed map of all handler lookups, shared across
   *     all instances of this entity type.
   */
  public DirectLockMutableEntity(
      String name,
      Map<EventKey, EventHandlerGroup> eventHandlerGroups,
      EngineValue[] attributes,
      Map<String, Integer> attributeNameToIndex,
      Map<String, Set<String>> attributesWithoutHandlersBySubstep,
      Map<String, List<EventHandlerGroup>> commonHandlerCache
  ) {
    this.name = name;

    // Use the immutable map directly - no defensive copy needed!
    // The map is already immutable and shared across all instances of this entity type.
    if (eventHandlerGroups == null) {
      this.eventHandlerGroups = Collections.emptyMap();
    } else {
      this.eventHandlerGroups = eventHandlerGroups;  // Direct assignment, no copy!
    }

    // Store reference to shared index map (immutable)
    if (attributeNameToIndex == null) {
      this.attributeNameToIndex = Collections.emptyMap();
    } else {
      this.attributeNameToIndex = attributeNameToIndex;
    }

    // Attributes array needs defensive copy (mutable per instance)
    if (attributes == null) {
      this.attributes = new EngineValue[0];
    } else {
      this.attributes = attributes.clone();
    }

    lock = CompatibilityLayerKeeper.get().getLock();
    substep = Optional.empty();
    priorAttributes = new EngineValue[this.attributes.length];
    onlyOnPrior = new HashSet<>();

    attributeNames = computeAttributeNames();
    this.attributesWithoutHandlersBySubstep = attributesWithoutHandlersBySubstep;
    this.commonHandlerCache = commonHandlerCache;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Iterable<EventHandlerGroup> getEventHandlers() {
    return eventHandlerGroups.values();
  }

  @Override
  public Optional<EventHandlerGroup> getEventHandlers(EventKey eventKey) {
    return Optional.ofNullable(eventHandlerGroups.get(eventKey));
  }

  @Override
  public Optional<EngineValue> getAttributeValue(String name) {
    int index = getAttributeIndex(name);
    if (index < 0) {
      return Optional.empty();
    }

    EngineValue value = attributes[index];
    if (value != null) {
      return Optional.of(value);
    }

    value = priorAttributes[index];
    return Optional.ofNullable(value);
  }

  @Override
  public void setAttributeValue(String name, EngineValue value) {
    int index = getAttributeIndex(name);
    if (index < 0) {
      // Unknown attribute - ignore or throw?
      // For now, silently ignore to maintain backward compatibility
      return;
    }

    onlyOnPrior.remove(name);
    attributes[index] = value;
  }

  @Override
  public void lock() {
    lock.lock();
  }

  @Override
  public void unlock() {
    lock.unlock();
  }

  @Override
  public Entity freeze() {
    // Copy values from priorAttributes for attributes only in prior
    for (String key : onlyOnPrior) {
      int index = getAttributeIndex(key);
      if (index >= 0) {
        attributes[index] = priorAttributes[index];
      }
    }

    // Swap arrays
    priorAttributes = attributes;

    // Create new empty attributes array
    attributes = new EngineValue[priorAttributes.length];

    // Reset onlyOnPrior tracking
    onlyOnPrior.clear();
    for (String attrName : attributeNameToIndex.keySet()) {
      int index = getAttributeIndex(attrName);
      if (index >= 0 && priorAttributes[index] != null) {
        onlyOnPrior.add(attrName);
      }
    }

    // Freeze all attribute values
    // Build frozen attributes map for FrozenEntity
    Map<String, EngineValue> frozenAttributes = new HashMap<>(
        (int) (priorAttributes.length / 0.75f) + 1);

    for (Map.Entry<String, Integer> entry : attributeNameToIndex.entrySet()) {
      String attrName = entry.getKey();
      int index = entry.getValue();
      if (index >= 0 && index < priorAttributes.length) {
        EngineValue value = priorAttributes[index];
        if (value != null) {
          frozenAttributes.put(attrName, value.freeze());
        }
      }
    }

    return new FrozenEntity(
        getEntityType(),
        name,
        frozenAttributes,
        getGeometry()
    );
  }

  @Override
  public Set<String> getAttributeNames() {
    return attributeNames;
  }

  @Override
  public Optional<GeoKey> getKey() {
    if (getGeometry().isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(new GeoKey(this));
    }
  }

  @Override
  public void startSubstep(String name) {
    if (substep.isPresent()) {
      String message = String.format(
          "Cannot start %s before %s is completed.",
          substep.get(),
          name
      );
      throw new IllegalStateException(message);
    }

    lock();
    substep = Optional.of(name);
  }

  @Override
  public void endSubstep() {
    substep = Optional.empty();
    unlock();
  }

  public Optional<String> getSubstep() {
    return substep;
  }

  /**
   * Determine unique attribute names.
   *
   * <p>Iterates through all event handler groups and their handlers to collect unique
   * attribute names. Uses direct iteration instead of streams for better performance
   * in this hot path (called during every entity construction).</p>
   *
   * @return Set of unique attribute names.
   */
  private Set<String> computeAttributeNames() {
    Set<String> attributeNames = new HashSet<>();
    for (EventHandlerGroup group : getEventHandlers()) {
      for (EventHandler handler : group.getEventHandlers()) {
        attributeNames.add(handler.getAttributeName());
      }
    }
    return attributeNames;
  }

  /**
   * Check if an attribute has no handlers for a specific substep.
   *
   * <p>This method enables fast-path optimization by identifying when handler lookup
   * can be skipped. If this returns true, the attribute will always resolve from
   * prior state for the given substep.</p>
   *
   * @param attributeName the attribute to check
   * @param substep the substep to check (e.g., "init", "step", "start")
   * @return true if attribute has no handlers for this substep and can use fast-path
   */
  public boolean hasNoHandlers(String attributeName, String substep) {
    Set<String> attrsForSubstep = attributesWithoutHandlersBySubstep.get(substep);
    return attrsForSubstep != null && attrsForSubstep.contains(attributeName);
  }

  /**
   * Get the pre-computed handler cache shared across all instances of this entity type.
   *
   * <p>This cache maps cache key strings (format: "attribute:substep" or
   * "attribute:substep:state") to lists of matching EventHandlerGroups. The cache
   * is computed once during entity type construction and shared across all instances,
   * eliminating the need for per-instance HandlerCacheKey allocations and expensive
   * ConcurrentHashMap lookups.</p>
   *
   * <p>This is a key optimization that reduces both CPU overhead (from HandlerCacheKey.equals()
   * and EventKey.of() calls) and memory overhead (from per-instance handler caches).</p>
   *
   * @return Immutable map from cache key string to list of matching EventHandlerGroups,
   *     or empty map if no handlers are defined
   */
  public Map<String, List<EventHandlerGroup>> getCommonHandlerCache() {
    if (commonHandlerCache == null) {
      return Collections.emptyMap();
    }
    return commonHandlerCache;
  }

  /**
   * Get the array index for an attribute name.
   *
   * @param name the attribute name
   * @return the array index, or -1 if attribute not found
   */
  private int getAttributeIndex(String name) {
    Integer index = attributeNameToIndex.get(name);
    return index != null ? index : -1;
  }

}
