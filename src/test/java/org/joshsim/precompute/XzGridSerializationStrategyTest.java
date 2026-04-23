package org.joshsim.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class XzGridSerializationStrategyTest {

  private final ValueSupportFactory factory = new ValueSupportFactory();
  private DoublePrecomputedGrid grid;

  @BeforeEach
  void setUp() {
    PatchBuilderExtents extents = mock(PatchBuilderExtents.class);
    when(extents.getTopLeftX()).thenReturn(BigDecimal.ZERO);
    when(extents.getTopLeftY()).thenReturn(BigDecimal.ZERO);
    when(extents.getBottomRightX()).thenReturn(BigDecimal.valueOf(2));
    when(extents.getBottomRightY()).thenReturn(BigDecimal.valueOf(2));

    double[][][] values = new double[3][3][3];
    values[1][2][0] = 42.5;
    values[0][0][2] = 7.0;

    grid = new DoublePrecomputedGrid(factory, extents, 0L, 2L, Units.of("count"), values);
  }

  @Test
  void testRoundTripPreservesMetadata() {
    XzGridSerializationStrategy strategy = new XzGridSerializationStrategy(
        new BinaryGridSerializationStrategy(factory)
    );

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    strategy.serialize(grid, out);

    DataGridLayer loaded = strategy.deserialize(new ByteArrayInputStream(out.toByteArray()));
    assertNotNull(loaded);

    DoublePrecomputedGrid loadedGrid = (DoublePrecomputedGrid) loaded;
    assertEquals(grid.getMinX(), loadedGrid.getMinX());
    assertEquals(grid.getMaxX(), loadedGrid.getMaxX());
    assertEquals(grid.getMinY(), loadedGrid.getMinY());
    assertEquals(grid.getMaxY(), loadedGrid.getMaxY());
    assertEquals(grid.getMinTimestep(), loadedGrid.getMinTimestep());
    assertEquals(grid.getMaxTimestep(), loadedGrid.getMaxTimestep());
    assertEquals(grid.getUnits(), loadedGrid.getUnits());
  }

  @Test
  void testRoundTripPreservesValues() {
    XzGridSerializationStrategy strategy = new XzGridSerializationStrategy(
        new BinaryGridSerializationStrategy(factory)
    );

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    strategy.serialize(grid, out);

    DoublePrecomputedGrid loadedGrid = (DoublePrecomputedGrid) strategy.deserialize(
        new ByteArrayInputStream(out.toByteArray())
    );

    assertEquals(
        42.5,
        loadedGrid.getAt(0, 2, 1).getAsDecimal().doubleValue(),
        1e-9
    );
    assertEquals(
        7.0,
        loadedGrid.getAt(2, 0, 0).getAsDecimal().doubleValue(),
        1e-9
    );
  }

  @Test
  void testCompressedSmallerThanUncompressed() {
    // Use a larger grid to ensure compression has something to work with
    PatchBuilderExtents extents = mock(PatchBuilderExtents.class);
    when(extents.getTopLeftX()).thenReturn(BigDecimal.ZERO);
    when(extents.getTopLeftY()).thenReturn(BigDecimal.ZERO);
    when(extents.getBottomRightX()).thenReturn(BigDecimal.valueOf(9));
    when(extents.getBottomRightY()).thenReturn(BigDecimal.valueOf(9));

    double[][][] values = new double[10][10][10];
    DoublePrecomputedGrid largeGrid = new DoublePrecomputedGrid(
        factory, extents, 0L, 9L, Units.of("count"), values
    );

    BinaryGridSerializationStrategy binary = new BinaryGridSerializationStrategy(factory);
    ByteArrayOutputStream binaryOut = new ByteArrayOutputStream();
    binary.serialize(largeGrid, binaryOut);

    XzGridSerializationStrategy xz = new XzGridSerializationStrategy(binary);

    ByteArrayOutputStream xzOut = new ByteArrayOutputStream();
    xz.serialize(largeGrid, xzOut);

    // XZ-compressed output should be smaller than raw binary for uniform data
    assert xzOut.size() < binaryOut.size()
        : "Expected XZ output (" + xzOut.size() + ") to be smaller than binary ("
            + binaryOut.size() + ")";
  }

}
