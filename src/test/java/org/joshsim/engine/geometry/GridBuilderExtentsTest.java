/**
 * Tests for the PatchBuilderExtents structure.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


/**
 * Tests for the PatchBuilderExtents structure.
 */
class GridBuilderExtentsTest {

  @Test
  @DisplayName("Constructor should succeed with valid coordinates")
  void constructorSucceedsWithValidCoordinates() {
    // Arrange
    BigDecimal topLeftX = new BigDecimal("-115.55");
    BigDecimal topLeftY = new BigDecimal("33.55");
    BigDecimal bottomRightX = new BigDecimal("-115.5");
    BigDecimal bottomRightY = new BigDecimal("33.5");

    // Act & Assert
    assertDoesNotThrow(() -> new PatchBuilderExtents(
        topLeftX,
        topLeftY,
        bottomRightX,
        bottomRightY
    ));
  }
}
