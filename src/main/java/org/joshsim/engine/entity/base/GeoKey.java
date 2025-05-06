/**
 * Structures describing keys for cells within a simulation across time steps.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.base;

import java.util.Objects;
import org.joshsim.engine.geometry.EngineGeometry;

/**
 * Represents a key to uniquely identify a Patch within a simulation across time steps.
 */
public class GeoKey {

  private final Entity entity;

  /**
   * Constructs a Key with the specified entity.
   *
   * @param entity The patch to be keyed.
   */
  public GeoKey(Entity entity) {
    this.entity = entity;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof GeoKey)) {
      return false;
    }
    GeoKey other = (GeoKey) o;
    
    // Compare entities by their geometry
    EngineGeometry thisGeom = entity.getGeometry().orElse(null);
    EngineGeometry otherGeom = other.entity.getGeometry().orElse(null);
    
    if (thisGeom == null || otherGeom == null) {
      return false;
    }

    return thisGeom.getOnGrid().equals(otherGeom.getOnGrid());
  }

  @Override
  public int hashCode() {
    EngineGeometry geom = entity.getGeometry().orElse(null);
    if (geom == null) {
      return 0;
    }
    return Objects.hash(geom.getOnGrid());
  }

  @Override
  public String toString() {
    EngineGeometry geometry = entity.getGeometry().orElseThrow();
    return String.format(
        "Entity of type %s at (%.6f, %.6f)",
        entity.getName(),
        geometry.getCenterX(),
        geometry.getCenterY()
    );
  }
}
