
/**
 * Utility to combine two DataGridLayers.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Utility to combine two DataGridLayers.
 *
 * <p>Utility which combines two DataGridLayers by specifying which one's values should take
 * precedence if there is a collision. Note that this specifically combines into a
 * DoublePrecomputedGrid.</p>
 */
public class GridCombiner {

  /**
   * Combine two DataGridLayers into a single DataGridLayer.
   *
   * <p>Combine two DataGridLayers together into a single DataGridLayer where values from the right
   * grid are used if there is a conflict. Note that this specifically combines into a
   * DoublePrecomputedGrid so values are held in memory.</p>
   *
   * @param left The first DataGridLayer to combine and from which values are overwritten by right
   *     in the case of a conflict.
   * @param right The second DataGridLayer to combine and which takes precedence if there is a
   *     conflict.
   * @return Newly created DataGridLayer which combines the two input layers.
   */
  public static DataGridLayer combine(DataGridLayer left, DataGridLayer right) {
    if (left == null || right == null) {
      throw new IllegalArgumentException("Both grids must be non-null");
    }

    // Get extents from right grid since it takes precedence
    PatchBuilderExtents extents = null;
    long minTimestep = Long.MAX_VALUE;
    long maxTimestep = Long.MIN_VALUE;
    
    // Find the overlapping time range and extents
    for (long t = 0; t < Long.MAX_VALUE; t++) {
      if (left.isCompatible(extents, t, t) && right.isCompatible(extents, t, t)) {
        minTimestep = Math.min(minTimestep, t);
        maxTimestep = Math.max(maxTimestep, t);
      } else if (t > maxTimestep) {
        break;
      }
    }

    if (minTimestep == Long.MAX_VALUE || maxTimestep == Long.MIN_VALUE) {
      throw new IllegalArgumentException("No overlapping timesteps found between grids");
    }

    // Create new grid using builder
    DoublePrecomputedGridBuilder builder = new DoublePrecomputedGridBuilder()
        .setEngineValueFactory(EngineValueFactory.getDefault())
        .setExtents(extents)
        .setTimestepRange(minTimestep, maxTimestep)
        .setUnits(right.getUnits());

    // Build initial grid
    DoublePrecomputedGrid combinedGrid = builder.build();

    // Copy values from left grid first
    for (long t = minTimestep; t <= maxTimestep; t++) {
      for (long x = combinedGrid.getMinX(); x <= combinedGrid.getMaxX(); x++) {
        for (long y = combinedGrid.getMinY(); y <= combinedGrid.getMaxY(); y++) {
          try {
            GeoKey key = new GeoKey(x, y);
            EngineValue value = left.getAt(key, t);
            if (value != null) {
              combinedGrid.setAt(x, y, t, value.getAsDecimal().doubleValue());
            }
          } catch (Exception e) {
            // Skip invalid coordinates
          }
        }
      }
    }

    // Overwrite with values from right grid
    for (long t = minTimestep; t <= maxTimestep; t++) {
      for (long x = combinedGrid.getMinX(); x <= combinedGrid.getMaxX(); x++) {
        for (long y = combinedGrid.getMinY(); y <= combinedGrid.getMaxY(); y++) {
          try {
            GeoKey key = new GeoKey(x, y);
            EngineValue value = right.getAt(key, t);
            if (value != null) {
              combinedGrid.setAt(x, y, t, value.getAsDecimal().doubleValue());
            }
          } catch (Exception e) {
            // Skip invalid coordinates
          }
        }
      }
    }

    return combinedGrid;
  }
}
