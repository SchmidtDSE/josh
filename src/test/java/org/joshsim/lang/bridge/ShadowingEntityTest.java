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
import java.util.Optional;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.handler.EventHandler;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.entity.type.Patch;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.CompiledSelector;
import org.joshsim.engine.simulation.Simulation;
import org.joshsim.engine.value.type.EngineValue;
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
        Arrays.asList("testAttr", "noHandlerAttr")
    );

    patchEntity = new ShadowingEntity(mockPatch, mockSimulation);
    spatialEntity = new ShadowingEntity(mockSpatialEntity, patchEntity, mockSimulation);
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
    verify(mockSpatialEntity).getAttributeValue(attrName);
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
    when(mockSpatialEntity.getAttributeNames()).thenReturn(Arrays.asList(attrName));
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

    EventKey eventKey = new EventKey(attrName, substepName);
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
}
