
/**
 * Tests for EngineBridge.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import org.joshsim.engine.entity.Patch;
import org.joshsim.engine.entity.Simulation;
import org.joshsim.engine.geometry.GeoPoint;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.simulation.Query;
import org.joshsim.engine.simulation.Replicate;
import org.joshsim.engine.value.Converter;
import org.joshsim.engine.value.EngineValue;
import org.joshsim.engine.value.EngineValueFactory;
import org.joshsim.engine.value.Units;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EngineBridgeTest {

  @Mock private Simulation mockSimulation;
  @Mock private Replicate mockReplicate;
  @Mock private Converter mockConverter;
  @Mock private Patch mockPatch;
  @Mock private EngineValue mockEngineValue;
  @Mock private GeoPoint mockPoint;
  @Mock private Geometry mockGeometry;

  private EngineBridge bridge;

  @BeforeEach
  void setUp() {
    bridge = new EngineBridge(mockSimulation, mockReplicate, mockConverter);
  }

  @Test
  void testStepLifecycle() {
    // Test starting a step
    bridge.startStep();
    assertThrows(IllegalStateException.class, () -> bridge.startStep(),
        "Should not be able to start step twice");

    // Test ending a step
    bridge.endStep();
    assertThrows(IllegalStateException.class, () -> bridge.endStep(),
        "Should not be able to end step twice");
  }

  @Test
  void testGetPatch() {
    when(mockReplicate.query(new Query(0, mockPoint)))
        .thenReturn(Arrays.asList(mockPatch));

    Optional<ShadowingEntity> result = bridge.getPatch(mockPoint);
    assertTrue(result.isPresent(), "Should return a patch");
  }

  @Test
  void testGetPatchThrowsOnNoPatch() {
    when(mockReplicate.query(new Query(0, mockPoint)))
        .thenReturn(Arrays.asList());

    assertThrows(IllegalStateException.class, () -> bridge.getPatch(mockPoint),
        "Should throw when no patch found");
  }

  @Test
  void testGetPatchThrowsOnMultiplePatches() {
    when(mockReplicate.query(new Query(0, mockPoint)))
        .thenReturn(Arrays.asList(mockPatch, mockPatch));

    assertThrows(IllegalStateException.class, () -> bridge.getPatch(mockPoint),
        "Should throw when multiple patches found");
  }

  @Test
  void testGetCurrentPatches() {
    when(mockReplicate.query(new Query(0)))
        .thenReturn(Arrays.asList(mockPatch));

    Iterable<ShadowingEntity> results = bridge.getCurrentPatches();
    assertTrue(results.iterator().hasNext(), "Should return patches");
  }

  @Test
  void testGetPriorPatches() {
    when(mockReplicate.query(new Query(-1, mockGeometry)))
        .thenReturn(Arrays.asList(mockPatch));

    Iterable<ShadowingEntity> results = bridge.getPriorPatches(mockGeometry);
    assertTrue(results.iterator().hasNext(), "Should return prior patches");
  }

  @Test
  void testValueConversion() {
    Units newUnits = new Units("test");
    when(mockEngineValue.getUnits()).thenReturn(new Units("old"));
    when(mockConverter.getConversion(mockEngineValue.getUnits(), newUnits))
        .thenReturn(scope -> mockEngineValue);

    EngineValue result = bridge.convert(mockEngineValue, newUnits);
    assertEquals(mockEngineValue, result, "Should return converted value");
  }
}
