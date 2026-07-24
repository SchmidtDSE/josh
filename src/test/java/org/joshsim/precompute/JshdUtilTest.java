package org.joshsim.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class JshdUtilTest {

  private final ValueSupportFactory factory = new ValueSupportFactory();

  @Mock
  private EngineValue mockEngineValue;

  private DoublePrecomputedGrid grid;
  private PatchBuilderExtents extents;
  private final Units testUnits = Units.of("meters");
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
        factory,
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
    Units units = Units.of(longUnits.toString());

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
    assertEquals(2, buffer.getInt()); // version
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
    assertEquals(Units.of(testUnits), loaded.getUnits());
  }

  @Test
  void testLoadBody() {
    byte[] serialized = JshdUtil.serializeToBytes(grid);
    ByteBuffer buffer = ByteBuffer.wrap(serialized);
    DoublePrecomputedGrid loaded = JshdUtil.loadFromBytes(factory, buffer.array());
    assertEquals(5, loaded.getAt(0, 1, 2).getAsDecimal().longValue());
  }

  @Test
  void serializesAndLoadsCountTimeAxis() {
    TimeAxis axis = TimeAxis.countRange(
        "calendar_year", "year", BigDecimal.valueOf(2015), BigDecimal.ONE, 3);
    DoublePrecomputedGrid timedGrid = new DoublePrecomputedGrid(
        factory, extents, minTimestep, maxTimestep, testUnits, new double[3][3][3],
        Optional.of(axis));

    DoublePrecomputedGrid loaded = JshdUtil.loadFromBytes(
        factory, JshdUtil.serializeToBytes(timedGrid));

    assertEquals(TimeAxis.Type.COUNT, loaded.getTimeAxis().orElseThrow().getType());
    assertEquals("year", loaded.getTimeAxis().orElseThrow().getCountUnit());
    assertEquals(BigDecimal.valueOf(2015), loaded.getTimeAxis().orElseThrow().getCountStart());
    assertEquals(3, loaded.getTimeAxis().orElseThrow().getCount());
  }

  @Test
  void serializesAndLoadsIsoTimeAxis() {
    TimeAxis axis = TimeAxis.isoRange(
        "time", LocalDate.parse("2026-01-01"), Period.parse("P1M"), 3);
    DoublePrecomputedGrid timedGrid = new DoublePrecomputedGrid(
        factory, extents, minTimestep, maxTimestep, testUnits, new double[3][3][3],
        Optional.of(axis));

    DoublePrecomputedGrid loaded = JshdUtil.loadFromBytes(
        factory, JshdUtil.serializeToBytes(timedGrid));

    assertEquals(TimeAxis.Type.ISO, loaded.getTimeAxis().orElseThrow().getType());
    assertEquals(LocalDate.parse("2026-01-01"), loaded.getTimeAxis().orElseThrow().getIsoStart());
    assertEquals(Period.parse("P1M"), loaded.getTimeAxis().orElseThrow().getIsoInterval());
  }

  @Test
  void resolvesOnlyExactDeclaredCoordinates() {
    TimeAxis countAxis = TimeAxis.countRange(
        "year", "year", BigDecimal.valueOf(2015), BigDecimal.ONE, 3);
    TimeAxis isoAxis = TimeAxis.isoRange(
        "time", LocalDate.parse("2026-01-01"), Period.parse("P1M"), 3);

    assertEquals(2, countAxis.getCountIndex(BigDecimal.valueOf(2017)));
    assertThrows(IllegalArgumentException.class,
        () -> countAxis.getCountIndex(BigDecimal.valueOf(2018)));
    assertEquals(1, isoAxis.getIsoIndex(LocalDate.parse("2026-02-01")));
    assertThrows(IllegalArgumentException.class,
        () -> isoAxis.getIsoIndex(LocalDate.parse("2026-02-15")));
  }

  @Test
  void countAxisRejectsNonAlignedCoordinates() {
    TimeAxis axis = TimeAxis.countRange(
        "year", "year", BigDecimal.valueOf(2015), BigDecimal.ONE, 3);
    // 2015.5 is not on the grid (start=2015, increment=1)
    assertThrows(IllegalArgumentException.class,
        () -> axis.getCountIndex(BigDecimal.valueOf(2015.5)));
  }

  @Test
  void countAxisSupportsNonUnitIncrements() {
    TimeAxis axis = TimeAxis.countRange(
        "year", "year", BigDecimal.valueOf(2010), BigDecimal.valueOf(5), 3);
    assertEquals(0, axis.getCountIndex(BigDecimal.valueOf(2010)));
    assertEquals(1, axis.getCountIndex(BigDecimal.valueOf(2015)));
    assertEquals(2, axis.getCountIndex(BigDecimal.valueOf(2020)));
    assertThrows(IllegalArgumentException.class,
        () -> axis.getCountIndex(BigDecimal.valueOf(2011)));
  }

  @Test
  void instantAxisResolvesSingleCoordinate() {
    TimeAxis countInstant = TimeAxis.countInstant("time", "year", BigDecimal.valueOf(2020));
    TimeAxis isoInstant = TimeAxis.isoInstant("time", LocalDate.parse("2020-09-01"));

    assertEquals(0, countInstant.getCountIndex(BigDecimal.valueOf(2020)));
    assertThrows(IllegalArgumentException.class,
        () -> countInstant.getCountIndex(BigDecimal.valueOf(2021)));
    assertEquals(0, isoInstant.getIsoIndex(LocalDate.parse("2020-09-01")));
    assertThrows(IllegalArgumentException.class,
        () -> isoInstant.getIsoIndex(LocalDate.parse("2020-09-02")));
  }

  @Test
  void appendsOnlyContiguousCompatibleTemporalRanges() {
    TimeAxis first = TimeAxis.countRange(
        "year", "year", BigDecimal.valueOf(2015), BigDecimal.ONE, 2);
    TimeAxis following = TimeAxis.countRange(
        "year", "year", BigDecimal.valueOf(2017), BigDecimal.ONE, 2);

    TimeAxis merged = first.append(following);

    assertEquals(4, merged.getCount());
    assertEquals(3, merged.getCountIndex(BigDecimal.valueOf(2018)));
    assertThrows(IllegalArgumentException.class, () -> first.append(TimeAxis.countRange(
        "year", "year", BigDecimal.valueOf(2018), BigDecimal.ONE, 2)));
    assertThrows(IllegalArgumentException.class, () -> first.append(TimeAxis.countRange(
        "year", "month", BigDecimal.valueOf(2017), BigDecimal.ONE, 2)));
  }

  @Test
  void appendsContiguousIsoRangesUsingCalendarArithmetic() {
    TimeAxis first = TimeAxis.isoRange(
        "time", LocalDate.of(2026, 1, 31), Period.ofMonths(1), 2);
    TimeAxis following = TimeAxis.isoRange(
        "time", LocalDate.of(2026, 3, 28), Period.ofMonths(1), 2);

    TimeAxis merged = first.append(following);

    assertEquals(4, merged.getCount());
    assertEquals(3, merged.getIsoIndex(LocalDate.of(2026, 4, 28)));
  }

  @Test
  void v1JshdLoadsAsTimeless() {
    // Build a v1-format buffer manually (version=1, no temporal metadata block)
    String testUnits = "meters";
    byte[] unitsBytes = testUnits.getBytes();
    ByteBuffer buffer = ByteBuffer.allocate(
        Integer.BYTES + 6 * Long.BYTES + Integer.BYTES + unitsBytes.length + 3 * 3 * 3 * 8
    );
    buffer.putInt(1); // version 1
    buffer.putLong(0L); // minX
    buffer.putLong(2L); // maxX
    buffer.putLong(0L); // minY
    buffer.putLong(2L); // maxY
    buffer.putLong(0L); // minTimestep
    buffer.putLong(2L); // maxTimestep
    buffer.putInt(unitsBytes.length);
    buffer.put(unitsBytes);
    for (int i = 0; i < 3 * 3 * 3; i++) {
      buffer.putDouble(0);
    }

    DoublePrecomputedGrid loaded = JshdUtil.loadFromBytes(factory, buffer.array());
    assertTrue(loaded.getTimeAxis().isEmpty(),
        "v1 JSHD should load without temporal metadata");
  }
}
