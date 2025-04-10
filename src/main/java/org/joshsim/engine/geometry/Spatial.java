/**
 * Structures describing a geospatial geometry.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import java.math.BigDecimal;

/**
 * Geometric object with geographic properties.
 */
public interface Spatial {
  /**
   * Get the Y component of this geometry's center point.
   *
   * @return the Y component as a Decimal value
   * @throws IllegalStateException if the geometry has no defined center
   */
  BigDecimal getCenterY();

  /**
   * Get the X component of this geometry's center point.
   *
   * @return the X component as a Decimal value
   * @throws IllegalStateException if the geometry has no defined center
   */
  BigDecimal getCenterX();

  /**
   * Determines if this geometry intersects with another spatial geometry.
   *
   * @param other the other spatial geometry to check for intersection
   * @return true if the geometries intersect, false otherwise
   */
  boolean intersects(EngineGeometry other);

  /**
   * Determines if this geometry intersects with a specific geographic point.
   *
   * @param locationX the X position (Longitude / Easting) of the point
   * @param locationY the Y position (Latitude / Northing) of the point
   * @return true if the geometry intersects with the point, false otherwise
   */
  boolean intersects(BigDecimal locationX, BigDecimal locationY);

}
