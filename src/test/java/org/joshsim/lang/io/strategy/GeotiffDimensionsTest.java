
package org.joshsim.lang.io.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.junit.jupiter.api.Test;

class GeotiffDimensionsTest {

  @Test
  void constructorShouldCalculateGridDimensionsCorrectly() {
    // San Francisco to Los Angeles area
    BigDecimal topLeftX = new BigDecimal("-122.45"); // SF longitude
    BigDecimal topLeftY = new BigDecimal("37.73");   // SF latitude
    BigDecimal bottomRightX = new BigDecimal("-118.24"); // LA longitude
    BigDecimal bottomRightY = new BigDecimal("34.05");   // LA latitude

    PatchBuilderExtents extents = new PatchBuilderExtents(
        topLeftX, topLeftY, bottomRightX, bottomRightY
    );

    BigDecimal cellWidth = new BigDecimal("10000"); // 10km cells
    GeotiffDimensions dimensions = new GeotiffDimensions(extents, cellWidth);

    // Check calculated dimensions
    assertTrue(dimensions.getGridWidthPixels() > 0);
    assertTrue(dimensions.getGridHeightPixels() > 0);

    // Check stored values
    assertEquals(topLeftX.doubleValue(), dimensions.getMinLon());
    assertEquals(bottomRightX.doubleValue(), dimensions.getMaxLon());
    assertEquals(bottomRightY.doubleValue(), dimensions.getMinLat());
    assertEquals(topLeftY.doubleValue(), dimensions.getMaxLat());
    assertEquals(cellWidth.doubleValue(), dimensions.getPatchWidthInMeters());
  }
}
