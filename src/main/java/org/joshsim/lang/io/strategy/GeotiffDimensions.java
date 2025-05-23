/**
 * Internal structures to represent geotiff dimensions.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.joshsim.engine.geometry.HaversineUtil;
import org.joshsim.engine.geometry.PatchBuilderExtents;


/**
 * Internal record describing the dimensions of a geotiff.
 */
public class GeotiffDimensions {

  private final double minLon;
  private final double maxLon;
  private final double minLat;
  private final double maxLat;
  private final double widthInMeters;
  private final int gridWidthPixels;
  private final int gridHeightPixels;
  private final BigDecimal widthMeters;
  private final BigDecimal heightMeters;

  /**
   * Create a new dimensions record.
   *
   * @param extents The extents of the simulation in Earth-space where values are in degrees.
   * @param width The width and height of each patch (and, thus, each pixel) in meters.
   */
  public GeotiffDimensions(PatchBuilderExtents extents, BigDecimal width) {
    // Process data from records where these are in degrees
    BigDecimal minLonBig = extents.getTopLeftX();
    minLon = minLonBig.doubleValue();

    BigDecimal maxLonBig = extents.getBottomRightX();
    maxLon = maxLonBig.doubleValue();

    BigDecimal minLatBig = extents.getBottomRightY();
    minLat = minLatBig.doubleValue();

    BigDecimal maxLatBig = extents.getTopLeftY();
    maxLat = maxLatBig.doubleValue();

    widthInMeters = width.doubleValue();

    // Calculate grid dimensions using HaversineUtil
    HaversineUtil.HaversinePoint topLeft = new HaversineUtil.HaversinePoint(
        minLonBig,
        maxLatBig
    );
    HaversineUtil.HaversinePoint topRight = new HaversineUtil.HaversinePoint(
        maxLonBig,
        maxLatBig
    );
    HaversineUtil.HaversinePoint bottomLeft = new HaversineUtil.HaversinePoint(
        minLonBig,
        minLatBig
    );

    widthMeters = HaversineUtil.getDistance(topLeft, topRight);
    heightMeters = HaversineUtil.getDistance(topLeft, bottomLeft);

    gridWidthPixels = widthMeters.divide(width, 0, RoundingMode.CEILING).intValue();
    gridHeightPixels = heightMeters.divide(width, 0, RoundingMode.CEILING).intValue();
  }

  /**
   * Get the minimum longitude of the grid in degrees.
   *
   * @return The minimum longitude value.
   */
  public double getMinLon() {
    return minLon;
  }

  /**
   * Get the maximum longitude of the grid in degrees.
   *
   * @return The maximum longitude value.
   */
  public double getMaxLon() {
    return maxLon;
  }

  /**
   * Get the minimum latitude of the grid in degrees.
   *
   * @return The minimum latitude value.
   */
  public double getMinLat() {
    return minLat;
  }

  /**
   * Get the maximum latitude of the grid in degrees.
   *
   * @return The maximum latitude value.
   */
  public double getMaxLat() {
    return maxLat;
  }

  /**
   * Get the width of each patch/pixel in meters.
   *
   * @return The width in meters.
   */
  public double getPatchWidthInMeters() {
    return widthInMeters;
  }

  /**
   * Get the width of the grid in pixels.
   *
   * @return The number of pixels in width.
   */
  public int getGridWidthPixels() {
    return gridWidthPixels;
  }

  /**
   * Get the height of the grid in pixels.
   *
   * @return The number of pixels in height.
   */
  public int getGridHeightPixels() {
    return gridHeightPixels;
  }

  /**
   * Retrieves the width of the grid in meters.
   *
   * @return The grid's width as a BigDecimal.
   */
  public BigDecimal getGridWidthMeters() {
    return widthMeters;
  }

  /**
   * Retrieves the height of the grid in meters.
   *
   * @return The grid's height as a BigDecimal.
   */
  public BigDecimal getGridHeightMeters() {
    return heightMeters;
  }

}
