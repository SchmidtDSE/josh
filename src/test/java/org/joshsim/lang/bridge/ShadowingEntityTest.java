/**
 * Tests for ShadowingEntity.
 *
 * @license BSD-3-Clause.
 */

package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.handler.EventHandler;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.entity.type.Patch;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.CompiledSelector;
import org.joshsim.engine.simulation.Simulation;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.engine.value.type.LanguageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for the shadowing entity decorator which deals with queries and progressive creation.
 */
@ExtendWith(MockitoExtension.class)
public class ShadowingEntityTest {

  @Mock(lenient = true) private Patch mockPatch;
  @Mock(lenient = true) private MutableEntity mockSpatialEntity;
  @Mock(lenient = true) private Simulation mockSimulation;
  @Mock(lenient = true) private EventHandlerGroup mockEventHandlerGroup;
  @Mock(lenient = true) private EventHandler mockEventHandler;
  @Mock(lenient = true) private EngineValue mockEngineValue;

  private ShadowingEntity patchEntity;
  private ShadowingEntity spatialEntity;

  @BeforeEach
  void setUp() {
    when(mockEventHandler.getAttributeName()).thenReturn("testAttr");
    when(mockEventHandlerGroup.getEventHandlers()).thenReturn(Arrays.asList(mockEventHandler));
    when(mockPatch.getEventHandlers()).thenReturn(Arrays.asList(mockEventHandlerGroup));
    when(mockSpatialEntity.getEventHandlers()).thenReturn(Arrays.asList(mockEventHandlerGroup));
    when(mockSpatialEntity.getAttributeNames()).thenReturn(
        Set.of("testAttr", "noHandlerAttr")
    );
    when(mockEngineValue.getLanguageType()).thenAnswer(x -> new LanguageType("test", false));

    EngineValueFactory valueFactory = new EngineValueFactory();
    patchEntity = new ShadowingEntity(valueFactory, mockPatch, mockSimulation);
    spatialEntity = new ShadowingEntity(
        valueFactory,
        mockSpatialEntity,
        patchEntity,
        mockSimulation
    );
  }

  @Test
  void testSetAttribute() {
    String attrName = "testAttr";

    when(mockSpatialEntity.getAttributeValue(attrName)).thenReturn(Optional.of(mockEngineValue));

    Optional<EngineValue> priorValue = spatialEntity.getPriorAttribute(attrName);
    assertEquals(mockEngineValue, priorValue.get());

    spatialEntity.setAttributeValue(attrName, mockEngineValue);
    verify(mockSpatialEntity).setAttributeValue(attrName, mockEngineValue);
  }

  @Test
  void testGetHandlersFailsOutsideSubstep() {
    assertThrows(
        IllegalStateException.class,
        () -> spatialEntity.getHandlersForAttribute("testAttr")
    );
  }

  @Test
  void testGetCurrentAttributeResolved() {
    String attrName = "testAttr";
    String substepName = "test";
    when(mockSpatialEntity.getAttributeValue(attrName)).thenReturn(Optional.of(mockEngineValue));

    spatialEntity.startSubstep(substepName);
    spatialEntity.setAttributeValue(attrName, mockEngineValue);
    Optional<EngineValue> result = spatialEntity.getAttributeValue(attrName);

    assertFalse(result.isEmpty());
    spatialEntity.endSubstep();
  }

  @Test
  void testPatchAccessors() {
    assertEquals(patchEntity, spatialEntity.getHere());
  }

  @Test
  void testSimulationAccessors() {
    assertEquals(mockSimulation, spatialEntity.getMeta());
  }

  @Test
  void testNonexistentAttributeAccess() {
    String nonexistentAttr = "nonexistent";
    assertThrows(IllegalArgumentException.class, () ->
        spatialEntity.setAttributeValue(nonexistentAttr, mockEngineValue));
    assertThrows(IllegalArgumentException.class, () ->
        spatialEntity.getPriorAttribute(nonexistentAttr));
  }

  @Test
  void testResolvePriorValueWhenNoHandlers() {
    String attrName = "noHandlerAttr";
    when(mockSpatialEntity.getAttributeNames()).thenReturn(Set.of(attrName));
    when(mockSpatialEntity.getAttributeValue(attrName)).thenReturn(Optional.of(mockEngineValue));

    spatialEntity.startSubstep("test");
    Optional<EngineValue> result = spatialEntity.getAttributeValue(attrName);

    assertTrue(result.isPresent());
    assertEquals(mockEngineValue, result.get());
    spatialEntity.endSubstep();
  }

