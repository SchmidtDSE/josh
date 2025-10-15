/**
 * Tests for a bridge which cache quries through GeometryMomentos.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.joshsim.engine.config.NoOpConfigGetter;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.prototype.EntityPrototypeStore;
import org.joshsim.engine.entity.type.Patch;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.simulation.Query;
import org.joshsim.engine.simulation.Replicate;
import org.joshsim.engine.simulation.Simulation;
import org.joshsim.engine.value.converter.Converter;
import org.joshsim.engine.value.engine.EngineValueFactory;
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
  @Mock(lenient = true) private EngineGeometry mockGeometry;
  @Mock(lenient = true) private GeometryMomento mockGeometryMomento;
  @Mock(lenient = true) private GeoKey mockGeoKey;
  @Mock(lenient = true) private ExternalResourceGetter mockExternalResourceGetter;
  @Mock(lenient = true) private EntityPrototypeStore mockPrototypeStore;

  private QueryCacheEngineBridge bridge;

  /**
   * Create an example bridge.
   */
  @BeforeEach
  void setUp() {
    EngineGeometryFactory geometryFactory = new GridGeometryFactory();
    bridge = new QueryCacheEngineBridge(
        new EngineValueFactory(),
        geometryFactory,
        mockSimulation,
        mockConverter,
        mockPrototypeStore,
        mockExternalResourceGetter,
        new NoOpConfigGetter(),
        mockReplicate
    );
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
    List<Entity> result = bridge.getPriorPatches(mockGeometryMomento);

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
