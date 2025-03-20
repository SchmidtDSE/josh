package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import org.joshsim.engine.entity.EventHandlerGroup;
import org.joshsim.engine.entity.Patch;
import org.joshsim.engine.entity.Simulation;
import org.joshsim.engine.entity.SpatialEntity;
import org.joshsim.engine.value.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public class ShadowingEntityTest {

  @Mock private Patch mockPatch;
  @Mock private SpatialEntity mockSpatialEntity;
  @Mock private Simulation mockSimulation;
  @Mock private EventHandlerGroup mockEventHandlerGroup;
  @Mock private EngineValue mockEngineValue;

  private ShadowingEntity patchEntity;
  private ShadowingEntity spatialEntity;

  @BeforeEach
  void setUp() {
    when(mockPatch.getEventHandlers()).thenReturn(Arrays.asList(mockEventHandlerGroup));
    when(mockSpatialEntity.getEventHandlers()).thenReturn(Arrays.asList(mockEventHandlerGroup));

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

    when(mockSpatialEntity.getEventHandlers(attrName, substepName))
      .thenReturn(Arrays.asList(mockEventHandlerGroup));

    spatialEntity.startSubstep(substepName);
    Iterable<EventHandlerGroup> handlers = spatialEntity.getHandlers(attrName);
    assertNotNull(handlers);
    spatialEntity.endSubstep();
  }

  @Test
  void testGetCurrentAttributeUnresolved() {
    String attrName = "testAttr";
    Optional<EngineValue> result = spatialEntity.getCurrentAttribute(attrName);
    assertTrue(result.isEmpty());
  }

  @Test
  void testGetCurrentAttributeResolved() {
    String attrName = "testAttr";
    spatialEntity.setCurrentAttribute(attrName, mockEngineValue);
    Optional<EngineValue> result = spatialEntity.getCurrentAttribute(attrName);
    assertFalse(result.isEmpty());
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
}