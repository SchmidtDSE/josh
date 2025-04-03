/**
 * A generic geometric object that implements the Spatial interface. Since we use
 * spatial4j, this class is a wrapper around spatial4j's Shape class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import org.apache.sis.geometry.GeneralEnvelope;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;

/**
 * Represents a geometric object that implements the Spatial interface.
 */
public class Geometry implements Spatial {

  protected Shape shape;

  /**
   * Constructs a Geometry with a provided spatial4j shape.
   */
  public Geometry(Shape shape) {
    this.shape = shape;
  }

  /**
   * Gets the shape of this geometry.
   */
  public Shape getShape() {
    return shape;
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
    return new BigDecimal(getSpatialContext().calcDistance(shape.getCenter(), other.getCenter()));
  }

  /**
   * Checks if a point is contained within this geometry, regardless of shape type.
   *
   * @param locationX X position of the point (Longitude / Easting)
   * @param locationY Y position of the point (Latitude / Northing)
   * @return true if the point is contained within the geometry
   */
  public boolean intersects(BigDecimal locationX, BigDecimal locationY) {
    if (shape == null) {
      throw new IllegalStateException("Shape not initialized");
    }

    Point point = getSpatialContext().getShapeFactory().pointXY(
        locationX.doubleValue(),
        locationY.doubleValue()
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

  /**
   * Returns a new geometry that is the intersection of this geometry and another.
   *
   * @param other Geometry to intersect with
   */
  public Geometry getIntersect(Geometry other) {
    throw new UnsupportedOperationException("Intersection not supported for this geometry type");
  }

  @Override
  public BigDecimal getCenterX() {
    return BigDecimal.valueOf(getCenter().getX());
  }

  @Override
  public BigDecimal getCenterY() {
    return BigDecimal.valueOf(getCenter().getY());
  }

  /**
   * Get the spatial context of this geometry.
   *
   * @return Spatial context which should be used for any geometries derived from this one.
   */
  public SpatialContext getSpatialContext() {
    return shape.getContext();
  }

  /**
   * Creates an envelope from this geometry's bounds.
   *
   * @return A GeneralEnvelope representing the geometry's bounds
   */
  public GeneralEnvelope getEnvelope() {
    // Extract bounds from the geometry
    GeneralEnvelope envelope = new GeneralEnvelope(2);

    double minX = getShape().getBoundingBox().getMinX();
    double minY = getShape().getBoundingBox().getMinY();
    double maxX = getShape().getBoundingBox().getMaxX();
    double maxY = getShape().getBoundingBox().getMaxY();

    envelope.setRange(0, minX, maxX);  // X min/max
    envelope.setRange(1, minY, maxY);  // Y min/max

    return envelope;
  }

}
