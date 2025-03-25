
package org.joshsim.engine.entity;

import org.joshsim.engine.geometry.Geometry;

public abstract class RootSpatialEntity implements SpatialEntity {
  private final Geometry geometry;

  @Override
  public Geometry getGeometry() {
    return geometry;
  }

  public RootSpatialEntity(Geometry geometry) {
    this.geometry = geometry;
  }
}
