/**
 * Structures describing geometric shapes and their properties.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import java.math.BigDecimal;


/**
 * Factory methods for creating geometric shapes using JTS geometry.
 */
public interface EngineGeometryFactory {

  /**
   * Create a square EngineGeometry with the specified width and center.
   *
   * @param width The width of the square
   * @param centerX The X position (longitude, easting) of the center
   * @param centerY The Y position (latitude, northing) of the center
   * @return A EngineGeometry object representing a square
   */
  EngineGeometry createSquare(
      BigDecimal width,
      BigDecimal centerX,
      BigDecimal centerY
  );

  /**
   * Create a square EngineGeometry from topLeft and bottomRight coordinates.
   *
   * @param topLeftX The X position (longitude, easting) of the top-left corner
   * @param topLeftY The Y position (latitude, northing) of the top-left corner
   * @param bottomRightX The X position (longitude, easting) of the bottom-right corner
   * @param bottomRightY The Y position (latitude, northing) of the bottom-right corner
   * @return A EngineGeometry object representing a square
   * @throws IllegalArgumentException if the coordinates don't form a square
   */
  EngineGeometry createSquare(
      BigDecimal topLeftX,
      BigDecimal topLeftY,
      BigDecimal bottomRightX,
      BigDecimal bottomRightY
  );

  /**
   * Create a circular EngineGeometry with the specified radius and center.
   *
   * @param radius The radius of the circle
   * @param centerX The X position (longitude, easting) of the center
   * @param centerY The Y position (latitude, northing) of the center
   * @return A EngineGeometry object representing a circle
   */
  EngineGeometry createCircle(
      BigDecimal radius,
      BigDecimal centerX,
      BigDecimal centerY
  );

  /**
   * Creates a circular EngineGeometry from radius and a point on the circumference.
   *
   * @param pointX The X position (longitude, easting) of a point on the circle's circumference
   * @param pointY The Y position (latitude, northing) of a point on the circle's circumference
   * @param centerX The X position (longitude, easting) of the circle's center
   * @param centerY The Y position (latitude, northing) of the circle's center
   * @return A EngineGeometry object representing a circle
   */
  EngineGeometry createCircle(
      BigDecimal pointX,
      BigDecimal pointY,
      BigDecimal centerX,
      BigDecimal centerY
  );

  /**
   * Creates a point EngineGeometry at the specified location.
   *
   * @param x The X position (longitude, easting)
   * @param y The Y position (latitude, northing)
   * @return A EngineGeometry object representing a point
   */
  EngineGeometry createPoint(
      BigDecimal x,
      BigDecimal y
  );

  /**
   * Description of how this geometry factory is building shapes.
   *
   * @return String with information like the CRS used.
   */
  String toString();

}
