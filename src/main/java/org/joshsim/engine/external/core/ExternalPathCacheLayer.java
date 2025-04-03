/**
 * Manages caching for external path resources, to avoid repeated loading.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.external.core;

import java.util.HashMap;
import java.util.Map;
import org.joshsim.engine.value.type.RealizedDistribution;

/**
 * Class for managing caching of external path resources, matching requests to
 * cached resources to avoid repeated loading. This _should_ be generic to the
 * way that external resource is fulfilled to work with any external resource.
 */
public class ExternalPathCacheLayer extends ExternalLayerDecorator {
  private final Map<Request, RealizedDistribution> cache = new HashMap<>();

  /**
   * Constructs a patch cache layer.
   *
   * @param decoratedLayer The layer to decorate
   */
  public ExternalPathCacheLayer(ExternalLayer decoratedLayer) {
    super(decoratedLayer);
  }

  @Override
  public RealizedDistribution fulfill(Request request) {

    // Check if we have this resource in cache
    if (cache.containsKey(request)) {
      return cache.get(request);
    }

    // Not in cache, delegate to decorated layer
    RealizedDistribution result = super.fulfill(request);

    // Cache the result
    cache.put(request, result);

    return result;
  }

  /**
   * Clears any cached resources.
   */
  void clearCache() {
    cache.clear();
  }

  /**
   * Returns the number of cached resources.
   *
   * @return Number of cached resources
   */
  int getCacheSize() {
    return cache.size();
  }
}
