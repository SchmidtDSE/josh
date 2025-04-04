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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.handler.EventHandler;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.entity.type.Patch;
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
    when(mockSpatialEntity.getAttributeNames()).thenReturn(Arrays.asList("testAttr", "noHandlerAttr"));

    patchEntity = new ShadowingEntity(mockPatch, mockSimulation);
    spatialEntity = new ShadowingEntity(mockSpatialEntity, patchEntity, mockSimulation);
  }

  @Test
  void testSubstepLifecycle() {
    String substepName = "start";
    spatialEntity.startSubstep(substepName);
    assertThrows(IllegalStateException.class, () -> spatialEntity.startSubstep("step"));
    spatialEntity.endSubstep();
    spatialEntity.startSubstep("step");
    spatialEntity.endSubstep();
  }

  @Test
  void testSetAttribute() {
    String attrName = "testAttr";
    when(mockSpatialEntity.getAttributeValue(attrName)).thenReturn(Optional.of(mockEngineValue));

    EngineValue priorValue = spatialEntity.getPriorAttribute(attrName);
    assertEquals(mockEngineValue, priorValue);

    spatialEntity.setCurrentAttribute(attrName, mockEngineValue);
    verify(mockSpatialEntity).setAttributeValue(attrName, mockEngineValue);
  }

  @Test
  void testGetHandlersFailsOutsideSubstep() {
    assertThrows(IllegalStateException.class, () -> spatialEntity.getHandlers("testAttr"));
  }

  @Test
  void testGetHandlersDuringSubstep() {
    String attrName = "testAttr";
    String substepName = "testSubstep";

    EventKey eventKey = new EventKey(attrName, substepName);
    when(mockSpatialEntity.getEventHandlers(eventKey))
        .thenReturn(Optional.of(mockEventHandlerGroup));

    spatialEntity.startSubstep(substepName);
    Optional<EventHandlerGroup> handlers = spatialEntity.getHandlers(attrName);
    assertTrue(handlers.isPresent());
    spatialEntity.endSubstep();
  }

  @Test
  void testGetCurrentAttributeUnresolved() {
    String attrName = "testAttr";
    spatialEntity.startSubstep("test");
    assertThrows(IllegalStateException.class, () -> spatialEntity.getAttributeValue(attrName));
  }

  @Test
  void testGetCurrentAttributeResolved() {
    String attrName = "testAttr";
    String substepName = "test";
    when(mockSpatialEntity.getAttributeValue(attrName)).thenReturn(Optional.of(mockEngineValue));

    spatialEntity.startSubstep(substepName);
    spatialEntity.setCurrentAttribute(attrName, mockEngineValue);
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
        spatialEntity.setCurrentAttribute(nonexistentAttr, mockEngineValue));
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
}
