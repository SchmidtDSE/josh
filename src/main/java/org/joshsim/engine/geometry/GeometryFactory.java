/**
 * Structures describing geometric shapes and their properties.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Circle;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.ShapeFactory;

/**
 * Factory methods for creating geometric shapes. By default, the shapes are
 * created using the WGS84 spatial context, but methods are provided to override
 * this behavior.
 */
public class GeometryFactory {
  private static final SpatialContext DEFAULT_CONTEXT = SpatialContext.GEO;

  public static SpatialContext getDefaultSpatialContext() {
    return DEFAULT_CONTEXT;
  }

  /**
   * Creates a square geometry with the specified width and center.
   *
   * @param width The width of the square
   * @param centerX The X position (longitude, easting) of the center
   * @param centerY The Y position (latitude, northing) of the center
   * @param spatialContext The context to use in creating the shape
   * @return A Geometry object representing a square
   */
  public static Geometry createSquare(
      BigDecimal width,
      BigDecimal centerX,
      BigDecimal centerY,
      SpatialContext spatialContext
  ) {

    ShapeFactory shapeFactory = spatialContext.getShapeFactory();
    double halfWidth = width.doubleValue() / 2.0;

    double minX = centerX.doubleValue() - halfWidth;
    double maxX = centerX.doubleValue() + halfWidth;
    double minY = centerY.doubleValue() - halfWidth;
    double maxY = centerY.doubleValue() + halfWidth;

    Rectangle rectangle = shapeFactory.rect(minX, maxX, minY, maxY);
    Geometry geometry = new Geometry(rectangle);
    return geometry;
  }

  /**
   * Creates a square geometry with the specified width and center.
   *
   * @param width The width of the square
   * @param centerX The X position (longitude, easting) of the center
   * @param centerY The Y position (latitude, northing) of the center
   * @return A Geometry object representing a square
   */
  public static Geometry createSquare(
      BigDecimal width,
      BigDecimal centerX,
      BigDecimal centerY
  ) {
    return createSquare(width, centerX, centerY, getDefaultSpatialContext());
  }

  /**
   * Creates a square geometry from topLeft and bottomRight coordinates.
   *
   * @param topLeftX The X position (longitude, easting) of the top-left corner
   * @param topLeftY The Y position (latitude, northing) of the top-left corner
   * @param bottomRightX The X position (longitude, easting) of the bottom-right corner
   * @param bottomRightY The Y position (latitude, northing) of the bottom-right corner
   * @param spatialContext The context to use in creating the shape
   * @return A Geometry object representing a square
   * @throws IllegalArgumentException if the coordinates don't form a square
   */
  public static Geometry createSquare(
      BigDecimal topLeftX,
      BigDecimal topLeftY,
      BigDecimal bottomRightX,
      BigDecimal bottomRightY,
      SpatialContext spatialContext
  ) {
    ShapeFactory shapeFactory = spatialContext.getShapeFactory();

    double minX = topLeftX.doubleValue();
    double maxX = bottomRightX.doubleValue();
    double maxY = topLeftY.doubleValue();
    double minY = bottomRightY.doubleValue();

    // Check if it's a square (approximately, within reasonable precision)
    double reasonableSquarePctDiff = .01;
    double width = Math.abs(maxX - minX);
    double height = Math.abs(maxY - minY);
    if ((1 - (width / height)) > reasonableSquarePctDiff) {
      return null;
    }

    Rectangle rectangle = shapeFactory.rect(minX, maxX, minY, maxY);
    Geometry geometry = new Geometry(rectangle);
    return geometry;
  }