  @Test
  void testResolveValueThroughEventHandlerGroup() {
    String attrName = "testAttr";
    String substepName = "test";

    EngineValue handlerValue = mock(EngineValue.class);

    EventKey eventKey = EventKey.of(attrName, substepName);
    CompiledCallable mockCallable = mock(CompiledCallable.class);
    CompiledSelector mockSelector = mock(CompiledSelector.class);
    when(mockEventHandler.getCallable()).thenReturn(mockCallable);
    when(mockEventHandler.getConditional()).thenReturn(Optional.of(mockSelector));
    when(mockSpatialEntity.getEventHandlers(eventKey)).thenReturn(
        Optional.of(mockEventHandlerGroup)
    );
    when(mockEventHandlerGroup.getEventHandlers()).thenReturn(Arrays.asList(mockEventHandler));
    when(mockSpatialEntity.getAttributeValue(attrName))
        .thenReturn(Optional.of(handlerValue));

    spatialEntity.startSubstep(substepName);
    Optional<EngineValue> result = spatialEntity.getAttributeValue(attrName);

    assertTrue(result.isPresent());
    spatialEntity.endSubstep();
  }

  @Test
  void testFastPathForAttributeWithNoHandlersInSubstep() {
    // Setup: Use existing noHandlerAttr which is already in attribute names
    String attrName = "noHandlerAttr";
    String substepName = "init";

    // Mock hasNoHandlers to return true for init
    when(mockSpatialEntity.hasNoHandlers(attrName, substepName)).thenReturn(true);

    // Mock prior value
    EngineValue priorValue = mock(EngineValue.class);
    when(mockSpatialEntity.getAttributeValue(attrName)).thenReturn(Optional.of(priorValue));

    // Start init substep and resolve attribute
    spatialEntity.startSubstep(substepName);
    Optional<EngineValue> result = spatialEntity.getAttributeValue(attrName);

    // Verify fast path was taken - should resolve from prior
    assertTrue(result.isPresent(), "Should resolve from prior");

    spatialEntity.endSubstep();
  }

  @Test
  void testSlowPathForAttributeWithHandlersInSubstep() {
    // Setup: Use existing testAttr which has handlers
    String attrName = "testAttr";
    String substepName = "step";

    // Mock hasNoHandlers to return false for step (has handler)
    when(mockSpatialEntity.hasNoHandlers(attrName, substepName)).thenReturn(false);

    // Mock handler setup
    EventKey eventKey = EventKey.of(attrName, substepName);
    EngineValue handlerValue = mock(EngineValue.class);

    when(mockSpatialEntity.getEventHandlers(eventKey)).thenReturn(
        Optional.of(mockEventHandlerGroup)
    );
    when(mockEventHandlerGroup.getEventHandlers()).thenReturn(Arrays.asList(mockEventHandler));
    when(mockEventHandler.getAttributeName()).thenReturn(attrName);
    when(mockSpatialEntity.getAttributeValue(attrName))
        .thenReturn(Optional.of(handlerValue));

    // Start step substep and resolve attribute
    spatialEntity.startSubstep(substepName);
    Optional<EngineValue> result = spatialEntity.getAttributeValue(attrName);

    // Should take slow path and resolve through handlers
    assertTrue(result.isPresent());

    spatialEntity.endSubstep();
  }

  @Test
  void testGetPriorAttributeByIndex() {
    // Setup
    Map<String, Integer> indexMap = Map.of("testAttr", 0);
    when(mockSpatialEntity.getAttributeNameToIndex()).thenReturn(indexMap);
    when(mockSpatialEntity.getAttributeValue(0)).thenReturn(Optional.of(mockEngineValue));

    // Test integer-based prior attribute access
    Optional<EngineValue> result = spatialEntity.getPriorAttribute(0);

    assertTrue(result.isPresent());
    assertEquals(mockEngineValue, result.get());
  }

  @Test
  void testResolveFromPriorByIndexMatchesStringVersion() {
    // This test verifies that integer and string paths produce same results
    String attrName = "testAttr";
    Map<String, Integer> indexMap = Map.of(attrName, 0);

    when(mockSpatialEntity.getAttributeNameToIndex()).thenReturn(indexMap);
    when(mockSpatialEntity.getAttributeValue(attrName)).thenReturn(Optional.of(mockEngineValue));
    when(mockSpatialEntity.getAttributeValue(0)).thenReturn(Optional.of(mockEngineValue));
    when(mockSpatialEntity.hasNoHandlers(attrName, "test")).thenReturn(true);

    // Resolve via string path
    spatialEntity.startSubstep("test");
    final Optional<EngineValue> resultString = spatialEntity.getAttributeValue(attrName);
    spatialEntity.endSubstep();

    // Clear resolved cache
    spatialEntity.startSubstep("test");

    // Resolve via integer path (getAttributeValue(int) will use same logic)
    Optional<EngineValue> resultInt = spatialEntity.getAttributeValue(0);
    spatialEntity.endSubstep();

    assertEquals(resultString, resultInt);
  }
}
