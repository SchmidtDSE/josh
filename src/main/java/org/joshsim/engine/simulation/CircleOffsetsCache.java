/**
 * Global cache of precomputed grid offsets for circle queries.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global cache of precomputed grid offsets for circle queries.
 *
 * <p>Maps radius (in grid cells, ceiled to integer) to list of (offsetX, offsetY) offsets
 * that intersect a circle of that radius centered at origin.</p>
 *
 * <p>Thread-safe using ConcurrentHashMap for parallel query processing.
 * Cache entries are never evicted as typical simulations use a small number
 * of distinct radii.</p>
 *
 * <p>Cache key: ceil(radiusInGridCells) ensures conservative correctness
 * by never missing cells that should be included.</p>
 */
class CircleOffsetsCache {
  private static final Map<Integer, List<GridOffset>> CIRCLE_OFFSETS_CACHE =
      new ConcurrentHashMap<>();

  /**
   * Gets or computes the list of grid cell offsets that intersect a circle.
   *
   * <p>This method uses the existing isSquareIntersectingCircle logic to determine
   * which (dx, dy) offsets should be included. The result is cached globally
   * and reused for all subsequent queries with the same radius.</p>
   *
   * <p>The cache key is ceil(radiusInGridCells) for conservative correctness.
   * Using ceiling ensures we never miss cells that should be included, though
   * it may include a few extra cells for fractional radii.</p>
   *
   * <p>Thread-safety: Uses ConcurrentHashMap.putIfAbsent for safe concurrent
   * first-access. Multiple threads may compute offsets simultaneously on first
   * access, but only one result is cached (acceptable redundant work).</p>
   *
   * @param radiusInGridCells the circle radius in grid cell units
   * @return immutable list of (offsetX, offsetY) offsets relative to circle center
   */
  static List<GridOffset> getOffsetsForRadius(double radiusInGridCells) {
    int radiusKey = (int) Math.ceil(radiusInGridCells);

    List<GridOffset> cached = CIRCLE_OFFSETS_CACHE.get(radiusKey);
    if (cached != null) {
      return cached;
    }

    int maxOffset = (int) Math.ceil(radiusInGridCells + Math.sqrt(2.0));
    int estimatedSize = (2 * maxOffset + 1) * (2 * maxOffset + 1);
    List<GridOffset> offsets = new ArrayList<>(estimatedSize);

    for (int dx = -maxOffset; dx <= maxOffset; dx++) {
      for (int dy = -maxOffset; dy <= maxOffset; dy++) {
        if (PatchSpatialIndex.isSquareIntersectingCircle(dx, dy, radiusInGridCells)) {
          offsets.add(new GridOffset(dx, dy));
        }
      }
    }

    List<GridOffset> immutableOffsets = Collections.unmodifiableList(offsets);
    CIRCLE_OFFSETS_CACHE.putIfAbsent(radiusKey, immutableOffsets);

    return CIRCLE_OFFSETS_CACHE.get(radiusKey);
  }
}
