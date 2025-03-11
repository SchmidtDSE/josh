/**
 * @license BSD-3-Clause
 */
package org.dse.JoshLang.core.geometry;

import java.math.BigDecimal;

/**
 * Interface representing geometric objects with geographic properties.
 */
public interface Geometry {
    /**
     * Gets the latitude component of this geometry's center point.
     *
     * @return the latitude as a Decimal value
     * @throws IllegalStateException if the geometry has no defined center
     */
    BigDecimal getCenterLatitude();

    /**
     * Gets the longitude component of this geometry's center point.
     *
     * @return the longitude as a Decimal value
     * @throws IllegalStateException if the geometry has no defined center
     */
    BigDecimal getCenterLongitude();

    /**
     * Gets the radius of this geometry.
     *
     * @return the radius as a Decimal value
     * @throws UnsupportedOperationException if the geometry has no defined radius
     */
    BigDecimal getRadius();
}