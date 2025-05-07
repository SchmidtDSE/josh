
package org.joshsim.precompute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Stream;
import org.joshsim.engine.entity.base.GeoKey;
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
  @Mock
  private EngineValue mockEngineValue;
  @Mock
  private GeoKey mockGeoKey;

  private final Units testUnits = new Units("meters");
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
    when(mockGeoKey.getCenterX()).thenReturn(BigDecimal.ONE);
    when(mockGeoKey.getCenterY()).thenReturn(BigDecimal.ONE);
    when(mockEngineValue.getAsDecimal()).thenReturn(BigDecimal.valueOf(42.0));

    Map.Entry<GeoKey, EngineValue> entry =
        new AbstractMap.SimpleEntry<>(mockGeoKey, mockEngineValue);
    StreamToPrecomputedGridUtil.StreamGetter streamGetter =
        timestep -> Stream.of(entry);

    // When
    PrecomputedGrid grid = StreamToPrecomputedGridUtil.streamToGrid(
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
    when(mockGeoKey.getCenterX()).thenReturn(BigDecimal.ONE);
    when(mockGeoKey.getCenterY()).thenReturn(BigDecimal.ONE);
    when(mockEngineValue.getAsDecimal()).thenReturn(BigDecimal.valueOf(42.0));

    GeoKey mockGeoKey2 = mock(GeoKey.class);
    when(mockGeoKey2.getCenterX()).thenReturn(BigDecimal.ZERO);
    when(mockGeoKey2.getCenterY()).thenReturn(BigDecimal.ZERO);

    EngineValue mockEngineValue2 = mock(EngineValue.class);
    when(mockEngineValue2.getAsDecimal()).thenReturn(BigDecimal.valueOf(24.0));

    Map.Entry<GeoKey, EngineValue> entry1 =
        new AbstractMap.SimpleEntry<>(mockGeoKey, mockEngineValue);
    Map.Entry<GeoKey, EngineValue> entry2 =
        new AbstractMap.SimpleEntry<>(mockGeoKey2, mockEngineValue2);

    StreamToPrecomputedGridUtil.StreamGetter streamGetter =
        timestep -> Stream.of(entry1, entry2);

    // When
    PrecomputedGrid grid = StreamToPrecomputedGridUtil.streamToGrid(
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
}
