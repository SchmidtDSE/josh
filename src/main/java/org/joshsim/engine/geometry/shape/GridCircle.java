/**
 * Structures describing a circular shape in grid space.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry.shape;

import java.math.BigDecimal;

/**
 * Geometric shape representing a circle in grid coordinates.
 *
 * <p>This class defines a circle by its center point and diameter, allowing for precise circular
 * representations in grid-based spatial systems like radial operations.</p>
 */
public class GridCircle extends GridShape {

  private final BigDecimal diameter;

  /**
   * Create a new circle in grid space.
   *
   * @param x The x-coordinate of the circle's center point
   * @param y The y-coordinate of the circle's center point
   * @param radius The radius of the circle
   */
  public GridCircle(BigDecimal x, BigDecimal y, BigDecimal radius) {
    super(x, y);
    diameter = radius.multiply(BigDecimal.TWO);
  }

  @Override
  public GridShapeType getGridShapeType() {
    return GridShapeType.CIRCLE;
  }

  @Override
  public BigDecimal getWidth() {
    return diameter;
  }

  @Override
  public BigDecimal getHeight() {
    return diameter;
  }

  @Override
  public String toString() {
    return String.format(
        "GridCircle at (%s, %s) of diameter %s.",
        getCenterX().toString(),
        getCenterY().toString(),
        getWidth().toString()
    );
  }
}
