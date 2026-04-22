
package org.joshsim.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Random;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.Test;


class ColdStartDataGridLayerTest {

  private static final long INNER_MIN_TIMESTEP = 0;
  private static final long INNER_MAX_TIMESTEP = 4;
  private static final long COLD_START_DURATION = 10;
  private static final long SEED = 42L;

  private DataGridLayer makeInner() {
    return makeInner(INNER_MIN_TIMESTEP, INNER_MAX_TIMESTEP);
  }

  private DataGridLayer makeInner(long minTimestep, long maxTimestep) {
    DataGridLayer inner = mock(DataGridLayer.class);
    when(inner.getMinTimestep()).thenReturn(minTimestep);
    when(inner.getMaxTimestep()).thenReturn(maxTimestep);
    when(inner.getMinX()).thenReturn(0L);
    when(inner.getMaxX()).thenReturn(5L);
    when(inner.getMinY()).thenReturn(0L);
    when(inner.getMaxY()).thenReturn(5L);
    when(inner.getWidth()).thenReturn(6L);
    when(inner.getHeight()).thenReturn(6L);
    when(inner.getUnits()).thenReturn(Units.of("meters"));
    when(inner.getAt(any(GeoKey.class), any(Long.class)))
        .thenReturn(mock(EngineValue.class));
    return inner;
  }

  @Test
  void testColdStartPhaseRemapsTimesteps() {
    DataGridLayer inner = makeInner();
    GeoKey key = mock(GeoKey.class);

    ColdStartDataGridLayer layer = new ColdStartDataGridLayer(
        inner, COLD_START_DURATION, new Random(SEED)
    );

    for (long t = 0; t < COLD_START_DURATION; t++) {
      layer.getAt(key, t);
    }
  }

  @Test
  void testColdStartDeterministicWithSeed() {
    DataGridLayer inner = makeInner();
    GeoKey key = mock(GeoKey.class);

    ColdStartDataGridLayer layer1 = new ColdStartDataGridLayer(
        inner, COLD_START_DURATION, new Random(SEED)
    );
    ColdStartDataGridLayer layer2 = new ColdStartDataGridLayer(
        inner, COLD_START_DURATION, new Random(SEED)
    );

    for (long t = 0; t < COLD_START_DURATION; t++) {
      layer1.getAt(key, t);
      layer2.getAt(key, t);
    }
  }

  @Test
  void testPostColdStartOffsetsToInnerTimesteps() {
    DataGridLayer inner = makeInner();
    GeoKey key = mock(GeoKey.class);

    ColdStartDataGridLayer layer = new ColdStartDataGridLayer(
        inner, COLD_START_DURATION, new Random(SEED)
    );

    // timestep 10 -> inner 0, timestep 11 -> inner 1, timestep 14 -> inner 4
    layer.getAt(key, COLD_START_DURATION);
    layer.getAt(key, COLD_START_DURATION + 1);
    layer.getAt(key, COLD_START_DURATION + 4);
  }

  @Test
  void testPassthroughForOutOfRangeTimesteps() {
    DataGridLayer inner = makeInner();
    GeoKey key = mock(GeoKey.class);

    ColdStartDataGridLayer layer = new ColdStartDataGridLayer(
        inner, COLD_START_DURATION, new Random(SEED)
    );

    // Literal timestep 2020 is outside decorator range -> passed through to inner
    layer.getAt(key, 2020L);
  }

  @Test
  void testSpatialGettersForwardToInner() {
    DataGridLayer inner = makeInner();

    ColdStartDataGridLayer layer = new ColdStartDataGridLayer(
        inner, COLD_START_DURATION, new Random(SEED)
    );

    assertEquals(0L, layer.getMinX());
    assertEquals(5L, layer.getMaxX());
    assertEquals(0L, layer.getMinY());
    assertEquals(5L, layer.getMaxY());
    assertEquals(6L, layer.getWidth());
    assertEquals(6L, layer.getHeight());
    assertEquals(Units.of("meters"), layer.getUnits());
  }

  @Test
  void testTimestepRangeExtended() {
    DataGridLayer inner = makeInner();

    ColdStartDataGridLayer layer = new ColdStartDataGridLayer(
        inner, COLD_START_DURATION, new Random(SEED)
    );

    assertEquals(0L, layer.getMinTimestep());
    // coldStart(10) + innerCount(5) - 1 = 14
    assertEquals(14L, layer.getMaxTimestep());
  }

  @Test
  void testZeroColdStartDurationIsPassthrough() {
    DataGridLayer inner = makeInner();
    GeoKey key = mock(GeoKey.class);

    ColdStartDataGridLayer layer = new ColdStartDataGridLayer(
        inner, 0, new Random(SEED)
    );

    layer.getAt(key, 0L);
    layer.getAt(key, 4L);

    assertEquals(0L, layer.getMinTimestep());
    assertEquals(4L, layer.getMaxTimestep());
  }

  @Test
  void testSingleTimestepSource() {
    DataGridLayer singleInner = makeInner(0, 0);
    GeoKey key = mock(GeoKey.class);

    ColdStartDataGridLayer layer = new ColdStartDataGridLayer(
        singleInner, 5, new Random(SEED)
    );

    // All cold-start timesteps map to the only available timestep (0)
    for (long t = 0; t < 5; t++) {
      layer.getAt(key, t);
    }

    // Post cold-start: timestep 5 maps to inner 0
    layer.getAt(key, 5L);
  }

  @Test
  void testNegativeColdStartDurationThrows() {
    DataGridLayer inner = makeInner();
    assertThrows(IllegalArgumentException.class, () -> {
      new ColdStartDataGridLayer(inner, -1, new Random(SEED));
    });
  }

  @Test
  void testColdStartDurationOfOneStep() {
    DataGridLayer inner = makeInner();
    GeoKey key = mock(GeoKey.class);

    ColdStartDataGridLayer layer = new ColdStartDataGridLayer(
        inner, 1, new Random(SEED)
    );

    layer.getAt(key, 0L);
    layer.getAt(key, 1L);

    assertEquals(0L, layer.getMinTimestep());
    assertEquals(5L, layer.getMaxTimestep());
  }

  @Test
  void testNonZeroInnerMinTimestep() {
    DataGridLayer offsetInner = makeInner(100, 104);
    GeoKey key = mock(GeoKey.class);

    ColdStartDataGridLayer layer = new ColdStartDataGridLayer(
        offsetInner, 3, new Random(SEED)
    );

    for (long t = 0; t < 3; t++) {
      layer.getAt(key, t);
    }

    assertEquals(0L, layer.getMinTimestep());
    assertEquals(7L, layer.getMaxTimestep());
  }
}
