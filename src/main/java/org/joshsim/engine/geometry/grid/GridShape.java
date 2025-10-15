/**
 * Base implementation of geometric shapes in grid space.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry.grid;

import java.math.BigDecimal;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.EnginePoint;
import org.joshsim.geo.geometry.EarthShape;


/**
 * Abstract base class for all geometric shapes in grid space.
 */
public abstract class GridShape implements EngineGeometry {

  public static final BigDecimal DISTANCE_TOLLERANCE = BigDecimal.valueOf(0.00001);

  private final BigDecimal centerX;
  private final BigDecimal centerY;
  private Integer cachedHashCode;

  /**
   * Creates a new grid shape with specified center coordinates.
   *
   * @param centerX The x-coordinate of the shape's center point
   * @param centerY The y-coordinate of the shape's center point
   */
  public GridShape(BigDecimal centerX, BigDecimal centerY) {
    this.centerX = centerX;
    this.centerY = centerY;
  }

  @Override
  public EnginePoint getCenter() {
    return new GridPoint(centerX, centerY);
  }

  @Override
  public EarthShape getOnEarth() {
    throw new UnsupportedOperationException(
        "Conversion to Earth from PatchSet space reserved for future use."
    );
  }

  @Override
  public GridShape getOnGrid() {
    return this;
  }

  @Override
  public BigDecimal getCenterX() {
    return centerX;
  }

  @Override
  public BigDecimal getCenterY() {
    return centerY;
  }

  @Override
  public boolean intersects(EngineGeometry other) {
    return IntersectionDetector.intersect(this, other.getOnGrid());
  }

  @Override
  public boolean intersects(BigDecimal locationX, BigDecimal locationY) {
    return intersects(new GridPoint(locationX, locationY));
  }

  /**
   * Retrieves the type of the grid shape.
   *
   * @return The GridShapeType representing the type of the grid shape, such as POINT, SQUARE, or
   *     CIRCLE.
   */
  public abstract GridShapeType getGridShapeType();

  /**
   * Retrieves the width of the grid shape.
   *
   * @return A BigDecimal representing the width of the grid shape.
   */
  public abstract BigDecimal getWidth();

  /**
   * Retrieves the height of the grid shape.
   *
   * @return A BigDecimal representing the height of the grid shape.
   */
  public abstract BigDecimal getHeight();

  @Override
  public boolean equals(Object other) {
    if (other instanceof GridShape) {
      GridShape otherCast = (GridShape) other;
      boolean matchesX = getCenterX().subtract(otherCast.getCenterX())
          .abs()
          .compareTo(DISTANCE_TOLLERANCE) < 0;

      boolean matchesY = getCenterY().subtract(otherCast.getCenterY())
          .abs()
          .compareTo(DISTANCE_TOLLERANCE) < 0;

      boolean widthMatches = getWidth().subtract(otherCast.getWidth())
          .abs()
          .compareTo(DISTANCE_TOLLERANCE) < 0;

      return matchesX && matchesY && widthMatches;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    if (cachedHashCode == null) {
      // Compute hashCode directly from numeric values instead of string representation
      // This avoids expensive String.format and BigDecimal.toString calls
      int result = 17;
      result = 31 * result + centerX.hashCode();
      result = 31 * result + centerY.hashCode();
      result = 31 * result + getWidth().hashCode();
      cachedHashCode = result;
    }
    return cachedHashCode;
  }

}
