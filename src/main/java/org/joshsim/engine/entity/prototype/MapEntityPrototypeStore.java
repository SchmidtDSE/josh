/**
 * Structure to manage a collection of EntityPrototypes indexed by name.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.prototype;

import java.util.Map;


public class MapEntityPrototypeStore implements EntityPrototypeStore {

  private final Map<String, EntityPrototype> prototypes;

  public MapEntityPrototypeStore(Map<String, EntityPrototype> prototypes) {
    this.prototypes = prototypes;
  }

  @Override
  public EntityPrototype get(String entityName) {
    if (!prototypes.containsKey(entityName)) {
      throw new IllegalArgumentException(entityName + " is not a known entity.");
    }
    return prototypes.get(entityName);
  }

  @Override
  public boolean has(String entityName) {
    return prototypes.containsKey(entityName);
  }

}
