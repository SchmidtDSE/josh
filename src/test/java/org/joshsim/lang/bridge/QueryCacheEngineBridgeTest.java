
package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.joshsim.engine.entity.Patch;
import org.joshsim.engine.entity.PatchKey;
import org.joshsim.engine.entity.Simulation;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.simulation.Query;
import org.joshsim.engine.simulation.Replicate;
import org.joshsim.engine.value.Converter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class QueryCacheEngineBridgeTest {

    @Mock private Simulation mockSimulation;
    @Mock private Replicate mockReplicate;
    @Mock private Converter mockConverter;
    @Mock private Patch mockPatch;
    @Mock private Geometry mockGeometry;
    @Mock private GeometryMomento mockGeometryMomento;
    @Mock private PatchKey mockPatchKey;

    private QueryCacheEngineBridge bridge;

    @BeforeEach
    void setUp() {
        bridge = new QueryCacheEngineBridge(mockSimulation, mockReplicate, mockConverter);
    }

    @Test
    void testGetPriorPatchesWithoutCache() {
        // Setup
        when(mockGeometryMomento.build()).thenReturn(mockGeometry);
        when(mockPatch.getPatchKey()).thenReturn(mockPatchKey);
        List<Patch> patches = Arrays.asList(mockPatch);
        when(mockReplicate.query(any(Query.class))).thenReturn(patches);
        when(mockReplicate.getPatchByKey(eq(mockPatchKey), eq(-1L))).thenReturn(mockPatch);

        // First call - should query and cache
        Iterable<ShadowingEntity> result = bridge.getPriorPatches(mockGeometryMomento);

        // Verify
        verify(mockReplicate, times(1)).query(any(Query.class));
        verify(mockGeometryMomento, times(1)).build();
    }

    @Test
    void testGetPriorPatchesWithCache() {
        // Setup
        when(mockGeometryMomento.build()).thenReturn(mockGeometry);
        when(mockPatch.getPatchKey()).thenReturn(mockPatchKey);
        List<Patch> patches = Arrays.asList(mockPatch);
        when(mockReplicate.query(any(Query.class))).thenReturn(patches);
        when(mockReplicate.getPatchByKey(eq(mockPatchKey), eq(-1L))).thenReturn(mockPatch);

        // First call - should query and cache
        bridge.getPriorPatches(mockGeometryMomento);

        // Second call - should use cache
        bridge.getPriorPatches(mockGeometryMomento);

        // Verify
        verify(mockReplicate, times(1)).query(any(Query.class));
        verify(mockGeometryMomento, times(1)).build();
        verify(mockReplicate, times(2)).getPatchByKey(eq(mockPatchKey), eq(-1L));
    }
}
