/**
 * Structures describing points in geospace.
 *
 * @license BSD-3-Clause
 */
package org.joshsim.engine.geometry;

import java.math.BigDecimal;

/** Interface representing a geographical point on Earth. */
public interface GeoPoint extends Geometry {
  /**
   * {@inheritDoc}
   *
   * @return the latitude of this point
   */
  @Override
  BigDecimal getCenterLatitude();

  /**
   * {@inheritDoc}
   *
   * @return the longitude of this point
   */
  @Override
  BigDecimal getCenterLongitude();

  /**
   * {@inheritDoc}
   *
   * @return zero for a point
   */
  @Override
  BigDecimal getRadius();
}
