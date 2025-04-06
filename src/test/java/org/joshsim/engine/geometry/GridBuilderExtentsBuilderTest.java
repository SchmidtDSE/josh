
package org.joshsim.engine.geometry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GridBuilderExtentsBuilderTest {

  private GridBuilderExtentsBuilder builder;

  @BeforeEach
  void setUp() {
    builder = new GridBuilderExtentsBuilder();
  }

  @Test
  @DisplayName("Builder should create valid GridBuilderExtents when all coordinates are set")
  void builderCreatesValidExtents() {
    // Arrange
    BigDecimal topLeftX = new BigDecimal("-115.55");
    BigDecimal topLeftY = new BigDecimal("33.55");
    BigDecimal bottomRightX = new BigDecimal("-115.5");
    BigDecimal bottomRightY = new BigDecimal("33.5");

    // Act & Assert
    assertDoesNotThrow(() -> builder
        .setTopLeftX(topLeftX)
        .setTopLeftY(topLeftY)
        .setBottomRightX(bottomRightX)
        .setBottomRightY(bottomRightY)
        .build());
  }

  @Test
  @DisplayName("Builder should throw IllegalStateException when coordinates are missing")
  void builderThrowsWhenCoordinatesMissing() {
    // Arrange - deliberately skip setting some coordinates
    builder.setTopLeftX(new BigDecimal("-115.55"))
        .setTopLeftY(new BigDecimal("33.55"));

    // Act & Assert
    assertThrows(IllegalStateException.class, () -> builder.build());
  }

  @Test
  @DisplayName("Built object should contain correct coordinate values")
  void builtObjectHasCorrectValues() {
    // Arrange
    BigDecimal topLeftX = new BigDecimal("-115.55");
    BigDecimal topLeftY = new BigDecimal("33.55");
    BigDecimal bottomRightX = new BigDecimal("-115.5");
    BigDecimal bottomRightY = new BigDecimal("33.5");

    // Act
    GridBuilderExtents extents = builder
        .setTopLeftX(topLeftX)
        .setTopLeftY(topLeftY)
        .setBottomRightX(bottomRightX)
        .setBottomRightY(bottomRightY)
        .build();

    // Assert
    assertEquals(topLeftX, extents.getTopLeftX());
    assertEquals(topLeftY, extents.getTopLeftY());
    assertEquals(bottomRightX, extents.getBottomRightX());
    assertEquals(bottomRightY, extents.getBottomRightY());
  }
}
