package org.joshsim.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
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
class JshdUtilTest {

  private final EngineValueFactory factory = EngineValueFactory.getDefault();

  @Mock
  private EngineValue mockEngineValue;

  private DoublePrecomputedGrid grid;
  private PatchBuilderExtents extents;
  private final Units testUnits = new Units("meters");
  private final long minTimestep = 0;
  private final long maxTimestep = 2;

  @BeforeEach
  void setUp() {
    extents = mock(PatchBuilderExtents.class);
    when(extents.getTopLeftX()).thenReturn(BigDecimal.ZERO);
    when(extents.getTopLeftY()).thenReturn(BigDecimal.ZERO);
    when(extents.getBottomRightX()).thenReturn(BigDecimal.valueOf(2));
    when(extents.getBottomRightY()).thenReturn(BigDecimal.valueOf(2));

    double[][][] innerValues = new double[3][3][3];
    innerValues[2][1][0] = 5;

    grid = new DoublePrecomputedGrid(
        EngineValueFactory.getDefault(),
        extents,
        minTimestep,
        maxTimestep,
        testUnits,
        innerValues
    );
  }

  @Test
  void testSerializeAndLoadBytes() {
    // When
    byte[] serialized = JshdUtil.serializeToBytes(grid);
    DoublePrecomputedGrid loaded = JshdUtil.loadFromBytes(factory, serialized);

    // Then
    assertEquals(grid.getMinX(), loaded.getMinX());
    assertEquals(grid.getMaxX(), loaded.getMaxX());
    assertEquals(grid.getMinY(), loaded.getMinY());
    assertEquals(grid.getMaxY(), loaded.getMaxY());
    assertEquals(grid.getMinTimestep(), loaded.getMinTimestep());
    assertEquals(grid.getMaxTimestep(), loaded.getMaxTimestep());
    assertEquals(grid.getUnits(), loaded.getUnits());
  }

  @Test
  void testUnitsExceedingMaxLength() {
    // Create a grid with very long units string
    StringBuilder longUnits = new StringBuilder();
    for (int i = 0; i < 201; i++) {
      longUnits.append('m');
    }
    Units units = new Units(longUnits.toString());

    DoublePrecomputedGrid gridWithLongUnits = new DoublePrecomputedGrid(
        factory,
        extents,
        minTimestep,
        maxTimestep,
        units,
        new double[3][3][3]
    );

    assertThrows(IllegalArgumentException.class, () -> {
      JshdUtil.serializeToBytes(gridWithLongUnits);
    });
  }

  @Test
  void testSerializeHeader() {
    // When
    byte[] serialized = JshdUtil.serializeToBytes(grid);
    ByteBuffer buffer = ByteBuffer.wrap(serialized);

    // Then
    assertEquals(1, buffer.getInt()); // version
    assertEquals(0L, buffer.getLong()); // minX
    assertEquals(2L, buffer.getLong()); // maxX
    assertEquals(0L, buffer.getLong()); // minY
    assertEquals(2L, buffer.getLong()); // maxY
    assertEquals(0L, buffer.getLong()); // minTimestep
    assertEquals(2L, buffer.getLong()); // maxTimestep
  }

  @Test
  void testLoadHeader() {
    // Given
    String testUnits = "meters";
    byte[] unitsBytes = testUnits.getBytes();
    ByteBuffer buffer = ByteBuffer.allocate(
        Integer.BYTES + 6 * 8 + Integer.BYTES + unitsBytes.length + 3 * 3 * 3 * 8
    );
    buffer.putInt(1); // version
    buffer.putLong(0L); // minX
    buffer.putLong(2L); // maxX
    buffer.putLong(0L); // minY
    buffer.putLong(2L); // maxY
    buffer.putLong(0L); // minTimestep
    buffer.putLong(2L); // maxTimestep
    buffer.putInt(unitsBytes.length); // units length
    buffer.put(unitsBytes); // units string

    for (int x = 0; x <= 2; x++) {
      for (int y = 0; y <= 2; y++) {
        for (int timestep = 0; timestep <= 2; timestep++) {
          buffer.putDouble(0);
        }
      }
    }

    // When
    DoublePrecomputedGrid loaded = JshdUtil.loadFromBytes(factory, buffer.array());

    // Then
    assertEquals(0L, loaded.getMinX());
    assertEquals(2L, loaded.getMaxX());
    assertEquals(0L, loaded.getMinY());
    assertEquals(2L, loaded.getMaxY());
    assertEquals(0L, loaded.getMinTimestep());
    assertEquals(2L, loaded.getMaxTimestep());
    assertEquals(new Units(testUnits), loaded.getUnits());
  }

  @Test
  void testLoadBody() {
    byte[] serialized = JshdUtil.serializeToBytes(grid);
    ByteBuffer buffer = ByteBuffer.wrap(serialized);
    DoublePrecomputedGrid loaded = JshdUtil.loadFromBytes(factory, buffer.array());
    assertEquals(5, loaded.getAt(0, 1, 2).getAsDecimal().longValue());
  }
}