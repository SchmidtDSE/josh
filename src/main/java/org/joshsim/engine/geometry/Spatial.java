/**
 * Structures describing a geospatial geometry.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import java.math.BigDecimal;

/**
 * Geometric object with geographic properties which may be in Earth or grid space.
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

}
