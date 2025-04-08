/**
 * Description of structures which aid in building Entities given cached information.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.prototype;

import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.EngineGeometry;


/**
 * Prototype which can be used to build entities.
 *
 * <p>Prototype which holds onto metadata about an Entity similar to a class definition that can be
 * used to create instances of that Entity given information required for constructing that
 * individual.</p>
 */
public interface EntityPrototype {

  /**
   * Gets the unique identifier of this entity prototype.
   *
   * @return The identifier string with which entities will be built from this prototype.
   */
  String getIdentifier();

  /**
   * Gets the type of entity this prototype will build.
   *
   * @return The EntityType enum value with which entities will be built from this prototype.
   */
  EntityType getEntityType();

  /**
   * Builds a non-spatial entity instance from this prototype.
   * Only valid for SIMULATION type entities.
   *
   * @return A new Entity instance created from this prototype.
   * @throws RuntimeException if the entity type cannot be built without spatial context.
   */
  Entity build();

  /**
   * Builds a spatial entity instance from this prototype with a parent entity.
   * Valid for AGENT and DISTURBANCE type entities.
   *
   * @param parent The parent Entity that houses this entity.
   * @return A new Entity instance created from this prototype.
   * @throws RuntimeException if the entity type cannot be built with a parent entity.
   */
  Entity buildSpatial(Entity parent);

  /**
   * Builds a spatial entity instance from this prototype with a geometry parent.
   * Valid only for PATCH type entities.
   *
   * @param parent The parent EngineGeometry that houses this entity.
   * @return A new Entity instance created from this prototype.
   * @throws RuntimeException if the entity type cannot be built with a geometry parent.
   */
  Entity buildSpatial(EngineGeometry parent);

  /**
   * Determine if this entity prototype requires a parent to be provided to be constructed.
   *
   * @return True if a parent is required and false otherwise.
   */
  boolean requiresParent();

  /**
   * Determine if this entity prototype requires a geometry to be provided to be constructed.
   *
   * @return True if a geometry is required and false otherwise.
   */
  boolean requiresGeometry();

}
