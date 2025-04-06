package org.joshsim.lang.bridge;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.joshsim.engine.entity.base.Entity;


/**
 * Operation on an EngineBridge which completes a full step in a simulation.
 */
public class SimulationStepper {

  /**
   * Operation to take a setp within an EngineBridge.
   *
   * @param target EngineBridge in which to perform this operation.
   * @return Iterable of frozen patches from the just completed timestep.
   */
  public static Iterable<Entity> perform(EngineBridge target) {
    target.startStep();

    if (target.getAbsoluteTimestep() == 0) {
      initSimulation();
    }

    Iterable<ShadowingEntity> patches = target.getCurrentPatches();
    List<Entity> results = StreamSupport.stream(patches.spliterator(), true)
        .map((x) -> updateEntity(x, "start"))
        .map((x) -> updateEntity(x, "step"))
        .map((x) -> updateEntity(x, "end"))
        .map(ShadowingEntity::freeze)
        .collect(Collectors.toList());

    target.endStep();

    return results;
  }

  /**
   * Updates the target shadowing entity based on the specified sub-step.
   *
   * @param target the shadowing entity to update
   * @param subStep the sub-step name, which can be "start", "step", or "end"
   * @return the updated shadowing entity
   */
  private static ShadowingEntity updateEntity(ShadowingEntity target, String subStep) {
    // TODO
    return target;
  }

  /**
   * Set up necessary state or configuration before simulation start.
   */
  private static void initSimulation() {
    // TODO
  }
}
