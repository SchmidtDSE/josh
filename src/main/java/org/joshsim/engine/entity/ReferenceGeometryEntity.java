package org.joshsim.engine.entity;

import java.util.HashMap;
import java.util.Map;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.EngineValue;

/**
 * Geometric entity that serves as a reference to a generic geometry.
 */
public class ReferenceGeometryEntity extends RootSpatialEntity {

  /**
   * Constructs a RootSpatialEntity with the specified geometry.
   *
   * @param geometry The geometry associated with this entity.
   */
  public ReferenceGeometryEntity(Geometry geometry) {
    super(geometry, "reference", new HashMap<>(), new HashMap<>());
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.REFERENCE;
  }
}
