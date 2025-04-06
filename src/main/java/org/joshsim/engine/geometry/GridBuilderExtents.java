
package org.joshsim.engine.geometry;

import java.math.BigDecimal;

public class GridBuilderExtents {
    private final BigDecimal topLeftX;
    private final BigDecimal topLeftY;
    private final BigDecimal bottomRightX;
    private final BigDecimal bottomRightY;

    public GridBuilderExtents(
            BigDecimal topLeftX,
            BigDecimal topLeftY,
            BigDecimal bottomRightX,
            BigDecimal bottomRightY) {
        this.topLeftX = topLeftX;
        this.topLeftY = topLeftY;
        this.bottomRightX = bottomRightX;
        this.bottomRightY = bottomRightY;
        validateCornerCoordinates();
    }

    public BigDecimal getTopLeftX() {
        return topLeftX;
    }

    public BigDecimal getTopLeftY() {
        return topLeftY;
    }

    public BigDecimal getBottomRightX() {
        return bottomRightX;
    }

    public BigDecimal getBottomRightY() {
        return bottomRightY;
    }

    /**
     * Validates corner coordinates based on coordinate system type.
     * For both geographic and projected coordinates, we expect Y to increase northward
     * and X to increase eastward.
     */
    public void validateCornerCoordinates() {
        if (topLeftX == null || topLeftY == null || bottomRightX == null || bottomRightY == null) {
            throw new IllegalArgumentException("Missing corner coordinates");
        }

        // Consistent validation for both geographic and projected coordinates
        // Y-coordinate (latitude/northing) should decrease from top to bottom
        if (topLeftY.compareTo(bottomRightY) <= 0) {
            throw new IllegalArgumentException(
                "Top-left Y-coordinate must be greater than bottom-right Y-coordinate");
        }

        // X-coordinate (longitude/easting) should increase from left to right
        if (topLeftX.compareTo(bottomRightX) >= 0) {
            throw new IllegalArgumentException(
                "Top-left X-coordinate must be less than bottom-right X-coordinate");
        }
    }
}
