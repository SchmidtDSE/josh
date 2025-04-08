/**
 * Structures describing geometric shapes and their properties.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.util.GeometricShapeFactory;

/**
 * Factory methods for creating geometric shapes using JTS geometry.
 */
public class EngineGeometryFactory {
  private static final GeometryFactory JTS_GEOMETRY_FACTORY = 
      new GeometryFactory();

  public static GeometryFactory getGeometryFactory() {
    return JTS_GEOMETRY_FACTORY;
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
    double halfWidth = width.doubleValue() / 2.0;
    double centerValX = centerX.doubleValue();
    double centerValY = centerY.doubleValue();

    double minX = centerValX - halfWidth;
    double maxX = centerValX + halfWidth;
    double minY = centerValY - halfWidth;
    double maxY = centerValY + halfWidth;

    Coordinate[] coords = new Coordinate[5];
    coords[0] = new Coordinate(minX, minY);
    coords[1] = new Coordinate(maxX, minY);
    coords[2] = new Coordinate(maxX, maxY);
    coords[3] = new Coordinate(minX, maxY);
    coords[4] = new Coordinate(minX, minY); // Close the ring

    LinearRing ring = JTS_GEOMETRY_FACTORY.createLinearRing(coords);
    Polygon square = JTS_GEOMETRY_FACTORY.createPolygon(ring, null);
    
    return new EngineGeometry(square, crs);
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

    Coordinate[] coords = new Coordinate[5];
    coords[0] = new Coordinate(minX, minY);
    coords[1] = new Coordinate(maxX, minY);
    coords[2] = new Coordinate(maxX, maxY);
    coords[3] = new Coordinate(minX, maxY);
    coords[4] = new Coordinate(minX, minY); // Close the ring

    LinearRing ring = JTS_GEOMETRY_FACTORY.createLinearRing(coords);
    Polygon square = JTS_GEOMETRY_FACTORY.createPolygon(ring, null);
    
    return new EngineGeometry(square, crs);
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
    double radiusVal = radius.doubleValue();
    double centerValX = centerX.doubleValue();
    double centerValY = centerY.doubleValue();

    GeometricShapeFactory shapeFactory = new GeometricShapeFactory(JTS_GEOMETRY_FACTORY);
    shapeFactory.setNumPoints(64); // Smooth circle approximation
    shapeFactory.setCentre(new Coordinate(centerValX, centerValY));
    shapeFactory.setSize(radiusVal * 2);
    Polygon circle = shapeFactory.createCircle();
    
    return new EngineGeometry(circle, crs);
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
    double pointValX = pointX.doubleValue();
    double pointValY = pointY.doubleValue();
    double centerValX = centerX.doubleValue();
    double centerValY = centerY.doubleValue();

    // Calculate radius using distance between center and point
    double radius = Math.sqrt(
        Math.pow(pointValX - centerValX, 2) + Math.pow(pointValY - centerValY, 2)
    );

    GeometricShapeFactory shapeFactory = new GeometricShapeFactory(JTS_GEOMETRY_FACTORY);
    shapeFactory.setNumPoints(64); // Smooth circle approximation
    shapeFactory.setCentre(new Coordinate(centerValX, centerValY));
    shapeFactory.setSize(radius * 2);
    Polygon circle = shapeFactory.createCircle();
    
    return new EngineGeometry(circle, crs);
  }

  /**
   * Creates a point EngineGeometry at the specified location.
   *
   * @param x The X position (longitude, easting)
   * @param y The Y position (latitude, northing)
   * @return A EngineGeometry object representing a point
   */
  public static EngineGeometry createPoint(
      BigDecimal x,
      BigDecimal y,
      CoordinateReferenceSystem crs
  ) {
    Point point = JTS_GEOMETRY_FACTORY.createPoint(
        new Coordinate(x.doubleValue(), y.doubleValue())
    );
    return new EngineGeometry(point, crs);
  }

  private EngineGeometryFactory() {}
}