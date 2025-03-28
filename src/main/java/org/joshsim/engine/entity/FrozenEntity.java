package org.joshsim.engine.entity;

import org.joshsim.engine.value.EngineValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FrozenEntity implements Entity {

  private final EntityType entityType;
  private final String name;
  private final Map<String, EngineValue> attributes;

  public FrozenEntity(EntityType entityType, String name, Map<String, EngineValue> attributes) {
    this.entityType = entityType;
    this.name = name;
    this.attributes = attributes;
  }

  @Override
  public EntityType getEntityType() {
    return null;
  }

  @Override
  public Entity freeze() {
    throw new IllegalStateException("Entity already frozen.");
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Iterable<EventHandlerGroup> getEventHandlers() {
    throwForFrozen();
    return null;
  }

  @Override
  public Optional<EventHandlerGroup> getEventHandlers(EventKey event) {
    return Optional.empty();
  }

  @Override
  public Optional<EngineValue> getAttributeValue(String name) {
    return Optional.ofNullable(attributes.get(name));
  }

  @Override
  public void setAttributeValue(String name, EngineValue value) {
    throwForFrozen();
  }

  @Override
  public void lock() {
    throwForFrozen();
  }

  @Override
  public void unlock() {
    throwForFrozen();
  }

  private void throwForFrozen() {
    throw new IllegalStateException("Entity already frozen.");
  }

  @Override
  public boolean isFrozen() {
    return true;
  }
}
