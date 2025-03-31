package org.joshsim.engine.entity.base;

import java.util.HashMap;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.Geometry;

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
