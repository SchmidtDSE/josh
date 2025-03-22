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
   * Get the latitude component of this geometry's center point.
   *
   * @return the latitude as a Decimal value
   * @throws IllegalStateException if the geometry has no defined center
   */
  BigDecimal getCenterLatitude();

  /**
   * Get the longitude component of this geometry's center point.
   *
   * @return the longitude as a Decimal value
   * @throws IllegalStateException if the geometry has no defined center
   */
  BigDecimal getCenterLongitude();

  /**
   * Determines if this geometry intersects with another spatial geometry.
   *
   * @param other the other spatial geometry to check for intersection
   * @return true if the geometries intersect, false otherwise
   */
  public boolean intersects(Geometry other);

  /**
   * Determines if this geometry intersects with a specific geographic point.
   *
   * @param latitude the latitude of the point
   * @param longitude the longitude of the point
   * @return true if the geometry intersects with the point, false otherwise
   */
  public boolean intersects(BigDecimal latitude, BigDecimal longitude);
}
