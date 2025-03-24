/**
 * Structures describing geometric shapes and their properties.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.ShapeFactory;

/**
 * Factory methods for creating geometric shapes.
 */
public class GeometryFactory {
  private static final ShapeFactory shapeFactory = SpatialContext.GEO.getShapeFactory();

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
    double minLon = topLeftLongitude.doubleValue();
    double maxLon = bottomRightLongitude.doubleValue();
    double maxLat = topLeftLatitude.doubleValue();
    double minLat = bottomRightLatitude.doubleValue();

    // Check if it's a square (approximately, within reasonable precision)
    double width = Math.abs(maxLon - minLon);
    double height = Math.abs(maxLat - minLat);

    if (Math.abs(width - height) > 0.000001) {
      throw new IllegalArgumentException(
        "The specified coordinates don't form a square: width="
            + width + ", height=" + height
      );
    }

    Rectangle rectangle = shapeFactory.rect(minLon, maxLon, minLat, maxLat);
    Geometry geometry = new Geometry(rectangle);
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
    double radiusVal = radius.doubleValue();
    double centerLat = centerLatitude.doubleValue();
    double centerLon = centerLongitude.doubleValue();

    org.locationtech.spatial4j.shape.Circle circle = shapeFactory.circle(centerLon, centerLat, radiusVal);
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
    double pLat = pointLatitude.doubleValue();
    double pLon = pointLongitude.doubleValue();
    double cLat = centerLatitude.doubleValue();
    double cLon = centerLongitude.doubleValue();

    // Calculate radius using distance between center and point
    double radius = Math.sqrt(Math.pow(pLat - cLat, 2) + Math.pow(pLon - cLon, 2));

    org.locationtech.spatial4j.shape.Circle circle = shapeFactory.circle(cLon, cLat, radius);
    Geometry geometry = new Geometry(circle);
    return geometry;
  }

  private GeometryFactory() {
  }
}
