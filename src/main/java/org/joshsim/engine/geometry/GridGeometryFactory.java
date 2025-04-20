/**
 * Structures describing geometric shapes and their properties in grid space.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import org.joshsim.engine.geometry.shape.GridCircle;
import org.joshsim.engine.geometry.shape.GridPoint;
import org.joshsim.engine.geometry.shape.GridSquare;

/**
 * Factory for creating geometric shapes in grid space.
 *
 * <p>This factory provides methods to create basic geometric shapes like squares,
 * circles and points in a grid-based coordinate system. All measurements and
 * coordinates use BigDecimal for precise calculations.</p>
 */
public class GridGeometryFactory implements EngineGeometryFactory {

  @Override
  public EngineGeometry createSquare(BigDecimal centerX, BigDecimal centerY, BigDecimal width) {
    return new GridSquare(centerX, centerY, width);
  }

  @Override
  public EngineGeometry createSquare(BigDecimal topLeftX, BigDecimal topLeftY,
        BigDecimal bottomRightX, BigDecimal bottomRightY) {

    BigDecimal width = bottomRightX.subtract(topLeftX);
    return new GridSquare(
        topLeftX.add(width.divide(BigDecimal.TWO)),
        topLeftY.add(width.divide(BigDecimal.TWO)),
        width
    );
  }

  @Override
  public EngineGeometry createCircle(BigDecimal centerX, BigDecimal centerY, BigDecimal radius) {
    return new GridCircle(centerX, centerY, radius);
  }

  @Override
  public EngineGeometry createCircle(BigDecimal point1X, BigDecimal point1Y, BigDecimal point2X,
        BigDecimal point2Y) {
    BigDecimal centerX = point1X.add(point2X).divide(BigDecimal.TWO);
    BigDecimal centerY = point1Y.add(point2Y).divide(BigDecimal.TWO);
    BigDecimal radius = BigDecimal.valueOf(Math.sqrt(
        point2X.subtract(point1X).pow(2).add(point2Y.subtract(point1Y).pow(2)).doubleValue()
    )).divide(BigDecimal.TWO);
    return new GridCircle(centerX, centerY, radius);
  }

  @Override
  public EngineGeometry createPoint(BigDecimal x, BigDecimal y) {
    return new GridPoint(x, y);
  }

}