/**
 * Utility to transform an extents from Earth space to grid space.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import java.math.BigDecimal;
import org.joshsim.engine.geometry.HaversineUtil;
import org.joshsim.engine.geometry.PatchBuilderExtents;


/**
 * Utility to transform Earth to grid space in extents.
 */
public class ExtentsTransformer {

  /**
   * Convert a set of extents from degrees to meters.
   *
   * <p>Convert a set of extents from degrees to coordinates expressed in cell / patch counts via
   * conversion to meters using Haverzine where the upper left corner is 0, 0 and the bottom right
   * is positive. This is done using HaversineUtil.</p>
   *
   * @param extents Original extents expressed in degrees which should be converted to meters and
   *     then cell counts.
   * @param sizeMeters Size of each cell / patch in meters where each patch is a square.
   */
  public static PatchBuilderExtents transformToGrid(PatchBuilderExtents extents,
        BigDecimal sizeMeters) {
    BigDecimal width = HaversineUtil.getDistance(
        new HaversineUtil.HaversinePoint(extents.getTopLeftX(), extents.getTopLeftY()),
        new HaversineUtil.HaversinePoint(extents.getBottomRightX(), extents.getTopLeftY())
    );
    BigDecimal height = HaversineUtil.getDistance(
        new HaversineUtil.HaversinePoint(extents.getTopLeftX(), extents.getTopLeftY()),
        new HaversineUtil.HaversinePoint(extents.getTopLeftX(), extents.getBottomRightY())
    );

    BigDecimal gridWidth = width.divide(sizeMeters, 0, BigDecimal.ROUND_CEILING);
    BigDecimal gridHeight = height.divide(sizeMeters, 0, BigDecimal.ROUND_CEILING);

    return new PatchBuilderExtents(
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        gridWidth,
        gridHeight
    );
  }

}
