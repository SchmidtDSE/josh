/**
 * Structure to help in constructing a GridBuilderExtents.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import java.math.BigDecimal;


/**
 * Builder class for creating {@link GridBuilderExtents} instances.
 */
public class GridBuilderExtentsBuilder {

  private BigDecimal topLeftX;
  private BigDecimal topLeftY;
  private BigDecimal bottomRightX;
  private BigDecimal bottomRightY;

  /**
   * Sets the x-coordinate of the top-left corner.
   *
   * @param topLeftX The x-coordinate as a BigDecimal
   * @return this builder instance
   */
  public GridBuilderExtentsBuilder setTopLeftX(BigDecimal topLeftX) {
    this.topLeftX = topLeftX;
    return this;
  }

  /**
   * Sets the y-coordinate of the top-left corner.
   *
   * @param topLeftY The y-coordinate as a BigDecimal
   * @return this builder instance
   */
  public GridBuilderExtentsBuilder setTopLeftY(BigDecimal topLeftY) {
    this.topLeftY = topLeftY;
    return this;
  }

  /**
   * Sets the x-coordinate of the bottom-right corner.
   *
   * @param bottomRightX The x-coordinate as a BigDecimal
   * @return this builder instance
   */
  public GridBuilderExtentsBuilder setBottomRightX(BigDecimal bottomRightX) {
    this.bottomRightX = bottomRightX;
    return this;
  }

  /**
   * Sets the y-coordinate of the bottom-right corner.
   *
   * @param bottomRightY The y-coordinate as a BigDecimal
   * @return this builder instance
   */
  public GridBuilderExtentsBuilder setBottomRightY(BigDecimal bottomRightY) {
    this.bottomRightY = bottomRightY;
    return this;
  }

  /**
   * Builds a new {@link GridBuilderExtents} instance with the currently set coordinates.
   *
   * @return a new GridBuilderExtents instance
   * @throws IllegalStateException if any of the required coordinates are not set
   */
  public GridBuilderExtents build() {
    if (topLeftX == null || topLeftY == null || bottomRightX == null || bottomRightY == null) {
      throw new IllegalStateException("All coordinates must be set before building");
    }
    return new GridBuilderExtents(topLeftX, topLeftY, bottomRightX, bottomRightY);
  }
}
