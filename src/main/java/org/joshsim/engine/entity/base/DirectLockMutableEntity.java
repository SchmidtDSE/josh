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

  private Map<String, EngineValue> attributes;
  private Map<String, EngineValue> priorAttributes;
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
   * @param attributes A map of attribute names to their corresponding EngineValues.
   * @param attributesWithoutHandlersBySubstep Precomputed map of attributes without
   *     handlers per substep.
   * @param commonHandlerCache Precomputed map of all handler lookups, shared across
   *     all instances of this entity type.
   */
  public DirectLockMutableEntity(
      String name,
      Map<EventKey, EventHandlerGroup> eventHandlerGroups,
      Map<String, EngineValue> attributes,
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

    // Attributes still need defensive copy as they are mutable per instance
    if (attributes == null) {
      this.attributes = new HashMap<>();
    } else {
      this.attributes = new HashMap<>(attributes);
    }

    lock = CompatibilityLayerKeeper.get().getLock();
    substep = Optional.empty();
    priorAttributes = new HashMap<>();
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
    EngineValue value = attributes.get(name);
    if (value != null) {
      return Optional.of(value);
    }
    return Optional.ofNullable(priorAttributes.get(name));
  }

  @Override
  public void setAttributeValue(String name, EngineValue value) {
    onlyOnPrior.remove(name);
    attributes.put(name, value);
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
    for (String key : onlyOnPrior) {
      attributes.put(key, priorAttributes.get(key));
    }

    priorAttributes = attributes;

    // Pre-size new HashMap based on current size to avoid rehashing
    int expectedSize = (int) (priorAttributes.size() / 0.75f) + 1;
    attributes = new HashMap<>(expectedSize);

    onlyOnPrior = new HashSet<>(priorAttributes.keySet());

    // Freeze all attribute values to ensure nested entities are also frozen
    // Pre-size HashMap to avoid rehashing during construction
    Map<String, EngineValue> frozenAttributes = new HashMap<>(expectedSize);
    for (Map.Entry<String, EngineValue> entry : priorAttributes.entrySet()) {
      frozenAttributes.put(entry.getKey(), entry.getValue().freeze());
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

}
