/**
 * Manages caching for external path resources, to avoid repeated loading.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.external;


/**
 * Abstract class for managing caching of external path resources.
 */
public abstract class ExternalPathCacheLayer extends ExternalLayerDecorator {
  /**
   * Decorates an external layer with caching functionality.
   *
   * @param decoratedLayer The layer to decorate
   */
  public ExternalPathCacheLayer(ExternalLayer decoratedLayer) {
    super(decoratedLayer);
  }

  /**
   * Clears any cached resources.
   */
  abstract void clearCache();

  /**
   * Returns the number of cached resources.
   *
   * @return Number of cached resources
   */
  abstract int getCacheSize();
}