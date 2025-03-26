package org.joshsim.engine.entity;

import org.joshsim.engine.geometry.Geometry;


/**
 * RootSpatialEntity is a type of SpatialEntity which has its own geometry.
 *
 * <p>RootSpatialEntity is a type of SpatialEntity which has its own geometry as opposed to
 * inhering that geometry by being part of another entity.</p>
 */
public abstract class RootSpatialEntity implements SpatialEntity {
  private final Geometry geometry;
  
  /**
   * Constructs a RootSpatialEntity with the specified geometry.
   *
   * @param geometry the geometry associated with this entity.
   */
  public RootSpatialEntity(Geometry geometry) {
    this.geometry = geometry;
  }
  
  @Override
  public Geometry getGeometry() {
    return geometry;
  }
}
