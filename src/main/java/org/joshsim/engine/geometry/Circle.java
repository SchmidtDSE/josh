/**
 * @license BSD-3-Clause
 */
package org.joshsim.engine.geometry;

import java.math.BigDecimal;

/**
 * Interface representing a circle in geographic coordinates.
 * A circle is defined by a center point and a radius.
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