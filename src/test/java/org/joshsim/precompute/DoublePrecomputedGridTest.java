
package org.joshsim.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
class DoublePrecomputedGridTest {

  @Mock
  private EngineValueFactory mockFactory;
  @Mock
  private PatchBuilderExtents mockExtents;
  @Mock
  private EngineValue mockEngineValue;

  private DoublePrecomputedGrid grid;
  private final Units testUnits = Units.of("meters");
  private final long minTimestep = 0;
  private final long maxTimestep = 10;

  @BeforeEach
  void setUp() {
    when(mockExtents.getTopLeftX()).thenReturn(BigDecimal.ZERO);
    when(mockExtents.getTopLeftY()).thenReturn(BigDecimal.ZERO);
    when(mockExtents.getBottomRightX()).thenReturn(BigDecimal.TEN);
    when(mockExtents.getBottomRightY()).thenReturn(BigDecimal.TEN);

    double[][][] innerValues = new double[11][11][11];
    innerValues[3][2][1] = 45.0;

    grid = new DoublePrecomputedGrid(
      mockFactory,
      mockExtents,
      minTimestep,
      maxTimestep,
      testUnits,
      innerValues
    );
  }

  @Test
  void testGetAt() {
    // Given
    long x = 1;
    long y = 2;
    long timestep = 3;
    double expectedValue = 45.0;
    when(mockFactory.buildForNumber(expectedValue, testUnits))
        .thenReturn(mockEngineValue);

    // When
    EngineValue result = grid.getAt(x, y, timestep);

    // Then
    assertEquals(mockEngineValue, result);
  }

  @Test
  void testGridDimensions() {
    // Given
    PatchBuilderExtents extents = mock(PatchBuilderExtents.class);
    when(extents.getTopLeftX()).thenReturn(BigDecimal.valueOf(-5));
    when(extents.getTopLeftY()).thenReturn(BigDecimal.valueOf(-5));
    when(extents.getBottomRightX()).thenReturn(BigDecimal.valueOf(5));
    when(extents.getBottomRightY()).thenReturn(BigDecimal.valueOf(5));

    // When
    DoublePrecomputedGrid testGrid = new DoublePrecomputedGrid(
        mockFactory,
        extents,
        minTimestep,
        maxTimestep,
        testUnits
    );

    // Then
    assertEquals(true, testGrid.isCompatible(extents, minTimestep, maxTimestep));
  }

  @Test
  void testFill() {
    // Given
    double fillValue = -999.0;
    when(mockFactory.buildForNumber(fillValue, testUnits)).thenReturn(mockEngineValue);

    // When
    grid.fill(fillValue);

    // Then - Check that all positions now have the fill value
    EngineValue result1 = grid.getAt(0, 0, 0);
    EngineValue result2 = grid.getAt(5, 5, 5);
    EngineValue result3 = grid.getAt(10, 10, 10);
    final EngineValue result4 = grid.getAt(1, 2, 3); // Previously had value 45.0

    assertEquals(mockEngineValue, result1);
    assertEquals(mockEngineValue, result2);
    assertEquals(mockEngineValue, result3);
    assertEquals(mockEngineValue, result4);
  }
}
