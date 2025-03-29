/**
 * Structures which aid in building Entities given cached information.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.prototype;

import org.joshsim.engine.entity.Entity;
import org.joshsim.engine.entity.EntityBuilder;
import org.joshsim.engine.entity.EntityType;
import org.joshsim.engine.geometry.Geometry;


/**
 * Prototype which can be used to build entities.
 *
 * <p>Prototype which holds onto metadata about an Entity similar to a class definition that can be
 * used to create instances of that Entity given information required for constructing that
 * individual.</p>
 */
public class EntityPrototype {

  private final String identifier;
  private final EntityType entityType;
  private final EntityBuilder entityBuilder;

  /**
   * Creates a new EntityPrototype with the specified identifier, type, and builder.
   *
   * @param identifier The unique identifier for this entity prototype
   * @param entityType The type of entity this prototype represents
   * @param entityBuilder The builder used to construct instances of this entity
   */
  public EntityPrototype(String identifier, EntityType entityType, EntityBuilder entityBuilder) {
    this.identifier = identifier;
    this.entityType = entityType;
    this.entityBuilder = entityBuilder;
  }

  /**
   * Gets the unique identifier of this entity prototype.
   *
   * @return The identifier string
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * Gets the type of entity this prototype represents.
   *
   * @return The EntityType enum value
   */
  public EntityType getEntityType() {
    return entityType;
  }

  /**
   * Builds a non-spatial entity instance from this prototype.
   * Only valid for SIMULATION type entities.
   *
   * @return A new Entity instance
   * @throws RuntimeException if the entity type cannot be built without spatial context
   */
  public Entity build() {
    return switch (entityType) {
      case EXTERNAL_RESOURCE -> throw new RuntimeException("External resources yet implemented.");
      case SIMULATION -> entityBuilder.buildSimulation();
      default -> throw new RuntimeException("Cannot instantiate without a location: " + entityType);
    };
  }

  /**
   * Builds a spatial entity instance from this prototype with a parent entity.
   * Valid for AGENT and DISTURBANCE type entities.
   *
   * @param parent The parent Entity that provides spatial context
   * @return A new Entity instance
   * @throws RuntimeException if the entity type cannot be built with a parent entity
   */
  public Entity buildSpatial(Entity parent) {
    return switch (entityType) {
      case AGENT -> entityBuilder.buildAgent(parent);
      case DISTURBANCE -> entityBuilder.buildDisturbance(parent);
      default -> throw new RuntimeException("Cannot instantiate with a parent: " + entityType);
    };
  }

  /**
   * Builds a spatial entity instance from this prototype with a geometry parent.
   * Valid only for PATCH type entities.
   *
   * @param parent The parent Geometry that provides spatial context
   * @return A new Entity instance
   * @throws RuntimeException if the entity type cannot be built with a geometry parent
   */
  public Entity buildSpatial(Geometry parent) {
    return switch (entityType) {
      case PATCH -> entityBuilder.buildPatch(parent);
      default -> throw new RuntimeException("Cannot instantiate with a geometry: " + entityType);
    };
  }

}
