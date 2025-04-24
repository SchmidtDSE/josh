/**
 * Structures describing geometric shapes and their properties.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;


/**
 * Factory methods for creating geometric shapes using JTS geometry.
 */
public interface EngineGeometryFactory {

  /**
   * Create a square EngineGeometry with the specified width and center.
   *
   * @param centerX The X position (longitude, easting) of the center
   * @param centerY The Y position (latitude, northing) of the center
   * @param width   The width of the square
   * @return A EngineGeometry object representing a square
   */
  EngineGeometry createSquare(
      BigDecimal centerX,
      BigDecimal centerY,
      BigDecimal width
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
   * @param centerX The X position (longitude, easting) of the center
   * @param centerY The Y position (latitude, northing) of the center
   * @param radius  The radius of the circle
   * @return A EngineGeometry object representing a circle
   */
  EngineGeometry createCircle(
      BigDecimal centerX,
      BigDecimal centerY,
      BigDecimal radius
  );

  /**
   * Creates a circular EngineGeometry from radius and a point on the circumference.
   *
   * @param point1X The X position (longitude, easting) of a point on the circle's circumference
   * @param point1Y The Y position (latitude, northing) of a point on the circle's circumference
   * @param point2X The X position (longitude, easting) of the circle's center
   * @param point2Y The Y position (latitude, northing) of the circle's center
   * @return A EngineGeometry object representing a circle
   */
  EngineGeometry createCircle(
      BigDecimal point1X,
      BigDecimal point1Y,
      BigDecimal point2X,
      BigDecimal point2Y
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

  /**
   * Create a new PatchBuilder using a GridCrsDefinition.
   *
   * <p>Creates a new instance of a PatchBuilder for creating grid structures
   * based on the provided grid coordinate reference system definition.</p>
   *
   * @param gridCrsDefinition The grid CRS definition containing extents, cell size,
   *                         and reference system information
   * @param prototype The prototype through which to build patches representing cells
   * @return A PatchBuilder instance configured for the specified grid CRS
   */
  PatchBuilder getPatchBuilder(
      GridCrsDefinition gridCrsDefinition,
      EntityPrototype prototype
  );
}
