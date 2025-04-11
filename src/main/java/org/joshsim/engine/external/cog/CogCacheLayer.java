package org.joshsim.engine.external.cog;

import org.joshsim.engine.external.core.ExternalLayer;
import org.joshsim.engine.external.core.GridCoverageCacheLayer;

/**
 * Concrete implementation of GridCoverageCacheLayer for COG files.
 */
public class CogCacheLayer extends GridCoverageCacheLayer {
  /**
   * Constructs a CogCacheLayer with a decorated external layer.
   *
   * @param decoratedLayer the external layer to be decorated
   */
  public CogCacheLayer(ExternalLayer decoratedLayer) {
    super(decoratedLayer, new CogReader());
  }
}