
/**
 * Tests for EngineBridge.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import org.joshsim.engine.entity.base.MutableEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.prototype.EntityPrototypeStore;
import org.joshsim.engine.entity.type.Patch;
import org.joshsim.engine.geometry.GeoPoint;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.simulation.Query;
import org.joshsim.engine.simulation.Replicate;
import org.joshsim.engine.simulation.Simulation;
import org.joshsim.engine.value.converter.Converter;
import org.joshsim.engine.value.converter.DirectConversion;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


/**
 * Test for EngineBridge which helps decouple the engine from interpreter.
 */
@ExtendWith(MockitoExtension.class)
public class MinimalEngineBridgeTest {

  @Mock(lenient = true) private Simulation mockSimulation;
  @Mock(lenient = true) private Replicate mockReplicate;
  @Mock(lenient = true) private Converter mockConverter;
  @Mock(lenient = true) private Patch mockPatch;
  @Mock(lenient = true) private EngineValue mockEngineValue;
  @Mock(lenient = true) private EngineValue mockEngineValueConverted;
  @Mock(lenient = true) private GeoPoint mockPoint;
  @Mock(lenient = true) private Geometry mockGeometry;
  @Mock(lenient = true) private EntityPrototypeStore mockPrototypeStore;

  private EngineBridge bridge;

  /**
   * Create a bridge to test before each.
   */
  @BeforeEach
  void setUp() {
    bridge = new MinimalEngineBridge(
        mockSimulation,
        mockConverter,
        mockPrototypeStore,
        mockReplicate
    );
  }

  @Test
  void testStepLifecycle() {
    // Test starting a step
    bridge.startStep();
    assertThrows(
        IllegalStateException.class, () -> bridge.startStep(),
        "Should not be able to start step twice"
    );

    // Test ending a step
    bridge.endStep();
    assertThrows(
        IllegalStateException.class,
        () -> bridge.endStep(),
        "Should not be able to end step twice"
    );
  }

  @Test
  void testGetPatch() {
    expectQuery(new Query(0, mockPoint), Arrays.asList(mockPatch));

    Optional<Entity> result = bridge.getPatch(mockPoint);
    assertTrue(result.isPresent(), "Should return a patch");
  }

  @Test
  void testGetPatchThrowsOnNoPatch() {
    expectQuery(new Query(0, mockPoint), Arrays.asList());

    assertThrows(
        IllegalStateException.class,
        () -> bridge.getPatch(mockPoint),
        "Should throw when no patch found"
    );
  }

  @Test
  void testGetPatchThrowsOnMultiplePatches() {
    expectQuery(new Query(0, mockPoint), Arrays.asList(mockPatch, mockPatch));

    assertThrows(
        IllegalStateException.class,
        () -> bridge.getPatch(mockPoint),
        "Should throw when multiple patches found"
    );
  }

  @Test
  void testGetCurrentPatches() {
    when(mockReplicate.getCurrentPatches()).thenReturn(Arrays.asList(mockPatch, mockPatch));

    Iterable<MutableEntity> results = bridge.getCurrentPatches();
    assertTrue(results.iterator().hasNext(), "Should return patches");
  }

  @Test
  void testGetPriorPatches() {
    expectQuery(new Query(0, mockGeometry), Arrays.asList(mockPatch));

    bridge.startStep();
    bridge.endStep();
    Iterable<Entity> results = bridge.getPriorPatches(mockGeometry);
    assertTrue(results.iterator().hasNext(), "Should return prior patches");
  }

  @Test
  void testValueConversion() {
    Units oldUnits = new Units("old");
    Units newUnits = new Units("test");
    when(mockEngineValue.getUnits()).thenReturn(oldUnits);
    when(mockConverter.getConversion(mockEngineValue.getUnits(), newUnits))
        .thenReturn(new DirectConversion(newUnits, newUnits, (x) -> mockEngineValueConverted));

    EngineValue result = bridge.convert(mockEngineValue, newUnits);
    assertEquals(mockEngineValueConverted, result, "Should return converted value");
  }

  private void expectQuery(Query query, List<Patch> result) {
    when(mockReplicate.query(any(Query.class))).thenAnswer(invocation -> {
      Query argument = invocation.getArgument(0);
      assert argument.getStep() == query.getStep();
      assert argument.getGeometry().equals(query.getGeometry());
      return result;
    });
  }
}
