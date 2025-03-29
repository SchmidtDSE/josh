/**
 * Structure to assist in constructing an EntityPrototypeStore.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.prototype;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder class for constructing an EntityPrototypeStore.
 */
public class EntityPrototypeStoreBuilder {

  private final Map<String, EntityPrototype> prototypes;

  /**
   * Creates a new EntityPrototypeStoreBuilder with an empty prototype collection.
   */
  public EntityPrototypeStoreBuilder() {
    prototypes = new HashMap<>();
  }

  /**
   * Adds an entity prototype to the store being built.
   *
   * @param prototype The EntityPrototype to be added to the store.
   */
  public void add(EntityPrototype prototype) {
    prototypes.put(prototype.getIdentifier(), prototype);
  }

  /**
   * Retrieves an entity prototype by name from the store being built.
   *
   * @param name The identifier of the entity prototype to retrieve.
   * @return The EntityPrototype associated with the given name.
   * @throws IllegalArgumentException if the entity name is not found in the store.
   */
  public EntityPrototype get(String name) {
    if (!prototypes.containsKey(name)) {
      throw new IllegalArgumentException("Unknown entity type: " + name);
    }
    return prototypes.get(name);
  }

  /**
   * Constructs and returns a new EntityPrototypeStore containing all added prototypes.
   *
   * @return A new MapEntityPrototypeStore instance containing all prototypes given to this builder.
   */
  public EntityPrototypeStore build() {
    return new MapEntityPrototypeStore(prototypes);
  }

}
