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


  /**
   * Constructs a FrozenEntity with the specified type, name, attributes, and geometry.
   *
   * @param type The type of the entity to be snapshot.
   * @param name The name of the entity to be snapshot.
   * @param attributes A map of attributes related to the entity to be snapshot.
   * @param geometry An optional geometry of the entity to be snapshot.
   * @param attributeNameToIndex The shared index map for this entity type.
   */
  public FrozenEntity(EntityType type, String name, Map<String, EngineValue> attributes,
      Optional<EngineGeometry> geometry, Map<String, Integer> attributeNameToIndex) {
    this.type = type;
    this.name = name;
    this.attributes = attributes;
    this.geometry = geometry;
    this.attributeNameToIndex = attributeNameToIndex != null
        ? attributeNameToIndex
        : Collections.emptyMap();
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
    // This is less efficient than array access, but FrozenEntity is not in hot path

    // Bounds check
    if (index < 0 || index >= attributeNameToIndex.size()) {
      return Optional.empty();
    }

    // Find the attribute name with this index
    for (Map.Entry<String, Integer> entry : attributeNameToIndex.entrySet()) {
      if (entry.getValue() == index) {
        return Optional.ofNullable(attributes.get(entry.getKey()));
      }
    }

    return Optional.empty();
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
