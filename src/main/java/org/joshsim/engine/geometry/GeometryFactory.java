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
import org.opengis.referencing.crs.CoordinateReferenceSystem;

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
   * Creates a square EngineGeometry with the specified width and center.
   *
   * @param width The width of the square
   * @param centerX The X position (longitude, easting) of the center
   * @param centerY The Y position (latitude, northing) of the center
   * @param spatialContext The context to use in creating the shape
   * @return A EngineGeometry object representing a square
   */
  public static EngineGeometry createSquare(
      BigDecimal width,
      BigDecimal centerX,
      BigDecimal centerY,
      SpatialContext spatialContext,
      CoordinateReferenceSystem crs
  ) {

    ShapeFactory shapeFactory = spatialContext.getShapeFactory();
    double halfWidth = width.doubleValue() / 2.0;

    double minX = centerX.doubleValue() - halfWidth;
    double maxX = centerX.doubleValue() + halfWidth;
    double minY = centerY.doubleValue() - halfWidth;
    double maxY = centerY.doubleValue() + halfWidth;

    Rectangle rectangle = shapeFactory.rect(minX, maxX, minY, maxY);
    EngineGeometry engineGeometry = new EngineGeometry(rectangle, crs);
    return engineGeometry;
  }

  /**
   * Creates a square EngineGeometry with the specified width and center.
   *
   * @param width The width of the square
   * @param centerX The X position (longitude, easting) of the center
   * @param centerY The Y position (latitude, northing) of the center
   * @return A EngineGeometry object representing a square
   */
  public static EngineGeometry createSquare(
      BigDecimal width,
      BigDecimal centerX,
      BigDecimal centerY,
      CoordinateReferenceSystem crs
  ) {
    return createSquare(width, centerX, centerY, getDefaultSpatialContext(), crs);
  }

  /**
   * Creates a square EngineGeometry from topLeft and bottomRight coordinates.
   *
   * @param topLeftX The X position (longitude, easting) of the top-left corner
   * @param topLeftY The Y position (latitude, northing) of the top-left corner
   * @param bottomRightX The X position (longitude, easting) of the bottom-right corner
   * @param bottomRightY The Y position (latitude, northing) of the bottom-right corner
   * @param spatialContext The context to use in creating the shape
   * @return A EngineGeometry object representing a square
   * @throws IllegalArgumentException if the coordinates don't form a square
   */
  public static EngineGeometry createSquare(
      BigDecimal topLeftX,
      BigDecimal topLeftY,
      BigDecimal bottomRightX,
      BigDecimal bottomRightY,
      SpatialContext spatialContext,
      CoordinateReferenceSystem crs
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
    EngineGeometry engineGeometry = new EngineGeometry(rectangle, crs);
    return engineGeometry;
  }

  /**
   * Creates a square EngineGeometry from topLeft and bottomRight coordinates.
   *
   * @param topLeftX The X position (longitude, easting) of the top-left corner
   * @param topLeftY The Y position (latitude, northing) of the top-left corner
   * @param bottomRightX The X position (longitude, easting) of the bottom-right corner
   * @param bottomRightY The Y position (latitude, northing) of the bottom-right corner
   * @return A EngineGeometry object representing a square
   * @throws IllegalArgumentException if the coordinates don't form a square
   */
  public static EngineGeometry createSquare(
      BigDecimal topLeftX,
      BigDecimal topLeftY,
      BigDecimal bottomRightX,
      BigDecimal bottomRightY,
      CoordinateReferenceSystem crs
  ) {
    return createSquare(
        topLeftX,
        topLeftY,
        bottomRightX,
        bottomRightY,
        getDefaultSpatialContext(),
        crs
    );
  }

  /**
   * Creates a circular EngineGeometry with the specified radius and center.
   *
   * @param radius The radius of the circle
   * @param centerX The X position (longitude, easting) of the center
   * @param centerY The Y position (latitude, northing) of the center
   * @param spatialContext The context to use in creating the shape
   * @return A EngineGeometry object representing a circle
   */
  public static EngineGeometry createCircle(
      BigDecimal radius,
      BigDecimal centerX,
      BigDecimal centerY,
      SpatialContext spatialContext,
      CoordinateReferenceSystem crs
  ) {
    double radiusVal = radius.doubleValue();
    double centerValX = centerX.doubleValue();
    double centerValY = centerY.doubleValue();

    ShapeFactory shapeFactory = spatialContext.getShapeFactory();

    Circle circle = shapeFactory.circle(centerValX, centerValY, radiusVal);
    EngineGeometry engineGeometry = new EngineGeometry(circle, crs);
    return engineGeometry;
  }

  /**
   * Creates a circular EngineGeometry with the specified radius and center.
   *
   * @param radius The radius of the circle
   * @param centerX The X position (longitude, easting) of the center
   * @param centerY The Y position (latitude, northing) of the center
   * @return A EngineGeometry object representing a circle
   */
  public static EngineGeometry createCircle(
      BigDecimal radius,
      BigDecimal centerX,
      BigDecimal centerY,
      CoordinateReferenceSystem crs
  ) {
    return createCircle(radius, centerX, centerY, getDefaultSpatialContext(), crs);
  }

  /**
   * Creates a circular EngineGeometry from radius and a point on the circumference.
   *
   * @param pointX The X position (longitude, easting) of a point on the circle's circumference
   * @param pointY The Y position (latitude, northing) of a point on the circle's circumference
   * @param centerX The X position (longitude, easting) of the circle's center
   * @param centerY The Y position (latitude, northing) of the circle's center
   * @param spatialContext The context to use in creating the shape
   * @return A EngineGeometry object representing a circle
   */
  public static EngineGeometry createCircle(
      BigDecimal pointX,
      BigDecimal pointY,
      BigDecimal centerX,
      BigDecimal centerY,
      SpatialContext spatialContext,
      CoordinateReferenceSystem crs
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
    EngineGeometry engineGeometry = new EngineGeometry(circle, crs);
    return engineGeometry;
  }

  /**
   * Creates a circular EngineGeometry from radius and a point on the circumference.
   *
   * @param pointX The X position (longitude, easting) of a point on the circle's circumference
   * @param pointY The Y position (latitude, northing) of a point on the circle's circumference
   * @param centerX The X position (longitude, easting) of the circle's center
   * @param centerY The Y position (latitude, northing) of the circle's center
   * @return A EngineGeometry object representing a circle
   */
  public static EngineGeometry createCircle(
      BigDecimal pointX,
      BigDecimal pointY,
      BigDecimal centerX,
      BigDecimal centerY,
      CoordinateReferenceSystem crs
  ) {
    return createCircle(
        pointX,
        pointY,
        centerX,
        centerY,
        getDefaultSpatialContext(),
        crs
    );
  }

  /**
   * Creates a point EngineGeometry with the specified center.
   *
   * @param centerLatitude The center latitude
   * @param centerLongitude The center longitude
   * @param spatialContext The context to use in creating the shape
   * @return A EngineGeometry object representing a point
   */
  public static EngineGeometry createPoint(
          BigDecimal centerLatitude,
          BigDecimal centerLongitude,
          SpatialContext spatialContext,
          CoordinateReferenceSystem crs
  ) {
    double centerLat = centerLatitude.doubleValue();
    double centerLon = centerLongitude.doubleValue();

    ShapeFactory shapeFactory = spatialContext.getShapeFactory();

    Shape point = shapeFactory.pointLatLon(centerLat, centerLon);
    EngineGeometry engineGeometry = new Geometry(point, crs);
    return engineGeometry;
  }

  /**
   * Creates a circular EngineGeometry with the specified radius and center.
   *
   * @param centerLatitude The center latitude
   * @param centerLongitude The center longitude
   * @return A EngineGeometry object representing a circle
   */
  public static EngineGeometry createPoint(
          BigDecimal centerLatitude,
          BigDecimal centerLongitude,
          CoordinateReferenceSystem crs
  ) {
    return createPoint(centerLatitude, centerLongitude, getDefaultSpatialContext(), crs);
  }

  private GeometryFactory() {}
}
