package org.joshsim.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;
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

  @Test
  void testStreamToGridFilterExactDefaultValue() {
    // Given
    double defaultValue = -999.0;
    double nonDefaultValue = 42.0;
    
    PatchKeyConverter.ProjectedValue defaultValueEntry = new PatchKeyConverter.ProjectedValue(
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.valueOf(defaultValue) // Exact match with default
    );
    PatchKeyConverter.ProjectedValue nonDefaultValueEntry = new PatchKeyConverter.ProjectedValue(
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.valueOf(nonDefaultValue) // Different from default
    );

    // Mock the factory to return mock values for our test values
    EngineValue mockDefaultEngineValue = mock(EngineValue.class);
    EngineValue mockNonDefaultEngineValue = mock(EngineValue.class);
    when(mockFactory.buildForNumber(defaultValue, testUnits))
        .thenReturn(mockDefaultEngineValue);
    when(mockFactory.buildForNumber(nonDefaultValue, testUnits))
        .thenReturn(mockNonDefaultEngineValue);

    StreamToPrecomputedGridUtil.StreamGetter streamGetter =
        timestep -> Stream.of(defaultValueEntry, nonDefaultValueEntry);

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
    DoublePrecomputedGrid doubleGrid = (DoublePrecomputedGrid) grid;
    
    // Position (1,1) should contain default value since input was filtered out
    EngineValue valueAtOneOne = doubleGrid.getAt(1, 1, 0);
    // Position (0,0) should contain non-default value since it was not filtered
    EngineValue valueAtZeroZero = doubleGrid.getAt(0, 0, 0);
    
    assertEquals(mockDefaultEngineValue, valueAtOneOne);
    assertEquals(mockNonDefaultEngineValue, valueAtZeroZero);
    assertEquals(true, grid.isCompatible(mockExtents, minTimestep, maxTimestep));
  }

  @Test
  void testStreamToGridFilterWithinTolerance() {
    // Given
    double defaultValue = -999.0;
    double tolerance = 0.000001;
    double withinToleranceValue = defaultValue + tolerance * 0.5; // Within tolerance
    double outsideToleranceValue = defaultValue + tolerance * 2; // Outside tolerance
    
    PatchKeyConverter.ProjectedValue withinToleranceEntry = new PatchKeyConverter.ProjectedValue(
        BigDecimal.ONE,
        BigDecimal.ONE,
        BigDecimal.valueOf(withinToleranceValue)
    );
    PatchKeyConverter.ProjectedValue outsideToleranceEntry = new PatchKeyConverter.ProjectedValue(
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.valueOf(outsideToleranceValue)
    );

    // Mock the factory responses
    EngineValue mockDefaultEngineValue = mock(EngineValue.class);
    EngineValue mockOutsideToleranceEngineValue = mock(EngineValue.class);
    when(mockFactory.buildForNumber(defaultValue, testUnits))
        .thenReturn(mockDefaultEngineValue);
    when(mockFactory.buildForNumber(outsideToleranceValue, testUnits))
        .thenReturn(mockOutsideToleranceEngineValue);

    StreamToPrecomputedGridUtil.StreamGetter streamGetter =
        timestep -> Stream.of(withinToleranceEntry, outsideToleranceEntry);

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
    DoublePrecomputedGrid doubleGrid = (DoublePrecomputedGrid) grid;
    
    // Position (1,1) should contain default since within-tolerance value filtered
    EngineValue valueAtOneOne = doubleGrid.getAt(1, 1, 0);
    // Position (0,0) should contain outside-tolerance value since not filtered
    EngineValue valueAtZeroZero = doubleGrid.getAt(0, 0, 0);
    
    assertEquals(mockDefaultEngineValue, valueAtOneOne);
    assertEquals(mockOutsideToleranceEngineValue, valueAtZeroZero);
    assertEquals(true, grid.isCompatible(mockExtents, minTimestep, maxTimestep));
  }

  @Test
  void testStreamToGridFilterNegativeTolerance() {
    // Given
    double defaultValue = 100.0;
    double tolerance = 0.000001;
    double withinNegativeToleranceValue = defaultValue - tolerance * 0.5;
    double outsideNegativeToleranceValue = defaultValue - tolerance * 2;
    
    PatchKeyConverter.ProjectedValue withinNegativeToleranceEntry =
        new PatchKeyConverter.ProjectedValue(
            BigDecimal.ONE,
            BigDecimal.ONE,
            BigDecimal.valueOf(withinNegativeToleranceValue)
        );
    PatchKeyConverter.ProjectedValue outsideNegativeToleranceEntry =
        new PatchKeyConverter.ProjectedValue(
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.valueOf(outsideNegativeToleranceValue)
        );

    // Mock the factory responses
    EngineValue mockDefaultEngineValue = mock(EngineValue.class);
    EngineValue mockOutsideToleranceEngineValue = mock(EngineValue.class);
    when(mockFactory.buildForNumber(defaultValue, testUnits))
        .thenReturn(mockDefaultEngineValue);
    when(mockFactory.buildForNumber(outsideNegativeToleranceValue, testUnits))
        .thenReturn(mockOutsideToleranceEngineValue);

    StreamToPrecomputedGridUtil.StreamGetter streamGetter =
        timestep -> Stream.of(withinNegativeToleranceEntry, outsideNegativeToleranceEntry);

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
    DoublePrecomputedGrid doubleGrid = (DoublePrecomputedGrid) grid;
    
    // Position (1,1) should contain default since within-tolerance value filtered
    EngineValue valueAtOneOne = doubleGrid.getAt(1, 1, 0);
    // Position (0,0) should contain outside-tolerance value since not filtered
    EngineValue valueAtZeroZero = doubleGrid.getAt(0, 0, 0);
    
    assertEquals(mockDefaultEngineValue, valueAtOneOne);
    assertEquals(mockOutsideToleranceEngineValue, valueAtZeroZero);
    assertEquals(true, grid.isCompatible(mockExtents, minTimestep, maxTimestep));
  }
}
