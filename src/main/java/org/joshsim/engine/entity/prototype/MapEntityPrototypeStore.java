package org.joshsim.engine.entity.prototype;

import java.util.Map;

public class MapEntityPrototypeStore implements EntityPrototypeStore {

  private final Map<String, EntityPrototype> prototypes;

  public MapEntityPrototypeStore(Map<String, EntityPrototype> prototypes) {
    this.prototypes = prototypes;
  }

  public EntityPrototype get(String entityName) {
    if (!prototypes.containsKey(entityName)) {
      throw new IllegalArgumentException(entityName + " is not a known entity.");
    }
    return prototypes.get(entityName);
  }

  public boolean has(String entityName) {
    return prototypes.containsKey(entityName);
  }

}
