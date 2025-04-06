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
  public Iterable<Entity> perform(EngineBridge target) {
    target.startStep();

    if (target.getAbsoluteTimestep() == 0) {
      initSimulation();
    }

    ShadowingEntity simulation = target.getSimulation();
    Iterable<ShadowingEntity> patches = target.getCurrentPatches();
    
    List<Entity> results = StreamSupport.stream(new SimAndPatchIterable(simulation, patches), true)
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
  private ShadowingEntity updateEntity(ShadowingEntity target, String subStep) {
    // TODO
    return target;
  }

  /**
   * Set up necessary state or configuration before simulation start.
   */
  private void initSimulation() {
    // TODO
  }

  

}



  /**
   * Iterable that first returns the simulation entity and then iterates through patches.
   */
  private static class SimAndPatchIterable implements Iterable<ShadowingEntity> {
    private final ShadowingEntity simulation;
    private final Iterable<ShadowingEntity> patches;

    public SimAndPatchIterable(ShadowingEntity simulation, Iterable<ShadowingEntity> patches) {
      this.simulation = simulation;
      this.patches = patches;
    }

    @Override
    public Iterator<ShadowingEntity> iterator() {
      return new Iterator<ShadowingEntity>() {
        private boolean simulationReturned = false;
        private final Iterator<ShadowingEntity> patchIterator = patches.iterator();

        @Override
        public boolean hasNext() {
          return !simulationReturned || patchIterator.hasNext();
        }

        @Override
        public ShadowingEntity next() {
          if (!simulationReturned) {
            simulationReturned = true;
            return simulation;
          }
          return patchIterator.next();
        }
      };
    }
  }
