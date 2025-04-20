
/**
 * Structures describing a square shape in grid space.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry.shape;

import java.math.BigDecimal;

/**
 * Geometric shape representing a square in grid coordinates.
 */
public class GridSquare extends GridShape {

  private final BigDecimal width;

  /**
   * Create a new square in grid space.
   *
   * @param x The x-coordinate of the square's center point
   * @param y The y-coordinate of the square's center point
   * @param width The width of the square's sides
   */
  public GridSquare(BigDecimal x, BigDecimal y, BigDecimal width) {
    super(x, y);
    this.width = width;
  }

  @Override
  public GridShapeType getGridShapeType() {
    return GridShapeType.SQUARE;
  }

  @Override
  public BigDecimal getWidth() {
    return width;
  }

  @Override
  public BigDecimal getHeight() {
    return width;
  }

  @Override
  public String toString() {
    return String.format(
        "GridSquare at (%s, %s) of width %s.",
        getCenterX().toString(),
        getCenterY().toString(),
        getWidth().toString()
    );
  }
}
