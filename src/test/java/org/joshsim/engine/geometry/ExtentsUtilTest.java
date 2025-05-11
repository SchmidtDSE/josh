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
  private EngineValue mockValue;

  @BeforeEach
  void setUp() {
    builder = new PatchBuilderExtentsBuilder();
    valueFactory = mock(EngineValueFactory.class);
    mockValue = mock(EngineValue.class);
    when(valueFactory.build(any(BigDecimal.class), any(Units.class))).thenReturn(mockValue);
    when(mockValue.getAsDecimal()).thenReturn(BigDecimal.ONE);
  }

  @Test
  @DisplayName("addExtents should handle latitude first coordinates")
  void addExtentsHandlesLatitudeFirst() {
    // Arrange
    String coordinates = "10 latitude, -20 longitude";
    
    // Act
    ExtentsUtil.addExtents(builder, coordinates, true, valueFactory);

    // Assert - verify the coordinates were set correctly
    PatchBuilderExtents extents = builder
        .setBottomRightX(BigDecimal.ONE)
        .setBottomRightY(BigDecimal.ONE)
        .build();
    assertEquals(BigDecimal.ONE, extents.getTopLeftX());
    assertEquals(BigDecimal.ONE, extents.getTopLeftY());
  }

  @Test
  @DisplayName("addExtents should handle longitude first coordinates")
  void addExtentsHandlesLongitudeFirst() {
    // Arrange
    String coordinates = "-20 longitude, 10 latitude";
    
    // Act
    ExtentsUtil.addExtents(builder, coordinates, true, valueFactory);

    // Assert - verify the coordinates were set correctly
    PatchBuilderExtents extents = builder
        .setBottomRightX(BigDecimal.ONE)
        .setBottomRightY(BigDecimal.ONE)
        .build();
    assertEquals(BigDecimal.ONE, extents.getTopLeftX());
    assertEquals(BigDecimal.ONE, extents.getTopLeftY());
  }

  @Test
  @DisplayName("parseExtentComponent should correctly parse coordinate components")
  void parseExtentComponentParsesCorrectly() {
    // Arrange
    String component = "1.23 degrees";
    when(valueFactory.build(new BigDecimal("1.23"), new Units("degrees"))).thenReturn(mockValue);

    // Act
    EngineValue result = ExtentsUtil.parseExtentComponent(component, valueFactory);

    // Assert
    assertEquals(mockValue, result);
    verify(valueFactory).build(new BigDecimal("1.23"), new Units("degrees"));
  }

  @Test
  @DisplayName("addExtents should handle end coordinates")
  void addExtentsHandlesEndCoordinates() {
    // Arrange
    String coordinates = "10 latitude, -20 longitude";
    
    // Act
    ExtentsUtil.addExtents(builder, coordinates, false, valueFactory);

    // Assert - verify the coordinates were set correctly
    PatchBuilderExtents extents = builder
        .setTopLeftX(BigDecimal.ONE)
        .setTopLeftY(BigDecimal.ONE)
        .build();
    assertEquals(BigDecimal.ONE, extents.getBottomRightX());
    assertEquals(BigDecimal.ONE, extents.getBottomRightY());
  }
}
