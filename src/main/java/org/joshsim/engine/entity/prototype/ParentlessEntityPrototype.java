/**
 * Structures which aid in building Entities given cached information.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.prototype;

import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.EntityBuilder;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.Geometry;


/**
 * Prototype which can be used to build entities.
 *
 * <p>Prototype which holds onto metadata about an Entity similar to a class definition that can be
 * used to create instances of that Entity given information required for constructing that
 * individual.</p>
 */
public class ParentlessEntityPrototype implements EntityPrototype {

  private final String identifier;
  private final EntityType entityType;
  private final EntityBuilder entityBuilder;

  /**
   * Creates a new EntityPrototype with the specified identifier, type, and builder.
   *
   * @param identifier The unique identifier for this entity prototype.
   * @param entityType The type of entity this prototype can build.
   * @param entityBuilder The builder used to construct instances of this entity.
   */
  public ParentlessEntityPrototype(String identifier, EntityType entityType,
      EntityBuilder entityBuilder) {
    this.identifier = identifier;
    this.entityType = entityType;
    this.entityBuilder = entityBuilder;
  }

  @Override
  public String getIdentifier() {
    return identifier;
  }

  @Override
  public EntityType getEntityType() {
    return entityType;
  }

  @Override
  public MutableEntity build() {
    return switch (entityType) {
      case EXTERNAL_RESOURCE -> throw new RuntimeException("External resources yet implemented.");
      case SIMULATION -> entityBuilder.buildSimulation();
      default -> throw new RuntimeException("Cannot instantiate without a location: " + entityType);
    };
  }

  @Override
  public MutableEntity buildSpatial(Entity parent) {
    return switch (entityType) {
      case AGENT -> entityBuilder.buildAgent(parent);
      case DISTURBANCE -> entityBuilder.buildDisturbance(parent);
      default -> throw new RuntimeException("Cannot instantiate with a parent: " + entityType);
    };
  }

  @Override
  public MutableEntity buildSpatial(Geometry parent) {
    return switch (entityType) {
      case PATCH -> entityBuilder.buildPatch(parent);
      default -> throw new RuntimeException("Cannot instantiate with a geometry: " + entityType);
    };
  }

  @Override
  public boolean requiresParent() {
    return entityType == EntityType.AGENT || entityType == EntityType.DISTURBANCE;
  }

  @Override
  public boolean requiresGeometry() {
    return entityType == EntityType.PATCH;
  }

}
