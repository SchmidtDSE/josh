package org.joshsim.engine.entity.base;

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


  /**
   * Constructs a FrozenEntity with the specified type, name, attributes, and geometry.
   *
   * @param type The type of the entity to be snapshot.
   * @param name The name of the entity to be snapshot.
   * @param attributes A map of attributes related to the entity to be snapshot.
   * @param geometry An optional geometry of the entity to be snapshot.
   */
  public FrozenEntity(EntityType type, String name, Map<String, EngineValue> attributes,
      Optional<EngineGeometry> geometry) {
    this.type = type;
    this.name = name;
    this.attributes = attributes;
    this.geometry = geometry;
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
  public Set<String> getAttributeNames() {
    return attributes.keySet();
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
