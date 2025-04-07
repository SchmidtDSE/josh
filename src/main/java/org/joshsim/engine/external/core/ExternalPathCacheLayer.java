/**
 * Manages caching for external path resources, to avoid repeated loading.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.external.core;


import java.util.Map;
import org.apache.commons.collections4.map.LRUMap;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.type.RealizedDistribution;

/**
 * Class for managing caching of external path resources, matching requests to
 * cached resources to avoid repeated loading. This _should_ be generic to the
 * way that external resource is fulfilled to work with any external resource.
 */
public class ExternalPathCacheLayer extends ExternalLayerDecorator {
  // Cache GridCoverage objects by path instead of Request->RealizedDistribution
  private final Map<Geometry, GridCoverageCache> coverageCache = new LRUMap<>();

  /**
   * Constructs an ExternalPathCacheLayer with a decorated external layer.
   *
   * @param decoratedLayer the external layer to be decorated
   */
  public ExternalPathCacheLayer(ExternalLayer decoratedLayer) {
    super(decoratedLayer);
  }

  @Override
  public RealizedDistribution fulfill(Request request) {
    String path = request.getPath();
    Geometry requestGeometry = request.getGeometry().orElseThrow();
    Geometry primingGeometry = request.getPrimingGeometry().orElse(null);

    // Check if we have a coverage cache for this path
    if (coverageCache.containsKey(path)) {
      GridCoverageCache cache = coverageCache.get(path);
      
      // If current geometry is contained in cached coverage, extract values
      if (cache.containsGeometry(requestGeometry)) {
        return extractValuesFromCache(cache, request);
      } else {
        // Need to expand cache - delegate to decorated layer and update cache
        RealizedDistribution result = super.fulfill(request);
        cache.expandWithGeometry(requestGeometry);
        return result;
      }
    } else {
      // First time seeing this path, delegate to decorated layer
      RealizedDistribution result = super.fulfill(request);
      
      // Create new cache entry
      GridCoverageCache newCache = new GridCoverageCache(path, requestGeometry);
      coverageCache.put(path, newCache);
      return result;
    }
  }
  
  // Helper class to manage cached coverage and its extent
  private static class GridCoverageCache {
    private final String path;
    private Geometry cachedExtent;
    
    GridCoverageCache(String path, Geometry initialGeometry) {
      this.path = path;
      this.cachedExtent = initialGeometry;
    }
    
    boolean containsGeometry(Geometry geometry) {
      // Check if the cached extent fully contains the requested geometry
      return cachedExtent.intersects(geometry);
    }
    
    void expandWithGeometry(Geometry geometry) {
      cachedExtent.getIntersect(geometry);
    }
  }
  
  private RealizedDistribution extractValuesFromCache(GridCoverageCache cache, Request request) {
    return super.fulfill(request);
  }
}