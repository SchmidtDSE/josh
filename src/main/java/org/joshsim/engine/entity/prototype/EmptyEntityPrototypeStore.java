package org.joshsim.engine.entity.prototype;

public class EmptyEntityPrototypeStore implements EntityPrototypeStore {
  @Override
  public EntityPrototype get(String entityName) {
    throw new IllegalArgumentException(entityName + " is not a known entity.");
  }

  @Override
  public boolean has(String entityName) {
    return false;
  }
}
