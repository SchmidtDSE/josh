package org.joshsim.engine.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


class ExtentsUtilTest {

  private PatchBuilderExtentsBuilder builder;
  private EngineValueFactory valueFactory;

  @BeforeEach
  void setUp() {
    builder = new PatchBuilderExtentsBuilder();
    valueFactory = EngineValueFactory.getDefault();
  }

  @Test
  @DisplayName("addExtents should handle latitude first coordinates")
  void addExtentsHandlesLatitudeFirst() {
    // Arrange
    String coordinates = "10 degrees latitude, -20 degrees longitude";
    
    // Act
    ExtentsUtil.addExtents(builder, coordinates, true, valueFactory);

    // Assert - verify the coordinates were set correctly
    PatchBuilderExtents extents = builder
        .setBottomRightX(BigDecimal.ONE)
        .setBottomRightY(BigDecimal.ONE)
        .build();
    assertEquals(BigDecimal.valueOf(-20), extents.getTopLeftX());
    assertEquals(BigDecimal.valueOf(10), extents.getTopLeftY());
  }

  @Test
  @DisplayName("addExtents should handle longitude first coordinates")
  void addExtentsHandlesLongitudeFirst() {
    // Arrange
    String coordinates = "-20 degrees longitude, 10 degrees latitude";
    
    // Act
    ExtentsUtil.addExtents(builder, coordinates, false, valueFactory);

    // Assert - verify the coordinates were set correctly
    PatchBuilderExtents extents = builder
        .setTopLeftX(BigDecimal.ONE)
        .setTopLeftY(BigDecimal.ONE)
        .build();
    assertEquals(BigDecimal.valueOf(-20), extents.getBottomRightX());
    assertEquals(BigDecimal.valueOf(10), extents.getBottomRightY());
  }
}
