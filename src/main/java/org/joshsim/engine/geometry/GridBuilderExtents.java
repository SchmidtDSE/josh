
/**
 * Structure to represent the extents of a grid in terms of its corner coordinates.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import java.math.BigDecimal;

/**
 * Structure to store and validate corner coordinates for grid construction.
 */
public class GridBuilderExtents {
  private final BigDecimal topLeftX;
  private final BigDecimal topLeftY;
  private final BigDecimal bottomRightX;
  private final BigDecimal bottomRightY;

  /**
   * Create extents over the given coordinates.
   *
   * @param topLeftX The x-coordinate of the top-left corner
   * @param topLeftY The y-coordinate of the top-left corner
   * @param bottomRightX The x-coordinate of the bottom-right corner
   * @param bottomRightY The y-coordinate of the bottom-right corner
   * @throws IllegalArgumentException if coordinates don't form a valid rectangle
   */
  public GridBuilderExtents(BigDecimal topLeftX, BigDecimal topLeftY, BigDecimal bottomRightX,
        BigDecimal bottomRightY) {
    this.topLeftX = topLeftX;
    this.topLeftY = topLeftY;
    this.bottomRightX = bottomRightX;
    this.bottomRightY = bottomRightY;
    validateCornerCoordinates();
  }

  /**
   * Gets the x-coordinate of the top-left corner.
   *
   * @return the x-coordinate as a BigDecimal
   */
  public BigDecimal getTopLeftX() {
    return topLeftX;
  }

  /**
   * Gets the y-coordinate of the top-left corner.
   *
   * @return the y-coordinate as a BigDecimal
   */
  public BigDecimal getTopLeftY() {
    return topLeftY;
  }

  /**
   * Gets the x-coordinate of the bottom-right corner.
   *
   * @return the x-coordinate as a BigDecimal
   */
  public BigDecimal getBottomRightX() {
    return bottomRightX;
  }

  /**
   * Gets the y-coordinate of the bottom-right corner.
   *
   * @return the y-coordinate as a BigDecimal
   */
  public BigDecimal getBottomRightY() {
    return bottomRightY;
  }

  /**
   * Validates corner coordinates based on coordinate system type.
   * For both geographic and projected coordinates, we expect Y to increase northward
   * and X to increase eastward.
   */
  public void validateCornerCoordinates() {
    if (topLeftX == null || topLeftY == null || bottomRightX == null || bottomRightY == null) {
      throw new IllegalArgumentException("Missing corner coordinates");
    }

    // Consistent validation for both geographic and projected coordinates
    // Y-coordinate (latitude/northing) should decrease from top to bottom
    if (topLeftY.compareTo(bottomRightY) <= 0) {
      throw new IllegalArgumentException(
          "Top-left Y-coordinate must be greater than bottom-right Y-coordinate"
      );
    }

    // X-coordinate (longitude/easting) should increase from left to right
    if (topLeftX.compareTo(bottomRightX) >= 0) {
      throw new IllegalArgumentException(
          "Top-left X-coordinate must be less than bottom-right X-coordinate"
      );
    }
  }
}
