/**
 * Structure to manage a collection of entity prototypes.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.prototype;


/**
 * A store which contains no entity prototypes.
 */
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
