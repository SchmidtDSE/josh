/**
 * A generic geometric object that implements the Spatial interface. Since we use
 * spatial4j, this class is a wrapper around spatial4j's Shape class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.sis.geometry.GeneralEnvelope;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;


/**
 * Represents a geometric object that implements the Spatial interface.
 */
public class Geometry implements Spatial {

  protected Shape shape;
  protected CoordinateReferenceSystem crs;
  protected Optional<Map<CoordinateReferenceSystem, MathTransform>> transformers;

  /**
   * Constructs a Geometry with a provided spatial4j shape, a coordinate reference system,
   * and an optional map of transformers to be used for mapping to other coordinate reference
   * systems, typically within external data sources.
   */
  public Geometry(
        Shape shape,
        CoordinateReferenceSystem crs,
        Optional<Map<CoordinateReferenceSystem, MathTransform>> transformers
  ) {
    this.shape = Objects.requireNonNull(shape, "Shape cannot be null");
    this.crs = Objects.requireNonNull(crs, "Coordinate reference system cannot be null");
    this.transformers = transformers;
  }

  /**
   * Constructs a Geometry with a provided spatial4j shape and a coordinate reference system,
   * without any transformers.
   */
  public Geometry(Shape shape, CoordinateReferenceSystem crs) {
    this(shape, crs, Optional.empty());
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
   * Gets the coordinate reference system of this geometry.
   *
   * @return The coordinate reference system
   */
  public CoordinateReferenceSystem getCrs() {
    return crs;
  }

  /**
   * Gets the area of this geometry.
   *
   * @return The area in square degrees
   */
  public Geometry asTargetCrs(CoordinateReferenceSystem targetCrs){
    if (!transformers.isPresent()) {
      throw new IllegalStateException("No transformers available for this geometry");
    }
    if (transformers.get().containsKey(targetCrs)) {
      MathTransform transform = transformers.get().get(targetCrs);
      try {
        Shape transformedShape = convertShapeToTargetCrs(transform);
        return new Geometry(transformedShape, targetCrs, Optional.empty());
      } catch (TransformException e) {
        throw new RuntimeException("Error transforming geometry to target CRS", e);
      }
    } else {
      throw new IllegalArgumentException("No transformer available for the target CRS");
    }
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

  /**
   * Returns the convex hull of this geometry and another geometry.
   *
   * @param other The other geometry to compute the convex hull with
   * @return A new Geometry representing the convex hull
   */
  public Geometry getConvexHull(Geometry other) {
    throw new UnsupportedOperationException("Convex hull not yet implemented");
  }

  /**
   * Converts the current shape to a shape in the target coordinate reference system.
   *
   * @param transform The mathematical transformation to apply
   * @return A new Shape in the target CRS
   * @throws TransformException If the coordinate transformation fails
   */
  protected Shape convertShapeToTargetCrs(MathTransform transform) throws TransformException {
    SpatialContext ctx = getSpatialContext();

    // Handle Point shapes
    if (this.shape instanceof Point) {
      Point point = (Point) this.shape;
      double[] src = new double[] {point.getX(), point.getY()};
      double[] dest = new double[2];
      transform.transform(src, 0, dest, 0, 1);
      return ctx.getShapeFactory().pointXY(dest[0], dest[1]);
    }
    // Handle Rectangle shapes
    else if (this.shape instanceof org.locationtech.spatial4j.shape.Rectangle) {
      org.locationtech.spatial4j.shape.Rectangle rect = (org.locationtech.spatial4j.shape.Rectangle) this.shape;

      // Transform the four corners of the rectangle
      double[][] corners = {
        {rect.getMinX(), rect.getMinY()},
        {rect.getMaxX(), rect.getMinY()},
        {rect.getMaxX(), rect.getMaxY()},
        {rect.getMinX(), rect.getMaxY()}
      };

      double[][] transformedCorners = new double[4][2];
      for (int i = 0; i < 4; i++) {
        double[] dest = new double[2];
        transform.transform(corners[i], 0, dest, 0, 1);
        transformedCorners[i] = dest;
      }

      // Find the min/max coordinates of the transformed rectangle
      double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
      double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

      for (double[] corner : transformedCorners) {
        minX = Math.min(minX, corner[0]);
        minY = Math.min(minY, corner[1]);
        maxX = Math.max(maxX, corner[0]);
        maxY = Math.max(maxY, corner[1]);
      }

      return ctx.getShapeFactory().rect(minX, maxX, minY, maxY);
    }
    // Handle Circle shapes
    else if (this.shape instanceof org.locationtech.spatial4j.shape.Circle) {
      org.locationtech.spatial4j.shape.Circle circle = (org.locationtech.spatial4j.shape.Circle) this.shape;

      // Transform the center point
      Point center = circle.getCenter();
      double[] src = new double[] {center.getX(), center.getY()};
      double[] dest = new double[2];
      transform.transform(src, 0, dest, 0, 1);

      // Note: This is a simplification as transformations may distort the circle
      return ctx.getShapeFactory().circle(dest[0], dest[1], circle.getRadius());
    }
    // For other shape types, fallback to bounding box transformation
    else {
      org.locationtech.spatial4j.shape.Rectangle bbox = shape.getBoundingBox();

      double[] minPoint = new double[] {bbox.getMinX(), bbox.getMinY()};
      double[] maxPoint = new double[] {bbox.getMaxX(), bbox.getMaxY()};

      double[] transformedMinPoint = new double[2];
      double[] transformedMaxPoint = new double[2];

      transform.transform(minPoint, 0, transformedMinPoint, 0, 1);
      transform.transform(maxPoint, 0, transformedMaxPoint, 0, 1);

      return ctx.getShapeFactory().rect(
        Math.min(transformedMinPoint[0], transformedMaxPoint[0]),
        Math.max(transformedMinPoint[0], transformedMaxPoint[0]),
        Math.min(transformedMinPoint[1], transformedMaxPoint[1]),
        Math.max(transformedMinPoint[1], transformedMaxPoint[1])
      );
    }
  }
}
