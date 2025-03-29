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

  public EntityPrototype(String identifier, EntityType entityType, EntityBuilder entityBuilder) {
    this.identifier = identifier;
    this.entityType = entityType;
    this.entityBuilder = entityBuilder;
  }

  public String getIdentifier() {
    return identifier;
  }

  public EntityType getEntityType() {
    return entityType;
  }

  public Entity build() {
    return switch (entityType) {
      case EXTERNAL_RESOURCE -> throw new RuntimeException("External resources yet implemented.");
      case SIMULATION -> entityBuilder.buildSimulation();
      default -> throw new RuntimeException("Cannot instantiate without a location: " + entityType);
    };
  }

  public Entity buildSpatial(Entity parent) {
    return switch (entityType) {
      case AGENT -> entityBuilder.buildAgent(parent);
      case DISTURBANCE -> entityBuilder.buildDisturbance(parent);
      default -> throw new RuntimeException("Cannot instantiate with a parent: " + entityType);
    };
  }

  public Entity buildSpatial(Geometry parent) {
    return switch (entityType) {
      case PATCH -> entityBuilder.buildPatch(parent);
      default -> throw new RuntimeException("Cannot instantiate with a geometry: " + entityType);
    };
  }

}
