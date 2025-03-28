package org.joshsim.lang.interpret.machine;

import org.joshsim.engine.entity.Entity;
import org.joshsim.engine.entity.EntityBuilder;
import org.joshsim.engine.entity.EntityType;
import org.joshsim.engine.entity.SpatialEntity;
import org.joshsim.engine.geometry.Geometry;


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

  Entity build() {
    return switch (entityType) {
      case EXTERNAL_RESOURCE -> throw new RuntimeException("External resources yet implemented.");
      case SIMULATION -> entityBuilder.buildSimulation();
      default -> throw new RuntimeException("Cannot instantiate the following without a location: " + entityType);
    };
  }

  SpatialEntity buildSpatial(SpatialEntity parent) {
    return switch (entityType) {
      case AGENT -> entityBuilder.buildAgent(parent);
      case DISTURBANCE -> entityBuilder.buildDisturbance(parent);
      default -> throw new RuntimeException("Cannot instantiate the following with a parent: " + entityType);
    };
  }

  SpatialEntity buildSpatial(Geometry parent) {
    return switch (entityType) {
      case PATCH -> entityBuilder.buildPatch(parent);
      default -> throw new RuntimeException("Cannot instantiate the following with a geometry: " + entityType);
    };
  }

}
