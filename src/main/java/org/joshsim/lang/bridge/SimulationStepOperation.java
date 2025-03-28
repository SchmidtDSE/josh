package org.joshsim.lang.bridge;

import org.joshsim.engine.entity.Patch;
import org.joshsim.engine.entity.Simulation;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.value.EngineValue;

import java.util.Optional;
import java.util.stream.StreamSupport;


public class SimulationStepOperation implements EngineBridgeOperation {

  private final EntityPrototype prototype;

  public SimulationStepOperation(EntityPrototype prototype) {
    this.prototype = prototype;
  }

  @Override
  public Optional<EngineValue> perform(EngineBridge target) {
    target.startStep();

    if (target.getAbsoluteTimestep() == 0) {
      initSimulation();
    }

    Iterable<ShadowingEntity> patches = target.getCurrentPatches();
    StreamSupport.stream(patches.spliterator(), true)
        .map((x) -> updateEntity(x, "start"))
        .map((x) -> updateEntity(x, "step"))
        .map((x) -> updateEntity(x, "end"))
        .map(ShadowingEntity::freeze);

    target.endStep();

    return Optional.empty();  // TODO
  }

  private ShadowingEntity updateEntity(ShadowingEntity target, String subStep) {
    return target;
  }

  private void initSimulation() {

  }
}
