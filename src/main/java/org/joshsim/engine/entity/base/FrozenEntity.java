package org.joshsim.engine.entity.base;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Immutable snapshot of an entity.
 */
public class FrozenEntity implements Entity {

  private final EntityType type;
  private final String name;
  private final Map<String, EngineValue> attributes;
  private final Optional<EngineGeometry> geometry;
  private final Map<String, Integer> attributeNameToIndex;
  private final String[] indexToAttributeName;


  /**
   * Constructs a FrozenEntity with the specified type, name, attributes, and geometry.
   *
   * @param type The type of the entity to be snapshot.
   * @param name The name of the entity to be snapshot.
   * @param attributes A map of attributes related to the entity to be snapshot.
   * @param geometry An optional geometry of the entity to be snapshot.
   * @param attributeNameToIndex The shared index map for this entity type.
   * @param indexToAttributeName The shared index-to-name array for this entity type.
   */
  public FrozenEntity(EntityType type, String name, Map<String, EngineValue> attributes,
      Optional<EngineGeometry> geometry, Map<String, Integer> attributeNameToIndex,
      String[] indexToAttributeName) {
    this.type = type;
    this.name = name;
    this.attributes = attributes;
    this.geometry = geometry;
    this.attributeNameToIndex = attributeNameToIndex != null
        ? attributeNameToIndex
        : Collections.emptyMap();
    this.indexToAttributeName = indexToAttributeName != null
        ? indexToAttributeName
        : new String[0];
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
    return Optional.ofNullable(attributes.get(name));
  }

  @Override
  public Optional<EngineValue> getAttributeValue(int index) {
    // FrozenEntity uses Map storage, so we need to find the name for this index
    // Use O(1) array lookup instead of O(n) HashMap iteration

    // Bounds check
    if (index < 0 || index >= indexToAttributeName.length) {
      return Optional.empty();
    }

    // O(1) array lookup to get attribute name
    String attributeName = indexToAttributeName[index];
    if (attributeName == null) {
      return Optional.empty();
    }

    // Look up value by name
    return Optional.ofNullable(attributes.get(attributeName));
  }

  @Override
  public Set<String> getAttributeNames() {
    return attributes.keySet();
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

  /**
   * Get the index-to-name array for O(1) reverse lookup.
   *
   * @return immutable array where array[index] = attribute name
   */
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
}
