/**
 * @license BSD-3-Clause
 */
package org.dse.JoshLang.core.geometry;

import java.math.Decimal;

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
    Decimal getCenterLatitude();

    /**
     * {@inheritDoc}
     *
     * @return the longitude of this circle's center
     */
    @Override
    Decimal getCenterLongitude();

    /**
     * {@inheritDoc}
     *
     * @return the radius of this circle
     */
    @Override
    Decimal getRadius();
}