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
import org.locationtech.spatial4j.shape.Shape;
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
   * @param centerLatitude The center latitude
   * @param centerLongitude The center longitude
   * @param spatialContext The context to use in creating the shape
   * @return A Geometry object representing a square
   */
  public static Geometry createSquare(
      BigDecimal width,
      BigDecimal centerLatitude,
      BigDecimal centerLongitude,
      SpatialContext spatialContext
  ) {

    ShapeFactory shapeFactory = spatialContext.getShapeFactory();

    double halfWidth = width.doubleValue() / 2.0;
    double centerLat = centerLatitude.doubleValue();
    double centerLon = centerLongitude.doubleValue();

    double minLon = centerLon - halfWidth;
    double maxLon = centerLon + halfWidth;
    double minLat = centerLat - halfWidth;
    double maxLat = centerLat + halfWidth;

    Rectangle rectangle = shapeFactory.rect(minLon, maxLon, minLat, maxLat);
    Geometry geometry = new Geometry(rectangle);
    return geometry;
  }

  /**
   * Creates a square geometry with the specified width and center.
   *
   * @param width The width of the square
   * @param centerLatitude The center latitude
   * @param centerLongitude The center longitude
   * @return A Geometry object representing a square
   */
  public static Geometry createSquare(
      BigDecimal width,
      BigDecimal centerLatitude,
      BigDecimal centerLongitude
  ) {
    return createSquare(width, centerLatitude, centerLongitude, getDefaultSpatialContext());
  }

  /**
   * Creates a square geometry from topLeft and bottomRight coordinates.
   *
   * @param topLeftLatitude The latitude of the top-left corner
   * @param topLeftLongitude The longitude of the top-left corner
   * @param bottomRightLatitude The latitude of the bottom-right corner
   * @param bottomRightLongitude The longitude of the bottom-right corner
   * @param spatialContext The context to use in creating the shape
   * @return A Geometry object representing a square
   * @throws IllegalArgumentException if the coordinates don't form a square
   */
  public static Geometry createSquare(
      BigDecimal topLeftLatitude,
      BigDecimal topLeftLongitude,
      BigDecimal bottomRightLatitude,
      BigDecimal bottomRightLongitude,
      SpatialContext spatialContext
  ) {
    ShapeFactory shapeFactory = spatialContext.getShapeFactory();

    double minLon = topLeftLongitude.doubleValue();
    double maxLon = bottomRightLongitude.doubleValue();
    double maxLat = topLeftLatitude.doubleValue();
    double minLat = bottomRightLatitude.doubleValue();

    // Check if it's a square (approximately, within reasonable precision)
    double reasonableSquarePctDiff = .01;
    double width = Math.abs(maxLon - minLon);
    double height = Math.abs(maxLat - minLat);
    if ((1 - (width / height)) > reasonableSquarePctDiff) {
      return null;
    }

    Rectangle rectangle = shapeFactory.rect(minLon, maxLon, minLat, maxLat);
    Geometry geometry = new Geometry(rectangle);
    return geometry;
  }

  /**
   * Creates a square geometry from topLeft and bottomRight coordinates.
   *
   * @param topLeftLatitude The latitude of the top-left corner
   * @param topLeftLongitude The longitude of the top-left corner
   * @param bottomRightLatitude The latitude of the bottom-right corner
   * @param bottomRightLongitude The longitude of the bottom-right corner
   * @return A Geometry object representing a square
   * @throws IllegalArgumentException if the coordinates don't form a square
   */
  public static Geometry createSquare(
      BigDecimal topLeftLatitude,
      BigDecimal topLeftLongitude,
      BigDecimal bottomRightLatitude,
      BigDecimal bottomRightLongitude
  ) {
    return createSquare(
        topLeftLatitude,
        topLeftLongitude,
        bottomRightLatitude,
        bottomRightLongitude,
        getDefaultSpatialContext()
    );
  }

  /**
   * Creates a circular geometry with the specified radius and center.
   *
   * @param radius The radius of the circle
   * @param centerLatitude The center latitude
   * @param centerLongitude The center longitude
   * @param spatialContext The context to use in creating the shape
   * @return A Geometry object representing a circle
   */
  public static Geometry createCircle(
      BigDecimal radius,
      BigDecimal centerLatitude,
      BigDecimal centerLongitude,
      SpatialContext spatialContext
  ) {
    double radiusVal = radius.doubleValue();
    double centerLat = centerLatitude.doubleValue();
    double centerLon = centerLongitude.doubleValue();

    ShapeFactory shapeFactory = spatialContext.getShapeFactory();

    Circle circle = shapeFactory.circle(centerLon, centerLat, radiusVal);
    Geometry geometry = new Geometry(circle);
    return geometry;
  }

  /**
   * Creates a circular geometry with the specified radius and center.
   *
   * @param radius The radius of the circle
   * @param centerLatitude The center latitude
   * @param centerLongitude The center longitude
   * @return A Geometry object representing a circle
   */
  public static Geometry createCircle(
      BigDecimal radius,
      BigDecimal centerLatitude,
      BigDecimal centerLongitude
  ) {
    return createCircle(radius, centerLatitude, centerLongitude, getDefaultSpatialContext());
  }

  /**
   * Creates a circular geometry from radius and a point on the circumference.
   *
   * @param pointLatitude The latitude of a point on the circle's circumference
   * @param pointLongitude The longitude of a point on the circle's circumference
   * @param centerLatitude The latitude of the circle's center
   * @param centerLongitude The longitude of the circle's center
   * @param spatialContext The context to use in creating the shape
   * @return A Geometry object representing a circle
   * @throws IllegalArgumentException if the coordinates are invalid
   */
  public static Geometry createCircle(
      BigDecimal pointLatitude,
      BigDecimal pointLongitude,
      BigDecimal centerLatitude,
      BigDecimal centerLongitude,
      SpatialContext spatialContext
  ) {
    double pointLat = pointLatitude.doubleValue();
    double pointLon = pointLongitude.doubleValue();
    double centerLat = centerLatitude.doubleValue();
    double centerLon = centerLongitude.doubleValue();

    // Calculate radius using distance between center and point
    double radius = Math.sqrt(
        Math.pow(pointLat - centerLat, 2) + Math.pow(pointLon - centerLon, 2)
    );

    ShapeFactory shapeFactory = spatialContext.getShapeFactory();

    Circle circle = shapeFactory.circle(centerLon, centerLat, radius);
    Geometry geometry = new Geometry(circle);
    return geometry;
  }

  /**
   * Creates a circular geometry from radius and a point on the circumference.
   *
   * @param pointLatitude The latitude of a point on the circle's circumference
   * @param pointLongitude The longitude of a point on the circle's circumference
   * @param centerLatitude The latitude of the circle's center
   * @param centerLongitude The longitude of the circle's center
   * @return A Geometry object representing a circle
   * @throws IllegalArgumentException if the coordinates are invalid
   */
  public static Geometry createCircle(
      BigDecimal pointLatitude,
      BigDecimal pointLongitude,
      BigDecimal centerLatitude,
      BigDecimal centerLongitude
  ) {
    return createCircle(
        pointLatitude,
        pointLongitude,
        centerLatitude,
        centerLongitude,
        getDefaultSpatialContext()
    );
  }

  /**
   * Creates a point geometry with the specified center.
   *
   * @param centerLatitude The center latitude
   * @param centerLongitude The center longitude
   * @param spatialContext The context to use in creating the shape
   * @return A Geometry object representing a point
   */
  public static Geometry createPoint(
          BigDecimal centerLatitude,
          BigDecimal centerLongitude,
          SpatialContext spatialContext
  ) {
    double centerLat = centerLatitude.doubleValue();
    double centerLon = centerLongitude.doubleValue();

    ShapeFactory shapeFactory = spatialContext.getShapeFactory();

    Shape point = shapeFactory.pointLatLon(centerLat, centerLon);
    Geometry geometry = new Geometry(point);
    return geometry;
  }

  /**
   * Creates a circular geometry with the specified radius and center.
   *
   * @param centerLatitude The center latitude
   * @param centerLongitude The center longitude
   * @return A Geometry object representing a circle
   */
  public static Geometry createPoint(
          BigDecimal centerLatitude,
          BigDecimal centerLongitude
  ) {
    return createPoint(centerLatitude, centerLongitude, getDefaultSpatialContext());
  }

  private GeometryFactory() {}
}
