
package org.joshsim.lang.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.engine.value.type.LanguageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for SimulationStepper which performs simulation steps through the EngineBridge.
 *
 * @license BSD-3-Clause
 */
@ExtendWith(MockitoExtension.class)
class SimulationStepperTest {

  @Mock(lenient = true) private EngineBridge mockBridge;
  @Mock(lenient = true) private MutableEntity mockSimulation;
  @Mock(lenient = true) private ShadowingEntity mockPatch;
  @Mock(lenient = true) private EventHandlerGroup mockHandlerGroup;
  @Mock(lenient = true) private EngineValue mockValue;

  private SimulationStepper stepper;

  /**
   * Sets up test environment before each test by creating mocks and initializing the stepper.
   */
  @BeforeEach
  void setUp() {
    ArrayList<MutableEntity> patches = new ArrayList<>();
    patches.add(mockPatch);

    when(mockBridge.getSimulation()).thenReturn(mockSimulation);
    when(mockBridge.getCurrentPatches()).thenReturn(patches);
    when(mockBridge.getCurrentTimestep()).thenReturn(1L);
    when(mockBridge.getAbsoluteTimestep()).thenReturn(0L);
    when(mockBridge.getSubstepOrder()).thenReturn(List.of("start", "step", "end"));

    stepper = new SimulationStepper(mockBridge);
  }

  @Test
  void performSuccessfullyCompletesStep() {
    // Setup mock behavior
    Set<String> attributes = Set.of("testAttribute");

    when(mockSimulation.getAttributeNames()).thenReturn(attributes);
    when(mockPatch.getAttributeNames()).thenReturn(attributes);

    EventKey eventKey = EventKey.of("testAttribute", "init");
    when(mockSimulation.getEventHandlers(eventKey)).thenReturn(Optional.of(mockHandlerGroup));
    when(mockPatch.getEventHandlers(eventKey)).thenReturn(Optional.of(mockHandlerGroup));

    when(mockHandlerGroup.getEventKey()).thenReturn(eventKey);
    when(mockSimulation.getAttributeValue("testAttribute")).thenReturn(Optional.of(mockValue));
    when(mockSimulation.getAttributeValue("state")).thenReturn(Optional.empty());
    when(mockPatch.getAttributeValue("testAttribute")).thenReturn(Optional.of(mockValue));

    when(mockValue.getLanguageType()).thenReturn(new LanguageType("test", true));

    // Perform the step
    long result = stepper.perform(true);

    // Verify
    assertEquals(1L, result, "Step should complete and return timestep 1");
  }

  @Test
  void performRunsOnlyDeclaredCustomPhasesThatAreActuallyUsed() {
    // A patch handler uses "disturbance" but not "base"; the declared order lists both.
    EventKey disturbanceKey = EventKey.of("testAttribute", "disturbance");
    when(mockPatch.getEventHandlers()).thenReturn(List.of(mockHandlerGroup));
    when(mockHandlerGroup.getEventKey()).thenReturn(disturbanceKey);
    when(mockBridge.getSubstepOrder()).thenReturn(List.of("base", "disturbance"));

    when(mockSimulation.getAttributeNames()).thenReturn(Set.of());
    when(mockPatch.getAttributeNames()).thenReturn(Set.of());

    SimulationStepper customStepper = new SimulationStepper(mockBridge);
    customStepper.perform(true);

    verify(mockPatch, never()).startSubstep("base");
    verify(mockPatch).startSubstep("disturbance");
    verify(mockSimulation, never()).startSubstep("base");
    verify(mockSimulation).startSubstep("disturbance");
  }
}
