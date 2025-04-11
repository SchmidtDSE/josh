/**
 * Manages caching for external path resources, to avoid repeated loading.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.external.cog;


import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.map.LRUMap;
import org.geotools.coverage.grid.GridCoverage2D;
import org.joshsim.engine.external.core.ExternalLayer;
import org.joshsim.engine.external.core.ExternalLayerDecorator;
import org.joshsim.engine.external.core.Request;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.RealizedDistribution;

/**
 * Class for managing caching of external path resources, matching requests to
 * cached resources to avoid repeated loading. This _should_ be generic to the
 * way that external resource is fulfilled to work with any external resource.
 */
public class CogCacheLayer extends ExternalLayerDecorator {
  // Cache GridCoverage objects by path instead of Request->RealizedDistribution
  private final Map<EngineGeometry, GridCoverage2D> coverageCache = new LRUMap<>();

  /**
   * Constructs an CogCacheLayer with a decorated external layer.
   *
   * @param decoratedLayer the external layer to be decorated
   */
  public CogCacheLayer(ExternalLayer decoratedLayer) {
    super(decoratedLayer);
  }

  @Override
  public RealizedDistribution fulfill(Request request) {

    if (request.getPrimingGeometry().isEmpty()) {
      // If the request has no primingGeometry, we can just use the decorated layer
      return super.fulfill(request);
    }

    // If the request has a priming geometry, we need to check if it's already cached
    EngineGeometry primingGeometry = request.getPrimingGeometry().orElseThrow();
    if (!coverageCache.containsKey(primingGeometry)) {
      loadCoverageIntoCache(request.getPath(), primingGeometry);
    }
    // Get the cached coverage, either from the cache or newly loaded
    GridCoverage2D cachedCoverage = coverageCache.get(primingGeometry);

    // Get the request geometry, which is the subset area for which we want to extract values
    EngineGeometry requestGeometry = request.getGeometry().orElseThrow();

    // Extract values from the cached coverage using the (subset) request geometry
    List<BigDecimal> decimalValuesWithinGeometry = CogReader.extractValuesFromCoverage(
        cachedCoverage,
        requestGeometry
    );

    RealizedDistribution realizedDistribution = RealizedDistribution.fromDecimalValues(
        getCaster(),
        decimalValuesWithinGeometry,
        getUnits()
    );

    // Create a new RealizedDistribution with the results
    return realizedDistribution;
  }

  /**
   * Loads a GridCoverage from disk and caches it.
   *
   * @param path the path to the coverage file
   * @param primingGeometry the geometry used to prime the cache
   */
  private void loadCoverageIntoCache(String path, EngineGeometry primingGeometry) {
    GridCoverage2D newCoverage;
    try {
      newCoverage = CogReader.getCoverageFromIo(path, primingGeometry);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load coverage from disk", e);
    }
    coverageCache.put(primingGeometry, newCoverage);
  }

  /**
   * Returns the size of the coverage cache.
   *
   * @return the size of the coverage cache
   */
  public int getCacheSize() {
    return coverageCache.size();
  }

  /**
   * Returns the cache of GridCoverage objects.
   *
   * @return the cache of GridCoverage objects
   */
  public Map<EngineGeometry, GridCoverage2D> getCoverageCache() {
    return coverageCache;
  }

  /**
   * Clears the coverage cache.
   */
  public void clearCache() {
    coverageCache.clear();
  }
}
