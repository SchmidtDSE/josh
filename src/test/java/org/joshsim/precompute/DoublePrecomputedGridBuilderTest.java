
package org.joshsim.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DoublePrecomputedGridBuilderTest {

  @Mock(lenient = true)
  private PatchBuilderExtents mockExtents;

  private DoublePrecomputedGridBuilder builder;
  private EngineValueFactory engineValueFactory;
  private final Units testUnits = Units.of("meters");
  private final long minTimestep = 0;
  private final long maxTimestep = 10;

  @BeforeEach
  void setUp() {
    when(mockExtents.getTopLeftX()).thenReturn(BigDecimal.ZERO);
    when(mockExtents.getTopLeftY()).thenReturn(BigDecimal.ZERO);
    when(mockExtents.getBottomRightX()).thenReturn(BigDecimal.TEN);
    when(mockExtents.getBottomRightY()).thenReturn(BigDecimal.TEN);

    builder = new DoublePrecomputedGridBuilder();
    engineValueFactory = CompatibilityLayerKeeper.get().getEngineValueFactory();
  }

  @Test
  void testBuildWithRequiredParameters() {
    // When
    DoublePrecomputedGrid grid = builder
        .setEngineValueFactory(engineValueFactory)
        .setExtents(mockExtents)
        .setTimestepRange(minTimestep, maxTimestep)
        .setUnits(testUnits)
        .build();

    // Then
    assertEquals(testUnits, grid.getUnits());
  }

  @Test
  void testBuildWithInnerValues() {
    // Given
    double[][][] innerValues = new double[11][11][11];
    innerValues[3][2][1] = 45.0;

    // When
    DoublePrecomputedGrid grid = builder
        .setEngineValueFactory(engineValueFactory)
        .setExtents(mockExtents)
        .setTimestepRange(minTimestep, maxTimestep)
        .setUnits(testUnits)
        .setInnerValues(innerValues)
        .build();

    // Then
    assertEquals(testUnits, grid.getUnits());
    assertEquals(45L, grid.getAt(1, 2, 3).getAsDecimal().longValue());
  }

  @Test
  void testBuildWithoutEngineValueFactory() {
    // When/Then
    assertThrows(IllegalArgumentException.class, () -> {
      builder
          .setExtents(mockExtents)
          .setTimestepRange(minTimestep, maxTimestep)
          .setUnits(testUnits)
          .build();
    });
  }

  @Test
  void testBuildWithoutExtents() {
    // When/Then
    assertThrows(IllegalArgumentException.class, () -> {
      builder
          .setEngineValueFactory(engineValueFactory)
          .setTimestepRange(minTimestep, maxTimestep)
          .setUnits(testUnits)
          .build();
    });
  }

  @Test
  void testBuildWithoutUnits() {
    // When/Then
    assertThrows(IllegalArgumentException.class, () -> {
      builder
          .setEngineValueFactory(engineValueFactory)
          .setExtents(mockExtents)
          .setTimestepRange(minTimestep, maxTimestep)
          .build();
    });
  }
}
