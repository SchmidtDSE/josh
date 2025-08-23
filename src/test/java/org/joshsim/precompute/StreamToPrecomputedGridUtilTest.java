package org.joshsim.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class StreamToPrecomputedGridUtilTest {

  @Mock
  private EngineValueFactory mockFactory;
  @Mock
  private PatchBuilderExtents mockExtents;

  private final Units testUnits = Units.of("meters");
  private final long minTimestep = 0;
  private final long maxTimestep = 2;

  @BeforeEach
  void setUp() {
    when(mockExtents.getTopLeftX()).thenReturn(BigDecimal.ZERO);
    when(mockExtents.getTopLeftY()).thenReturn(BigDecimal.ZERO);
    when(mockExtents.getBottomRightX()).thenReturn(BigDecimal.valueOf(2));
    when(mockExtents.getBottomRightY()).thenReturn(BigDecimal.valueOf(2));
  }

  @Test
  void testStreamToGrid() {
    // Given
    PatchKeyConverter.ProjectedValue projectedValue = new PatchKeyConverter.ProjectedValue(
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.valueOf(42.0)
    );

    StreamToPrecomputedGridUtil.StreamGetter streamGetter =
        timestep -> Stream.of(projectedValue);

    // When
    DataGridLayer grid = StreamToPrecomputedGridUtil.streamToGrid(
        mockFactory,
        streamGetter,
        mockExtents,
        minTimestep,
        maxTimestep,
        testUnits
    );

    // Then
    assertEquals(true, grid.isCompatible(mockExtents, minTimestep, maxTimestep));
  }

  @Test
  void testStreamToGridWithMultipleEntries() {
    // Given
    PatchKeyConverter.ProjectedValue projectedValue1 = new PatchKeyConverter.ProjectedValue(
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.valueOf(42.0)
    );

    PatchKeyConverter.ProjectedValue projectedValue2 = new PatchKeyConverter.ProjectedValue(
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.valueOf(24.0)
    );

    StreamToPrecomputedGridUtil.StreamGetter streamGetter =
        timestep -> Stream.of(projectedValue1, projectedValue2);

    // When
    DataGridLayer grid = StreamToPrecomputedGridUtil.streamToGrid(
        mockFactory,
        streamGetter,
        mockExtents,
        minTimestep,
        maxTimestep,
        testUnits
    );

    // Then
    assertEquals(true, grid.isCompatible(mockExtents, minTimestep, maxTimestep));
  }

  @Test
  void testStreamToGridWithDefaultValue() {
    // Given
    double defaultValue = -999.0;
    
    // Empty stream to test that default values are applied
    StreamToPrecomputedGridUtil.StreamGetter streamGetter =
        timestep -> Stream.empty();

    // When
    DataGridLayer grid = StreamToPrecomputedGridUtil.streamToGrid(
        mockFactory,
        streamGetter,
        mockExtents,
        minTimestep,
        maxTimestep,
        testUnits,
        Optional.of(defaultValue)
    );

    // Then
    assertEquals(true, grid.isCompatible(mockExtents, minTimestep, maxTimestep));
    // Additional verification that the grid is properly filled would require
    // accessing the internal values, which is not exposed through the interface
  }

  @Test
  void testStreamToGridWithoutDefaultValue() {
    // Given
    PatchKeyConverter.ProjectedValue projectedValue = new PatchKeyConverter.ProjectedValue(
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.valueOf(42.0)
    );

    StreamToPrecomputedGridUtil.StreamGetter streamGetter =
        timestep -> Stream.of(projectedValue);

    // When
    DataGridLayer grid = StreamToPrecomputedGridUtil.streamToGrid(
        mockFactory,
        streamGetter,
        mockExtents,
        minTimestep,
        maxTimestep,
        testUnits,
        Optional.empty()
    );

    // Then
    assertEquals(true, grid.isCompatible(mockExtents, minTimestep, maxTimestep));
  }
}