  /**
   * Creates a square geometry from topLeft and bottomRight coordinates.
   *
   * @param topLeftX The X position (longitude, easting) of the top-left corner
   * @param topLeftY The Y position (latitude, northing) of the top-left corner
   * @param bottomRightX The X position (longitude, easting) of the bottom-right corner
   * @param bottomRightY The Y position (latitude, northing) of the bottom-right corner
   * @return A Geometry object representing a square
   * @throws IllegalArgumentException if the coordinates don't form a square
   */
  public static Geometry createSquare(
      BigDecimal topLeftX,
      BigDecimal topLeftY,
      BigDecimal bottomRightX,
      BigDecimal bottomRightY
  ) {
    return createSquare(
        topLeftX,
        topLeftY,
        bottomRightX,
        bottomRightY,
        getDefaultSpatialContext()
    );
  }

  /**
   * Creates a circular geometry with the specified radius and center.
   *
   * @param radius The radius of the circle
   * @param centerX The X position (longitude, easting) of the center
   * @param centerY The Y position (latitude, northing) of the center
   * @param spatialContext The context to use in creating the shape
   * @return A Geometry object representing a circle
   */
  public static Geometry createCircle(
      BigDecimal radius,
      BigDecimal centerX,
      BigDecimal centerY,
      SpatialContext spatialContext
  ) {
    double radiusVal = radius.doubleValue();
    double centerValX = centerX.doubleValue();
    double centerValY = centerY.doubleValue();

    ShapeFactory shapeFactory = spatialContext.getShapeFactory();

    Circle circle = shapeFactory.circle(centerValX, centerValY, radiusVal);
    Geometry geometry = new Geometry(circle);
    return geometry;
  }

  /**
   * Creates a circular geometry with the specified radius and center.
   *
   * @param radius The radius of the circle
   * @param centerX The X position (longitude, easting) of the center
   * @param centerY The Y position (latitude, northing) of the center
   * @return A Geometry object representing a circle
   */
  public static Geometry createCircle(
      BigDecimal radius,
      BigDecimal centerX,
      BigDecimal centerY
  ) {
    return createCircle(radius, centerX, centerY, getDefaultSpatialContext());
  }

  /**
   * Creates a circular geometry from radius and a point on the circumference.
   *
   * @param pointX The X position (longitude, easting) of a point on the circle's circumference
   * @param pointY The Y position (latitude, northing) of a point on the circle's circumference
   * @param centerX The X position (longitude, easting) of the circle's center
   * @param centerY The Y position (latitude, northing) of the circle's center
   * @param spatialContext The context to use in creating the shape
   * @return A Geometry object representing a circle
   */
  public static Geometry createCircle(
      BigDecimal pointX,
      BigDecimal pointY,
      BigDecimal centerX,
      BigDecimal centerY,
      SpatialContext spatialContext
  ) {
    double pointValX = pointX.doubleValue();
    double pointValY = pointY.doubleValue();
    double centerValX = centerX.doubleValue();
    double centerValY = centerY.doubleValue();

    // Calculate radius using distance between center and point
    double radius = Math.sqrt(
        Math.pow(pointValX - centerValX, 2) + Math.pow(pointValY - centerValY, 2)
    );

    ShapeFactory shapeFactory = spatialContext.getShapeFactory();

    Circle circle = shapeFactory.circle(centerValX, centerValY, radius);
    Geometry geometry = new Geometry(circle);
    return geometry;
  }

  /**
   * Creates a circular geometry from radius and a point on the circumference.
   *
   * @param pointX The X position (longitude, easting) of a point on the circle's circumference
   * @param pointY The Y position (latitude, northing) of a point on the circle's circumference
   * @param centerX The X position (longitude, easting) of the circle's center
   * @param centerY The Y position (latitude, northing) of the circle's center
   * @return A Geometry object representing a circle
   */
  public static Geometry createCircle(
      BigDecimal pointX,
      BigDecimal pointY,
      BigDecimal centerX,
      BigDecimal centerY
  ) {
    return createCircle(
        pointX,
        pointY,
        centerX,
        centerY,
        getDefaultSpatialContext()
    );
  }

  private GeometryFactory() {}
}