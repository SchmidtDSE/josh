/**
 * Structure to manage a collection of entity prototypes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.prototype;


/**
 * Store for managing entity prototypes that can be used to create new entities.
 */
public interface EntityPrototypeStore {

  /**
   * Retrieve an entity prototype by its name.
   *
   * @param entityName the identifier of the entity prototype to retrieve
   * @return the EntityPrototype associated with the given name
   * @throws IllegalArgumentException if the entity name is not found in the store
   */
  EntityPrototype get(String entityName);

  /**
   * Check if an entity prototype exists in the store.
   *
   * @param entityName the identifier of the entity prototype to check
   * @return true if the entity prototype exists, false otherwise
   */
  boolean has(String entityName);

  /**
   * Get all prorotypes in this store.
   *
   * @return Iterable over all registered prototypes within this EntityPrototypeStore.
   */
  Iterable<EntityPrototype> getAll();

}
