/**
 * Structures describing a circle geometry.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import java.math.BigDecimal;

/**
 * Geospatial circle described in geographic coordinates.
 */
public interface Circle extends Geometry {
  /**
   * {@inheritDoc}
   *
   * @return the latitude of this circle's center
   */
  @Override
  BigDecimal getCenterLatitude();

  /**
   * {@inheritDoc}
   *
   * @return the longitude of this circle's center
   */
  @Override
  BigDecimal getCenterLongitude();

  /**
   * {@inheritDoc}
   *
   * @return the radius of this circle
   */
  @Override
  BigDecimal getRadius();
}