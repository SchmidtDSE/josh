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
 * <p>Maps radius (in grid cells) to list of (offsetX, offsetY) offsets
 * that intersect a circle of that radius centered at origin.</p>
 *
 * <p>Thread-safe using ConcurrentHashMap for parallel query processing.
 * Cache entries are never evicted as typical simulations use a small number
 * of distinct radii.</p>
 *
 * <p>Cache key: exact radiusInGridCells double value. The same within expression
 * always produces bitwise-identical values from the same BigDecimal inputs.</p>
 */
class CircleOffsetsCache {
  private static final Map<Double, List<GridOffset>> CIRCLE_OFFSETS_CACHE =
      new ConcurrentHashMap<>();

  /**
   * Gets or computes the list of grid cell offsets that intersect a circle.
   *
   * <p>This method uses the existing isSquareIntersectingCircle logic to determine
   * which (dx, dy) offsets should be included. The result is cached globally
   * and reused for all subsequent queries with the same radius.</p>
   *
   * <p>The cache key is the exact radiusInGridCells double value. Each distinct
   * radius gets its own cached offset set, avoiding collisions between different
   * radii that previously mapped to the same ceiling integer.</p>
   *
   * <p>Thread-safety: Uses ConcurrentHashMap.putIfAbsent for safe concurrent
   * first-access. Multiple threads may compute offsets simultaneously on first
   * access, but only one result is cached (acceptable redundant work).</p>
   *
   * @param radiusInGridCells the circle radius in grid cell units
   * @return immutable list of (offsetX, offsetY) offsets relative to circle center
   */
  static List<GridOffset> getOffsetsForRadius(double radiusInGridCells) {
    List<GridOffset> cached = CIRCLE_OFFSETS_CACHE.get(radiusInGridCells);
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
    CIRCLE_OFFSETS_CACHE.putIfAbsent(radiusInGridCells, immutableOffsets);

    return CIRCLE_OFFSETS_CACHE.get(radiusInGridCells);
  }

  /**
   * Clears the global offset cache.
   *
   * <p>Intended for test isolation — the static cache persists across JUnit tests
   * in the same JVM, which can cause cross-test contamination.</p>
   */
  static void clearCache() {
    CIRCLE_OFFSETS_CACHE.clear();
  }
}
