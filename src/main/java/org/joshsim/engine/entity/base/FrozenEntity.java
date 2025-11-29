package org.joshsim.engine.entity.base;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Immutable snapshot of an entity.
 */
public class FrozenEntity implements Entity {

  /**
   * Cached empty Optional to avoid repeated Optional.empty() allocations.
   *
   * <p>This singleton is reused across all FrozenEntity instances.</p>
   */
  private static final Optional<EngineValue> EMPTY_ATTRIBUTE_VALUE = Optional.empty();

  /**
   * Cached empty map to avoid repeated Collections.emptyMap() allocations.
   *
   * <p>FrozenEntity instances have no handlers, so we reuse this singleton.</p>
   */
  private static final Map<String, List<EventHandlerGroup>> EMPTY_HANDLERS = Collections.emptyMap();

  private final EntityType type;
  private final String name;
  private final EngineValue[] attributeValues;
  private final Optional<EngineGeometry> geometry;
  private final Map<String, Integer> attributeNameToIndex;
  private final String[] indexToAttributeName;
  private final Set<String> attributeNames;
  private final boolean usesState;
  private final int stateIndex;


  /**
   * Constructs a FrozenEntity with the specified type, name, attributes, and geometry.
   *
   * @param type The type of the entity to be snapshot.
   * @param name The name of the entity to be snapshot.
   * @param attributeValues Array of attribute values indexed by attributeNameToIndex.
   * @param geometry An optional geometry of the entity to be snapshot.
   * @param attributeNameToIndex The shared index map for this entity type.
   * @param indexToAttributeName The shared index-to-name array for this entity type.
   * @param sharedAttributeNames The shared set of all defined attribute names for this entity type.
   * @param usesState Whether this entity type has a state attribute.
   * @param stateIndex The index of the state attribute, or -1 if not used.
   */
  public FrozenEntity(EntityType type, String name, EngineValue[] attributeValues,
      Optional<EngineGeometry> geometry, Map<String, Integer> attributeNameToIndex,
      String[] indexToAttributeName, Set<String> sharedAttributeNames,
      boolean usesState, int stateIndex) {
    this.type = type;
    this.name = name;
    this.attributeValues = attributeValues;
    this.geometry = geometry;
    this.attributeNameToIndex = attributeNameToIndex;
    this.indexToAttributeName = indexToAttributeName;
    this.attributeNames = sharedAttributeNames;
    this.usesState = usesState;
    this.stateIndex = stateIndex;
  }

  @Override
  public EntityType getEntityType() {
    return type;
  }

  @Override
  public Entity freeze() {
    return this;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Optional<EngineValue> getAttributeValue(String name) {
    // Look up index for this attribute name
    Integer index = attributeNameToIndex.get(name);
    if (index == null || index < 0 || index >= attributeValues.length) {
      return EMPTY_ATTRIBUTE_VALUE;
    }

    // Use index to access array
    EngineValue value = attributeValues[index];
    return value == null ? EMPTY_ATTRIBUTE_VALUE : Optional.of(value);
  }

  @Override
  public Optional<EngineValue> getAttributeValue(int index) {
    // Direct array access using index
    if (index < 0 || index >= attributeValues.length) {
      return EMPTY_ATTRIBUTE_VALUE;
    }

    // Explicit null check to reuse EMPTY_ATTRIBUTE_VALUE
    EngineValue value = attributeValues[index];
    return value == null ? EMPTY_ATTRIBUTE_VALUE : Optional.of(value);
  }

  @Override
  public Set<String> getAttributeNames() {
    return attributeNames;
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
    return attributeNameToIndex;
  }

  @Override
  public String[] getIndexToAttributeName() {
    return indexToAttributeName;
  }

  private void throwForFrozen() {
    throw new IllegalStateException("Entity already frozen.");
  }

  @Override
  public Optional<EngineGeometry> getGeometry() {
    return geometry;
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
  public Map<String, List<EventHandlerGroup>> getResolvedHandlers() {
    return EMPTY_HANDLERS;
  }

  @Override
  public boolean usesState() {
    return usesState;
  }

  @Override
  public int getStateIndex() {
    return stateIndex;
  }
}
