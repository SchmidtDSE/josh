package org.joshsim.engine.entity;

import java.util.Map;
import java.util.Optional;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.EngineValue;

public class FrozenEntity implements Entity {

  private final EntityType type;
  private final String name;
  private final Map<String, EngineValue> attributes;
  private final Optional<Geometry> geometry;

  public FrozenEntity(EntityType type, String name, Map<String, EngineValue> attributes,
      Optional<Geometry> geometry) {
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
  public Iterable<String> getAttributeNames() {
    return attributes.keySet();
  }

  private void throwForFrozen() {
    throw new IllegalStateException("Entity already frozen.");
  }

  @Override
  public Optional<Geometry> getGeometry() {
    return geometry;
  }
}
