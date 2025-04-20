/**
 * Structures describing a point in grid space.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry.shape;

import java.math.BigDecimal;
import org.joshsim.engine.geometry.EnginePoint;


/**
 * Geometric shape representing a point in grid space coordinates.
 */
public class GridPoint extends GridShape implements EnginePoint {

  /**
   * Create a new point in grid space.
   *
   * @param locationX The x-coordinate of the point
   * @param locationY The y-coordinate of the point
   */
  public GridPoint(BigDecimal locationX, BigDecimal locationY) {
    super(locationX, locationY);
  }

  @Override
  public GridShapeType getGridShapeType() {
    return GridShapeType.POINT;
  }

  @Override
  public BigDecimal getWidth() {
    return BigDecimal.ZERO;
  }

  @Override
  public BigDecimal getHeight() {
    return BigDecimal.ZERO;
  }

  @Override
  public BigDecimal getX() {
    return getCenterX();
  }

  @Override
  public BigDecimal getY() {
    return getCenterY();
  }

  @Override
  public String toString() {
    return String.format(
        "GridPoint at (%s, %s).",
        getCenterX().toString(),
        getCenterY().toString()
    );
  }
}
