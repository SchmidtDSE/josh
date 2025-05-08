/**
 * Utilities to convert from Earth-space patch keys to grid-space patch keys.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import java.math.BigDecimal;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.geometry.HaversineUtil;
import org.joshsim.engine.geometry.PatchBuilderExtents;


/**
 * Utility to convert from earth-space patch keys to grid-space patch keys.
 *
 * <p>Utility which converts from Earth-space patch keys which report longitude in degrees for x
 * and latitude in degrees for y into cell index column for x and row for y.</p>
 */
public class PatchKeyConverter {

  private final PatchBuilderExtents geoExtents;
  private final HaversineUtil.HaversinePoint startPoint;
  private final BigDecimal patchWidth;

  /**
   * Create a utility which performs conversions from patch keys in geoExtents to grid-space.
   *
   * @param geoExtents Extents of the Earth-space grid from which patch keys will be converted.
   * @param patchWidth The width of the patch in meters.
   */
  public PatchKeyConverter(PatchBuilderExtents geoExtents, BigDecimal patchWidth) {
    this.geoExtents = geoExtents;
    this.startPoint = new HaversineUtil.HaversinePoint(
        geoExtents.getTopLeftX(),
        geoExtents.getTopLeftY()
    );
    this.patchWidth = patchWidth;
  }

  /**
   * Convert a single patch key to grid-space.
   *
   * @param key The patch key to be converted.
   * @return A new patch key in grid-space.
   */
  public GeoKey convert(GeoKey key) {
    // TODO
  }

}
