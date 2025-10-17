
/**
 * Tests for EngineBridge.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.joshsim.engine.config.Config;
import org.joshsim.engine.config.NoOpConfigGetter;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototypeStore;
import org.joshsim.engine.entity.type.Patch;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.EnginePoint;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.simulation.Query;
import org.joshsim.engine.simulation.Replicate;
import org.joshsim.engine.simulation.Simulation;
import org.joshsim.engine.value.converter.Converter;
import org.joshsim.engine.value.converter.DirectConversion;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.precompute.DataGridLayer;
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
  @Mock(lenient = true) private EnginePoint mockEnginePoint;
  @Mock(lenient = true) private EngineGeometry mockGeometry;
  @Mock(lenient = true) private EntityPrototypeStore mockPrototypeStore;
  @Mock(lenient = true) private ExternalResourceGetter mockExternalResourceGetter;

  private EngineBridge bridge;

  /**
   * Create a bridge to test before each.
   */
  @BeforeEach
  void setUp() {
    EngineGeometryFactory geometryFactory = new GridGeometryFactory();
    bridge = new MinimalEngineBridge(
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
    expectQuery(new Query(0, mockEnginePoint), Arrays.asList(mockPatch));

    Optional<Entity> result = bridge.getPatch(mockEnginePoint);
    assertTrue(result.isPresent(), "Should return a patch");
  }

  @Test
  void testGetPatchThrowsOnNoPatch() {
    expectQuery(new Query(0, mockEnginePoint), Arrays.asList());

    assertThrows(
        IllegalStateException.class,
        () -> bridge.getPatch(mockEnginePoint),
        "Should throw when no patch found"
    );
  }

  @Test
  void testGetPatchThrowsOnMultiplePatches() {
    expectQuery(new Query(0, mockEnginePoint), Arrays.asList(mockPatch, mockPatch));

    assertThrows(
        IllegalStateException.class,
        () -> bridge.getPatch(mockEnginePoint),
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
    List<Entity> results = bridge.getPriorPatches(mockGeometry);
    assertTrue(results.iterator().hasNext(), "Should return prior patches");
  }

  @Test
  void testValueConversion() {
    Units oldUnits = Units.of("old");
    Units newUnits = Units.of("test");
    when(mockEngineValue.getUnits()).thenReturn(oldUnits);
    when(mockEngineValueConverted.replaceUnits(any())).thenReturn(mockEngineValueConverted);
    when(mockConverter.getConversion(mockEngineValue.getUnits(), newUnits))
        .thenReturn(new DirectConversion(newUnits, newUnits, (x) -> mockEngineValueConverted));

    EngineValue result = bridge.convert(mockEngineValue, newUnits);
    assertEquals(mockEngineValueConverted, result, "Should return converted value");
  }

  @Test
  void testGetConfigWithExtensionHandling() {
    // Setup
    ConfigGetter mockConfigGetter = mock(ConfigGetter.class);
    Config mockConfig = mock(Config.class);
    EngineValueFactory engineValueFactory = new EngineValueFactory();
    EngineValue mockValue = engineValueFactory.build(5.0, Units.of("meters"));

    when(mockConfig.getValue("testVar")).thenReturn(mockValue);
    when(mockConfigGetter.getConfig("test.jshc")).thenReturn(Optional.of(mockConfig));

    EngineBridge bridgeWithConfig = new MinimalEngineBridge(
        new EngineValueFactory(),
        new GridGeometryFactory(),
        mockSimulation,
        mockConverter,
        mockPrototypeStore,
        mockExternalResourceGetter,
        mockConfigGetter,
        mockReplicate
    );

    // Execute - note: Josh code uses "config test.testVar" without extension
    Optional<EngineValue> result = bridgeWithConfig.getConfigOptional("test.testVar");

    // Verify - bridge should append .jshc before calling getter
    assertTrue(result.isPresent());
    assertEquals(5.0, result.get().getAsDouble(), 0.001);
    verify(mockConfigGetter).getConfig("test.jshc");
  }

  @Test
  void testGetExternalWithExtensionHandling() {
    // Setup
    ExternalResourceGetter mockExternalGetter = mock(ExternalResourceGetter.class);
    DataGridLayer mockLayer = mock(DataGridLayer.class);
    EngineValueFactory engineValueFactory = new EngineValueFactory();
    EngineValue mockValue = engineValueFactory.build(10.0, Units.of("mm"));
    GeoKey mockKey = mock(GeoKey.class);

    when(mockLayer.getAt(mockKey, 0L)).thenReturn(mockValue);
    when(mockExternalGetter.getResource("Precipitation.jshd")).thenReturn(mockLayer);

    EngineBridge bridgeWithExternal = new MinimalEngineBridge(
        new EngineValueFactory(),
        new GridGeometryFactory(),
        mockSimulation,
        mockConverter,
        mockPrototypeStore,
        mockExternalGetter,
        new NoOpConfigGetter(),
        mockReplicate
    );

    // Execute - note: Josh code uses "external Precipitation" without extension
    EngineValue result = bridgeWithExternal.getExternal(mockKey, "Precipitation", 0L);

    // Verify - bridge should append .jshd before calling getter
    assertEquals(10.0, result.getAsDouble(), 0.001);
    verify(mockExternalGetter).getResource("Precipitation.jshd");
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
