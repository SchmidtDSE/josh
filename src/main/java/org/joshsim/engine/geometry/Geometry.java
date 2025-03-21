package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;

/**
 * Represents a geometric object that implements the Spatial interface.
 */
public class Geometry implements Spatial {
  
  protected SpatialContext spatialContext = SpatialContext.GEO;
  protected Shape shape;

  /**
   * Constructs a Geometry with a provided spatial4j shape.
   */
  public Geometry(Shape shape) {
    this.shape = shape;
  }

  /**
   * Gets the center point of this geometry, calculating from the shape.
   *
   * @return The center point
   */
  protected Point getCenter() {
    return shape.getCenter();
  }

  /**
   * Calculates the distance between this geometry's center and another point.
   *
   * @param other geometry to calculate distance to
   * @return Distance in degrees
   */
  public BigDecimal centerDistanceTo(Geometry other) {
    return new BigDecimal(spatialContext.calcDistance(shape.getCenter(), other.getCenter()));
  }

  /**
   * Checks if a point is contained within this geometry, regardless of shape type.
   *
   * @param latitude Latitude of the point
   * @param longitude Longitude of the point
   * @return true if the point is contained within the geometry
   */
  public boolean intersects(BigDecimal latitude, BigDecimal longitude) {
    if (shape == null) {
      throw new IllegalStateException("Shape not initialized");
    }
    
    Point point = spatialContext.getShapeFactory().pointXY(
        longitude.doubleValue(), 
        latitude.doubleValue()
    );
    
    return shape.relate(point).intersects();
  }

  /**
   * Checks if another Geometry is contained within this geometry.
   *
   * @param other Geometry to check
   * @return true if the other geometry is contained within this geometry
   */
  public boolean intersects(Geometry other) {
    if (shape == null) {
      throw new IllegalStateException("Shape not initialized");
    }
    
    return shape.relate(other.shape).intersects();
  }

  @Override
  public BigDecimal getCenterLatitude() {
    return BigDecimal.valueOf(getCenter().getY());
  }

  @Override
  public BigDecimal getCenterLongitude() {
    return BigDecimal.valueOf(getCenter().getX());
  }

}