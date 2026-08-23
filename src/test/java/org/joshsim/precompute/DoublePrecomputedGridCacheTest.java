/**
 * Tests for the built value cache within DoublePrecomputedGrid.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchBuilderExtentsBuilder;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Tests that cached EngineValues are reused, correctly addressed, and invalidated on writes.
 */
class DoublePrecomputedGridCacheTest {

  private static final Units TEST_UNITS = Units.of("meters");
  private static final long MIN_TIMESTEP = 0;
  private static final long MAX_TIMESTEP = 2;

  private DoublePrecomputedGrid grid;

  @BeforeEach
  void setUp() {
    PatchBuilderExtents extents = new PatchBuilderExtentsBuilder()
        .setTopLeftX(BigDecimal.ZERO)
        .setTopLeftY(BigDecimal.ZERO)
        .setBottomRightX(BigDecimal.valueOf(2))
        .setBottomRightY(BigDecimal.valueOf(2))
        .build();

    grid = new DoublePrecomputedGridBuilder()
        .setValueSupportFactory(new ValueSupportFactory())
        .setExtents(extents)
        .setTimestepRange(MIN_TIMESTEP, MAX_TIMESTEP)
        .setUnits(TEST_UNITS)
        .build();

    // Give every cell of every timestep a distinct value so a mis-addressed cache is visible.
    for (long timestep = MIN_TIMESTEP; timestep <= MAX_TIMESTEP; timestep++) {
      for (long y = 0; y <= 2; y++) {
        for (long x = 0; x <= 2; x++) {
          grid.setAt(x, y, timestep, valueFor(x, y, timestep));
        }
      }
    }
  }

  private static double valueFor(long x, long y, long timestep) {
    return timestep * 100 + y * 10 + x;
  }

  @Test
  void testEveryCellReadsBackItsOwnValue() {
    for (long timestep = MIN_TIMESTEP; timestep <= MAX_TIMESTEP; timestep++) {
      for (long y = 0; y <= 2; y++) {
        for (long x = 0; x <= 2; x++) {
          EngineValue result = grid.getAt(x, y, timestep);
          assertEquals(
              valueFor(x, y, timestep),
              result.getAsDecimal().doubleValue(),
              0.0001
          );
        }
      }
    }
  }

  @Test
  void testCellsWithinOneTimestepStayDistinctWhenCached() {
    // Read the whole slice twice so every read after the first is served from the cache.
    for (int pass = 0; pass < 2; pass++) {
      for (long y = 0; y <= 2; y++) {
        for (long x = 0; x <= 2; x++) {
          assertEquals(
              valueFor(x, y, 1),
              grid.getAt(x, y, 1).getAsDecimal().doubleValue(),
              0.0001
          );
        }
      }
    }
  }

  @Test
  void testRepeatedReadReusesTheSameValue() {
    EngineValue first = grid.getAt(1, 2, 1);
    EngineValue second = grid.getAt(1, 2, 1);

    assertSame(first, second);
  }

  @Test
  void testTimestepsDoNotShareCachedValues() {
    assertEquals(11.0, grid.getAt(1, 1, 0).getAsDecimal().doubleValue(), 0.0001);
    assertEquals(111.0, grid.getAt(1, 1, 1).getAsDecimal().doubleValue(), 0.0001);
    assertEquals(211.0, grid.getAt(1, 1, 2).getAsDecimal().doubleValue(), 0.0001);
  }

  @Test
  void testWriteInvalidatesCachedValue() {
    assertEquals(111.0, grid.getAt(1, 1, 1).getAsDecimal().doubleValue(), 0.0001);

    grid.setAt(1, 1, 1, -7.0);

    assertEquals(-7.0, grid.getAt(1, 1, 1).getAsDecimal().doubleValue(), 0.0001);
    // Neighbours must be untouched by the invalidation.
    assertEquals(112.0, grid.getAt(2, 1, 1).getAsDecimal().doubleValue(), 0.0001);
    assertEquals(121.0, grid.getAt(1, 2, 1).getAsDecimal().doubleValue(), 0.0001);
  }

  @Test
  void testNonSquareGridAddressesEveryCellDistinctly() {
    // A square grid cannot tell width from height in the flat cache index, so this case uses a
    // grid four wide and two tall. Substituting height for width in the index would make
    // (3, 0) and (1, 1) collide on the same slot.
    PatchBuilderExtents wideExtents = new PatchBuilderExtentsBuilder()
        .setTopLeftX(BigDecimal.ZERO)
        .setTopLeftY(BigDecimal.ZERO)
        .setBottomRightX(BigDecimal.valueOf(3))
        .setBottomRightY(BigDecimal.ONE)
        .build();

    DoublePrecomputedGrid wideGrid = new DoublePrecomputedGridBuilder()
        .setValueSupportFactory(new ValueSupportFactory())
        .setExtents(wideExtents)
        .setTimestepRange(MIN_TIMESTEP, MAX_TIMESTEP)
        .setUnits(TEST_UNITS)
        .build();

    for (long y = 0; y <= 1; y++) {
      for (long x = 0; x <= 3; x++) {
        wideGrid.setAt(x, y, 0, valueFor(x, y, 0));
      }
    }

    // Read every cell twice so the second pass is served entirely from the cache.
    for (int pass = 0; pass < 2; pass++) {
      for (long y = 0; y <= 1; y++) {
        for (long x = 0; x <= 3; x++) {
          assertEquals(
              valueFor(x, y, 0),
              wideGrid.getAt(x, y, 0).getAsDecimal().doubleValue(),
              0.0001
          );
        }
      }
    }
  }

  @Test
  void testNonSquareGridInvalidatesOnlyTheCellWritten() {
    PatchBuilderExtents wideExtents = new PatchBuilderExtentsBuilder()
        .setTopLeftX(BigDecimal.ZERO)
        .setTopLeftY(BigDecimal.ZERO)
        .setBottomRightX(BigDecimal.valueOf(3))
        .setBottomRightY(BigDecimal.ONE)
        .build();

    DoublePrecomputedGrid wideGrid = new DoublePrecomputedGridBuilder()
        .setValueSupportFactory(new ValueSupportFactory())
        .setExtents(wideExtents)
        .setTimestepRange(MIN_TIMESTEP, MAX_TIMESTEP)
        .setUnits(TEST_UNITS)
        .build();

    for (long y = 0; y <= 1; y++) {
      for (long x = 0; x <= 3; x++) {
        wideGrid.setAt(x, y, 0, valueFor(x, y, 0));
        wideGrid.getAt(x, y, 0);
      }
    }

    wideGrid.setAt(3, 0, 0, -12.0);

    assertEquals(-12.0, wideGrid.getAt(3, 0, 0).getAsDecimal().doubleValue(), 0.0001);
    assertEquals(valueFor(1, 1, 0), wideGrid.getAt(1, 1, 0).getAsDecimal().doubleValue(), 0.0001);
  }

  @Test
  void testFillInvalidatesCachedValues() {
    assertEquals(111.0, grid.getAt(1, 1, 1).getAsDecimal().doubleValue(), 0.0001);

    grid.fill(-3.0);

    for (long timestep = MIN_TIMESTEP; timestep <= MAX_TIMESTEP; timestep++) {
      for (long y = 0; y <= 2; y++) {
        for (long x = 0; x <= 2; x++) {
          assertEquals(
              -3.0,
              grid.getAt(x, y, timestep).getAsDecimal().doubleValue(),
              0.0001
          );
        }
      }
    }
  }

}
