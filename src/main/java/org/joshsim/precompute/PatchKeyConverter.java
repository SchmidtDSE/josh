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
   * @param value The value at the location.
   * @return A new patch key in grid-space with the associated value.
   */
  public ProjectedValue convert(GeoKey key, BigDecimal value) {
    // Calculate horizontal distance from start point (going east)
    BigDecimal horizontalDistance = HaversineUtil.getDistance(
        startPoint,
        new HaversineUtil.HaversinePoint(key.getCenterX(), startPoint.getLatitude())
    );
    
    // Calculate vertical distance from start point (going south)
    BigDecimal verticalDistance = HaversineUtil.getDistance(
        startPoint,
        new HaversineUtil.HaversinePoint(startPoint.getLongitude(), key.getCenterY())
    );
    
    // Convert distances to grid cell indices by dividing by patch width
    BigDecimal gridX = horizontalDistance.divide(patchWidth, 0, BigDecimal.ROUND_FLOOR);
    BigDecimal gridY = verticalDistance.divide(patchWidth, 0, BigDecimal.ROUND_FLOOR);
    
    return new ProjectedValue(gridX, gridY, value);
  }

  
  /**
   * Result of a projection from Earth-space to grid-space.
   */
  public static class ProjectedValue {

    private final BigDecimal positionX;
    private final BigDecimal positionY;
    private final BigDecimal value;

    /**
     * Create a new record of a projection.
     *
     * @param positionX The horizontal position in grid-space where the value is located.
     * @param positionY The vertical position in grid-space where the value is located.
     * @param value The value whose positoin was projected.
     */
    public ProjectedValue(BigDecimal positionX, BigDecimal positionY, BigDecimal value) {
      this.positionX = positionX;
      this.positionY = positionY;
      this.value = value;
    }

    /**
     * Get the horizontal location of the projected point on grid-space.
     *
     * @returns Zero indexed number of patch width to the horizontal center position of this point.
     */
    public BigDecimal getX() {
      return positionX;
    }

    /**
     * Get the vertical location of the projected point on grid-space.
     *
     * @returns Zero indexed number of patch width to the vertical center position of this point.
     */
    public BigDecimal getY() {
      return positionY;
    }

    /**
     * Get the value that was projected.
     *
     * @returns The value at the projected point.
     */
    public BigDecimal getValue() {
      return value;
    }
    
  }

}
