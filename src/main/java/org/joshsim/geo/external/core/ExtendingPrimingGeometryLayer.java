/**
 * Caches and manages geometry extents for optimized data loading.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.geo.external.core;

import java.util.Optional;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.RealizedDistribution;
import org.joshsim.geo.geometry.EarthGeometry;

/**
 * An abstract class that decorates an external layer and manages a priming geometry.
 * The priming geometry is typically the intersection of all geometries added to the priming extent.
 */
public class ExtendingPrimingGeometryLayer extends ExternalLayerDecorator {
  private Optional<EarthGeometry> primingGeometry = Optional.empty();

  /**
   * Constructs a PrimingGeometryLayer with the specified decorated layer.
   *
   * @param decoratedLayer The external layer to be decorated
   */
  public ExtendingPrimingGeometryLayer(ExternalLayer decoratedLayer) {
    super(decoratedLayer);
  }

  /**
   * Returns the current priming geometry, which is typically the intersection of all geometries
   * added to the    * priming extent.
   *
   * @return The current priming geometry, or an empty optional if no geometry has been set
   */
  Optional<EarthGeometry> getPrimingGeometry() {
    return primingGeometry;
  }

  /**
   * Updates the priming geometry to include a new geometry, which will be the intersection of
   * the current priming geometry and the new geometry.
   *
   * @param geometry The geometry to add to the priming extent
   */
  void extendPrimingGeometry(EarthGeometry geometry) {
    // TODO: getConvexHull() is not implemented yet, and may be kind of a pain with Spatial4j
    if (primingGeometry.isPresent()) {
      primingGeometry = Optional.of(primingGeometry.get().getConvexHull(geometry));
    } else {
      setPrimingGeometry(geometry);
    }
  }

  /**
   * Sets the priming geometry to a new geometry.
   *
   * @param geometry The new geometry to set as the priming geometry
   */
  void setPrimingGeometry(EarthGeometry geometry) {
    this.primingGeometry = Optional.of(geometry);
  }

  @Override
  public RealizedDistribution fulfill(Request request) {
    // Get the geometry from the request
    EarthGeometry requestGeometry = request.getGeometry().orElseThrow().getOnEarth();

    // Update our priming geometry to include this request
    extendPrimingGeometry(requestGeometry);

    // Set the priming geometry in the request for later cache checking
    EngineGeometry primingGeometryGeneral = primingGeometry.orElseThrow();
    request.setPrimingGeometry(Optional.of(primingGeometryGeneral));

    // Pass the request to the decorated layer
    return super.fulfill(request);
  }
}
