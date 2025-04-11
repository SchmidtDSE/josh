package org.joshsim.engine.external.core;

import java.util.Optional;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.RealizedDistribution;

/**
 * A decorator for external layers that uses a static priming geometry
 * which doesn't change after initialization.
 */
public class StaticPrimingGeometryLayer extends ExternalLayerDecorator {
  private Optional<EngineGeometry> primingGeometry = Optional.empty();

  /**
   * Constructs a StaticPrimingGeometryLayer with the specified decorated layer.
   *
   * @param decoratedLayer The external layer to be decorated
   */
  public StaticPrimingGeometryLayer(ExternalLayer decoratedLayer) {
    super(decoratedLayer);
  }

  /**
   * Returns the current priming geometry.
   *
   * @return The current priming geometry, or an empty optional if no geometry has been set
   */
  public Optional<EngineGeometry> getPrimingGeometry() {
    return primingGeometry;
  }

  /**
   * Sets the priming geometry to a new geometry.
   * This can only be done once during initialization.
   *
   * @param geometry The new geometry to set as the priming geometry
   */
  public void setPrimingGeometry(EngineGeometry geometry) {
    this.primingGeometry = Optional.of(geometry);
  }

  @Override
  public RealizedDistribution fulfill(Request request) {
    if (primingGeometry.isEmpty()) {
      throw new IllegalStateException("Static priming geometry not initialized");
    }

    // Set the priming geometry in the request for cache checking
    request.setPrimingGeometry(primingGeometry);

    // Pass the request to the decorated layer
    return super.fulfill(request);
  }
}