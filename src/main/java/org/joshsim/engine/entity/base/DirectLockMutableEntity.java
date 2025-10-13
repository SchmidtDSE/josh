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
  private final String[] indexToAttributeName;
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
   * @param indexToAttributeName Shared immutable array from index to attribute name.
   * @param attributesWithoutHandlersBySubstep Precomputed map of attributes without
   *     handlers per substep.
   * @param commonHandlerCache Precomputed map of all handler lookups, shared across
   *     all instances of this entity type.
   * @param sharedAttributeNames Precomputed immutable set of attribute names, shared
   *     across all instances of this entity type. Eliminates per-instance HashSet
   *     allocation and handler iteration.
   */
  public DirectLockMutableEntity(
      String name,
      Map<EventKey, EventHandlerGroup> eventHandlerGroups,
      EngineValue[] attributes,
      Map<String, Integer> attributeNameToIndex,
      String[] indexToAttributeName,
      Map<String, Set<String>> attributesWithoutHandlersBySubstep,
      Map<String, List<EventHandlerGroup>> commonHandlerCache,
      Set<String> sharedAttributeNames
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

    // Store reference to shared index-to-name array (immutable)
    if (indexToAttributeName == null) {
      this.indexToAttributeName = new String[0];
    } else {
      this.indexToAttributeName = indexToAttributeName;
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

    attributeNames = sharedAttributeNames;
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
    int index = getAttributeIndexInternal(name);
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
  public Optional<EngineValue> getAttributeValue(int index) {
    // Bounds check
    if (index < 0 || index >= attributes.length) {
      return Optional.empty();
    }

    // Check current attributes first
    EngineValue value = attributes[index];
    if (value != null) {
      return Optional.of(value);
    }

    // Check prior attributes
    value = priorAttributes[index];
    return Optional.ofNullable(value);
  }

  @Override
  public void setAttributeValue(String name, EngineValue value) {
    int index = getAttributeIndexInternal(name);
    if (index < 0) {
      // Unknown attribute - ignore or throw?
      // For now, silently ignore to maintain backward compatibility
      return;
    }

    onlyOnPrior.remove(name);
    attributes[index] = value;
  }

  @Override
  public void setAttributeValue(int index, EngineValue value) {
    // Bounds check - throw exception for invalid index
    if (index < 0 || index >= attributes.length) {
      String message = String.format(
          "Attribute index %d out of bounds [0, %d) for entity %s",
          index, attributes.length, name);
      throw new IndexOutOfBoundsException(message);
    }

    // Update attribute at index
    attributes[index] = value;

    // Remove from onlyOnPrior set if present - O(1) array lookup
    if (index < indexToAttributeName.length) {
      String attributeName = indexToAttributeName[index];
      if (attributeName != null) {
        onlyOnPrior.remove(attributeName);
      }
    }
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
      int index = getAttributeIndexInternal(key);
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
      int index = getAttributeIndexInternal(attrName);
      if (index >= 0 && priorAttributes[index] != null) {
        onlyOnPrior.add(attrName);
      }
    }

    // Freeze all attribute values into array
    // Create frozen attribute values array for FrozenEntity
    EngineValue[] frozenAttributeValues = new EngineValue[priorAttributes.length];

    for (int i = 0; i < priorAttributes.length; i++) {
      EngineValue value = priorAttributes[i];
      if (value != null) {
        frozenAttributeValues[i] = value.freeze();
      }
      // else: leave null in array (uninitialized attribute)
    }

    return new FrozenEntity(
        getEntityType(),
        name,
        frozenAttributeValues,
        getGeometry(),
        attributeNameToIndex,
        indexToAttributeName,
        attributeNames
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

  @Override
  public Optional<Integer> getAttributeIndex(String name) {
    Integer index = attributeNameToIndex.get(name);
    if (index != null && index >= 0) {
      return Optional.of(index);
    }
    return Optional.empty();
  }

  @Override
  public Map<String, Integer> getAttributeNameToIndex() {
    // Return the shared immutable map directly
    return attributeNameToIndex;
  }

  /**
   * Get the index-to-name array for O(1) reverse lookup.
   *
   * <p>This array maps attribute indices to their corresponding names,
   * enabling O(1) reverse lookup without HashMap iteration.</p>
   *
   * @return immutable array where array[index] = attribute name
   */
  public String[] getIndexToAttributeName() {
    // Return the shared immutable array directly
    return indexToAttributeName;
  }

  /**
   * Get the array index for an attribute name, returning -1 if not found.
   *
   * <p>This is a private helper method for internal use that returns -1 for
   * missing attributes rather than Optional.empty(). Prefer this for internal
   * code paths to avoid Optional allocation overhead.</p>
   *
   * @param name the attribute name
   * @return the array index, or -1 if attribute not found
   */
  private int getAttributeIndexInternal(String name) {
    Integer index = attributeNameToIndex.get(name);
    return index != null ? index : -1;
  }

}
