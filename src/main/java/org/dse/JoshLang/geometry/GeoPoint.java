/**
 * @license BSD-3-Clause
 */
package org.dse.JoshLang.core.geometry;

import java.math.Decimal;

/**
 * Interface representing a geographical point on Earth.
 * A point is a zero-dimensional geometry with a specific location.
 */
public interface GeoPoint extends Geometry {
    /**
     * {@inheritDoc}
     *
     * @return the latitude of this point
     */
    @Override
    Decimal getCenterLatitude();

    /**
     * {@inheritDoc}
     *
     * @return the longitude of this point
     */
    @Override
    Decimal getCenterLongitude();

    /**
     * {@inheritDoc}
     * 
     * @return zero for a point
     */
    @Override
    Decimal getRadius();
}