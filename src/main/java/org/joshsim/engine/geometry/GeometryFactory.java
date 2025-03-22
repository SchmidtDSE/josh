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

  private GeometryFactory() {
  }
}
