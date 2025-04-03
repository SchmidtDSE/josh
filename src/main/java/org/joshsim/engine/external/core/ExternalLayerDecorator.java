/**
 * Base class for external layer decorators.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.external.core;

import org.joshsim.engine.value.type.RealizedDistribution;

/**
 * An abstract decorator class for the ExternalLayer interface.
 * This class provides a base implementation for wrapping an ExternalLayer instance,
 * which will extend its functionality to support different file types and caching logic.
 */
public abstract class ExternalLayerDecorator implements ExternalLayer {
  protected final ExternalLayer decoratedLayer;
  
  /**
   * Constructs an ExternalLayerDecorator with the specified ExternalLayer.
   *
   * @param decoratedLayer the ExternalLayer to be decorated
   */
  public ExternalLayerDecorator(ExternalLayer decoratedLayer) {
    this.decoratedLayer = decoratedLayer;
  }
  
  @Override
  public RealizedDistribution fulfill(Request request) {
    return decoratedLayer.fulfill(request);
  }
}
