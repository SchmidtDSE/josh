/**
 * Tests for a bridge which cache quries through GeometryMomentos.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.base.Simulation;
import org.joshsim.engine.entity.type.Patch;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.simulation.Query;
import org.joshsim.engine.simulation.Replicate;
import org.joshsim.engine.value.converter.Converter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


/**
 * Test an EngineBridge which performs query caching.
 */
@ExtendWith(MockitoExtension.class)
public class QueryCacheEngineBridgeTest {

  @Mock(lenient = true) private Simulation mockSimulation;
  @Mock(lenient = true) private Replicate mockReplicate;
  @Mock(lenient = true) private Converter mockConverter;
  @Mock(lenient = true) private Patch mockPatch;
  @Mock(lenient = true) private Geometry mockGeometry;
  @Mock(lenient = true) private GeometryMomento mockGeometryMomento;
  @Mock(lenient = true) private GeoKey mockGeoKey;

  private QueryCacheEngineBridge bridge;

  /**
   * Create an example bridge.
   */
  @BeforeEach
  void setUp() {
    bridge = new QueryCacheEngineBridge(mockSimulation, mockReplicate, mockConverter);
  }

  @Test
  void testGetPriorPatchesWithoutCache() {
    // Setup
    when(mockGeometryMomento.build()).thenReturn(mockGeometry);
    when(mockPatch.getKey()).thenReturn(Optional.of(mockGeoKey));
    List<Entity> patches = Arrays.asList(mockPatch);
    when(mockReplicate.query(any(Query.class))).thenReturn(patches);
    when(mockReplicate.getPatchByKey(eq(mockGeoKey), eq(-1L))).thenReturn(mockPatch);

    // First call - should query and cache
    Iterable<Entity> result = bridge.getPriorPatches(mockGeometryMomento);

    // Verify
    verify(mockReplicate, times(1)).query(any(Query.class));
    verify(mockGeometryMomento, times(1)).build();
  }

  @Test
  void testGetPriorPatchesWithCache() {
    // Setup
    when(mockGeometryMomento.build()).thenReturn(mockGeometry);
    when(mockPatch.getKey()).thenReturn(Optional.of(mockGeoKey));
    List<Entity> patches = Arrays.asList(mockPatch);
    when(mockReplicate.query(any(Query.class))).thenReturn(patches);
    when(mockReplicate.getPatchByKey(eq(mockGeoKey), eq(-1L))).thenReturn(mockPatch);

    // First call - should query and cache
    bridge.getPriorPatches(mockGeometryMomento);

    // Second call - should use cache
    bridge.getPriorPatches(mockGeometryMomento);

    // Verify
    verify(mockReplicate, times(1)).query(any(Query.class));
    verify(mockGeometryMomento, times(1)).build();
    verify(mockReplicate, times(1)).getPatchByKey(eq(mockGeoKey), eq(-1L));
  }
}
