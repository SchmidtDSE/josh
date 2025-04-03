/**
 * Caches and manages geometry extents for optimized data loading.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.external;

import java.util.Optional;
import org.joshsim.engine.geometry.Geometry;

/**
 * An abstract class that decorates an external layer and manages a priming geometry.
 * The priming geometry is typically the intersection of all geometries added to the priming extent.
 */
public abstract class PrimingGeometryLayer extends ExternalLayerDecorator {
  private Optional<Geometry> primingGeometry;

  /**
   * Constructs a PrimingGeometryLayer with the specified decorated layer.
   *
   * @param decoratedLayer The external layer to be decorated
   */
  public PrimingGeometryLayer(ExternalLayer decoratedLayer) {
    super(decoratedLayer);
  }

  /**
   * Returns the current priming geometry, which is typically the intersection of all geometries
   * added to the    * priming extent.
   *
   * @return The current priming geometry, or an empty optional if no geometry has been set
   */
  Optional<Geometry> getPrimingGeometry() {
    return primingGeometry;
  }
  
  /**
   * Updates the priming geometry to include a new geometry, which will be the intersection of
   * the current priming geometry and the new geometry.
   *
   * @param geometry The geometry to add to the priming extent
   */
  void extendPrimingGeometry(Geometry geometry) {
    if (primingGeometry.isPresent()) {
      primingGeometry = Optional.of(primingGeometry.get().getIntersect(geometry));
    } else {
      primingGeometry = Optional.of(geometry);
    }
  }

  /**
   * Sets the priming geometry to a new geometry.
   *
   * @param geometry The new geometry to set as the priming geometry
   */
  void setPrimingGeometry(Geometry geometry) {
    this.primingGeometry = Optional.of(geometry);
  }
}