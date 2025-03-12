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
public interface Geometry {
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
   * Get the radius of this geometry.
   *
   * @return the radius as a Decimal value
   * @throws UnsupportedOperationException if the geometry has no defined radius
   */
  BigDecimal getRadius();
}