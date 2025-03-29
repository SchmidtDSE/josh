
package org.joshsim.engine.entity.prototype;


/**
 * Store for managing entity prototypes that can be used to create new entities.
 * Provides methods to access and check existence of entity prototypes by name.
 */
public interface EntityPrototypeStore {

  /**
   * Retrieves an entity prototype by its name.
   *
   * @param entityName the identifier of the entity prototype to retrieve
   * @return the EntityPrototype associated with the given name
   * @throws IllegalArgumentException if the entity name is not found in the store
   */
  EntityPrototype get(String entityName);

  /**
   * Checks if an entity prototype exists in the store.
   *
   * @param entityName the identifier of the entity prototype to check
   * @return true if the entity prototype exists, false otherwise
   */
  boolean has(String entityName);

}
