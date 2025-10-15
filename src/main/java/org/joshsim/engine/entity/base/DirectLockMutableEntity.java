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
  private boolean[] onlyOnPrior;

  private Optional<String> substep;
  private Set<String> attributeNames;
  private Map<String, boolean[]> attributesWithoutHandlersBySubstep;
  private Map<String, List<EventHandlerGroup>> commonHandlerCache;

  /**
   * Constructor for Entity.
   *
   * @param initInfo The initialization information containing all shared entity configuration.
   */
  public DirectLockMutableEntity(EntityInitializationInfo initInfo) {
    this.name = initInfo.getName();

    Map<EventKey, EventHandlerGroup> sharedImmutHandlerGroups = initInfo.getEventHandlerGroups();
    if (sharedImmutEventHandlerGroups == null) {
      this.eventHandlerGroups = Collections.emptyMap();
    } else {
      this.eventHandlerGroups = sharedImmutHandlerGroups;
    }

    Map<String, Integer> sharedImmutAttributeNameToIndex = initInfo.getAttributeNameToIndex();
    if (sharedImmutAttributeNameToIndex == null) {
      this.attributeNameToIndex = Collections.emptyMap();
    } else {
      this.attributeNameToIndex = sharedImmutAttributeNameToIndex;
    }

    String[] sharedImmutIndexToAttributeName = initInfo.getIndexToAttributeName();
    if (sharedImmutIndexToAttributeName == null) {
      this.indexToAttributeName = new String[0];
    } else {
      this.indexToAttributeName = sharedImmutIndexToAttributeName;
    }

    // Attributes array needs defensive copy (mutable per instance)
    EngineValue[] attributes = initInfo.createAttributesArray();
    if (attributes == null) {
      this.attributes = new EngineValue[0];
    } else {
      this.attributes = attributes.clone();
    }

    // Assert that array lengths match for consistency
    assert this.attributes.length == this.indexToAttributeName.length
        : "attributes and indexToAttributeName must have equal length";

    lock = CompatibilityLayerKeeper.get().getLock();
    substep = Optional.empty();
    priorAttributes = new EngineValue[this.attributes.length];
    onlyOnPrior = new boolean[this.attributes.length];

    attributeNames = initInfo.getSharedAttributeNames();
    this.attributesWithoutHandlersBySubstep = initInfo.getAttributesWithoutHandlersBySubstep();
    this.commonHandlerCache = initInfo.getCommonHandlerCache();
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
      String message = String.format(
          "Unknown attribute '%s' for entity %s",
          name,
          this.name
      );
      throw new IllegalArgumentException(message);
    }

    onlyOnPrior[index] = false;
    attributes[index] = value;
  }

  @Override
  public void setAttributeValue(int index, EngineValue value) {
    // Bounds check - throw exception for invalid index
    if (index < 0 || index >= attributes.length) {
      String message = String.format(
          "Attribute index %d out of bounds [0, %d) for entity %s",
          index,
          attributes.length,
          name
      );
      throw new IndexOutOfBoundsException(message);
    }

    // Update attribute at index
    attributes[index] = value;

    // Mark as not only on prior
    onlyOnPrior[index] = false;
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
    for (int i = 0; i < onlyOnPrior.length; i++) {
      if (onlyOnPrior[i]) {
        attributes[i] = priorAttributes[i];
      }
    }

    // Swap arrays
    priorAttributes = attributes;

    // Create new empty attributes array
    attributes = new EngineValue[priorAttributes.length];

    // Reset onlyOnPrior tracking
    for (int i = 0; i < onlyOnPrior.length; i++) {
      onlyOnPrior[i] = priorAttributes[i] != null;
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
   * <p>This method enables fast-path checking by identifying when handler lookup
   * can be skipped. If this returns true, the attribute will always resolve from
   * prior state for the given substep.</p>
   *
   * <p>This method is public to allow external callers (like ShadowingEntity) to
   * perform fast-path checks.</p>
   *
   * @param attributeName the attribute to check
   * @param substep the substep to check (e.g., "init", "step", "start")
   * @return true if attribute has no handlers for this substep
   */
  public boolean hasNoHandlers(String attributeName, String substep) {
    int index = getAttributeIndexInternal(attributeName);
    if (index < 0) {
      return false;
    }
    boolean[] attrsForSubstep = attributesWithoutHandlersBySubstep.get(substep);
    return attrsForSubstep != null && attrsForSubstep[index];
  }

  @Override
  public Map<String, List<EventHandlerGroup>> getResolvedHandlers() {
    if (commonHandlerCache == null) {
      return Collections.emptyMap();
    } else {
      return commonHandlerCache;
    }
  }

  @Override
  public Optional<Integer> getAttributeIndex(String name) {
    Integer index = attributeNameToIndex.get(name);
    if (index != null && index >= 0) {
      return Optional.of(index);
    } else {
      return Optional.empty();
    }
  }

  /**
   * Get the complete attribute name to index mapping for this entity type.
   *
   * <p>This map is shared across all instances of this entity type and maps
   * attribute names to their array indices. The map is immutable and uses
   * alphabetical ordering for deterministic indexing.</p>
   *
   * <p>This method is useful for caching index lookups across multiple
   * attribute accesses on entities of the same type.</p>
   *
   * <p>Note: This method returns a shared immutable reference that is reused
   * across all entity instances of this type for memory efficiency.</p>
   *
   * @return immutable map from attribute name to array index
   */
  @Override
  public Map<String, Integer> getAttributeNameToIndex() {
    return attributeNameToIndex;
  }

  /**
   * Get the index-to-name array for reverse lookup.
   *
   * <p>This array maps attribute indices to their corresponding names,
   * providing efficient reverse lookup from index to attribute name.</p>
   *
   * <p>For entity types that do not support index-based access, this
   * method returns null to avoid allocation overhead.</p>
   *
   * <p>Note: This method returns a shared immutable reference that is reused
   * across all entity instances of this type for memory efficiency.</p>
   *
   * @return immutable array where array[index] = attribute name, or null
   *     if this entity type does not support index-based reverse lookup
   */
  public String[] getIndexToAttributeName() {
    return indexToAttributeName;
  }

  /**
   * Get the array index for an attribute name, returning -1 if not found.
   *
   * <p>This is a private helper method for internal use that returns -1 for
   * missing attributes rather than Optional.empty().</p>
   *
   * @param name the attribute name
   * @return the array index, or -1 if attribute not found
   */
  private int getAttributeIndexInternal(String name) {
    Integer index = attributeNameToIndex.get(name);
    return index != null ? index : -1;
  }

}
