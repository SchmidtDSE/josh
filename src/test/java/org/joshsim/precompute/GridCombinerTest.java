
package org.joshsim.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GridCombinerTest {

  @Mock
  private EngineGeometryFactory mockGeometryFactory;
  @Mock
  private DataGridLayer leftGrid;
  @Mock
  private DataGridLayer rightGrid;
  @Mock
  private EngineValue mockValue;

  private GridCombiner gridCombiner;
  private final Units testUnits = Units.of("meters");

  @BeforeEach
  void setUp() {
    gridCombiner = new GridCombiner(mockGeometryFactory);
    
    // Setup basic grid properties
    when(leftGrid.getMinX()).thenReturn(0L);
    when(leftGrid.getMaxX()).thenReturn(2L);
    when(leftGrid.getMinY()).thenReturn(0L);
    when(leftGrid.getMaxY()).thenReturn(2L);
    when(leftGrid.getMinTimestep()).thenReturn(0L);
    when(leftGrid.getMaxTimestep()).thenReturn(2L);
    when(leftGrid.getUnits()).thenReturn(testUnits);

    when(rightGrid.getMinX()).thenReturn(1L);
    when(rightGrid.getMaxX()).thenReturn(3L);
    when(rightGrid.getMinY()).thenReturn(1L);
    when(rightGrid.getMaxY()).thenReturn(3L);
    when(rightGrid.getMinTimestep()).thenReturn(1L);
    when(rightGrid.getMaxTimestep()).thenReturn(3L);
    when(rightGrid.getUnits()).thenReturn(testUnits);
  }

  @Test
  void testCombineBasicProperties() {
    // When
    DataGridLayer combined = gridCombiner.combine(leftGrid, rightGrid);

    // Then
    assertEquals(0L, combined.getMinX());
    assertEquals(3L, combined.getMaxX());
    assertEquals(0L, combined.getMinY());
    assertEquals(3L, combined.getMaxY());
    assertEquals(0L, combined.getMinTimestep());
    assertEquals(3L, combined.getMaxTimestep());
    assertEquals(testUnits, combined.getUnits());
  }

  @Test
  void testCombineWithDifferentUnits() {
    // Given
    when(rightGrid.getUnits()).thenReturn(Units.of("feet"));

    // Then
    assertThrows(IllegalArgumentException.class, () -> {
      gridCombiner.combine(leftGrid, rightGrid);
    });
  }

  @Test
  void testCombineExtentsCalculation() {
    // Given
    PatchBuilderExtents mockExtents = mock(PatchBuilderExtents.class);
    when(mockExtents.getTopLeftX()).thenReturn(BigDecimal.ZERO);
    when(mockExtents.getTopLeftY()).thenReturn(BigDecimal.valueOf(3));
    when(mockExtents.getBottomRightX()).thenReturn(BigDecimal.valueOf(3));
    when(mockExtents.getBottomRightY()).thenReturn(BigDecimal.ZERO);

    // When
    DataGridLayer combined = gridCombiner.combine(leftGrid, rightGrid);

    // Then
    assertEquals(true, combined.isCompatible(mockExtents, 0L, 3L));
  }
}
