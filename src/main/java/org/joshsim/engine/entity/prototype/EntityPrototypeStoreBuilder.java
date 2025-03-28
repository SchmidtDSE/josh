package org.joshsim.engine.entity.prototype;

import java.util.HashMap;
import java.util.Map;

public class EntityPrototypeStoreBuilder {

  private final Map<String, EntityPrototype> prototypes;

  public EntityPrototypeStoreBuilder() {
    prototypes = new HashMap<>();
  }

  public void add(EntityPrototype prototype) {
    prototypes.put(prototype.getIdentifier(), prototype);
  }

  public EntityPrototype get(String name) {
    if (!prototypes.containsKey(name)) {
      throw new IllegalArgumentException("Unknown entity type: " + name);
    }
    return prototypes.get(name);
  }

  public EntityPrototypeStore build() {
    return new MapEntityPrototypeStore(prototypes);
  }

}
