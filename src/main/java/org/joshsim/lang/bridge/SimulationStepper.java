package org.joshsim.lang.bridge;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Operation on an EngineBridge which completes a full step in a simulation.
 */
public class SimulationStepper {

  private final EngineBridge target;

  /**
   * Create a new stepper around a bridge.
   *
   * @param target EngineBridge in which to perform this operation.
   */
  public SimulationStepper(EngineBridge target) {
    this.target = target;
  }

  /**
   * Operation to take a setp within an EngineBridge.
   *
   * @return The timestep completed.
   */
  public long perform() {
    target.startStep();

    boolean isFirstStep = target.getAbsoluteTimestep() == 0;
    MutableEntity simulation = target.getSimulation();
    Iterable<ShadowingEntity> patches = target.getCurrentPatches();
    Iterable<MutableEntity> entities = new SimAndPatchIterable(simulation, patches);
    
    Stream<MutableEntity> entityStream = StreamSupport.stream(entities.spliterator(), true);

    Stream<MutableEntity> initalizedStream;
    if (isFirstStep) {
      initalizedStream = entityStream.map((x) -> updateEntity(x, "init"));
    } else {
      initalizedStream = entityStream;
    }

    Stream<MutableEntity> steppedStream = initalizedStream.map((x) -> updateEntity(x, "start"))
        .map((x) -> updateEntity(x, "step"))
        .map((x) -> updateEntity(x, "end"));

    long numCompleted = steppedStream.filter((x) -> x != null).count();
    assert numCompleted > 0;

    long timestepCompleted = target.getCurrentTimestep();
    target.endStep();

    return timestepCompleted;
  }

  /**
   * Updates the target shadowing entity based on the specified sub-step.
   *
   * @param target the shadowing entity to update
   * @param subStep the sub-step name, which can be "start", "step", or "end"
   * @return the updated shadowing entity
   */
  private MutableEntity updateEntity(MutableEntity target, String subStep) {
    Iterable<String> attributeNames = target.getAttributeNames();
    
    Stream<EventKey> eventKeysNoState = StreamSupport.stream(attributeNames.spliterator(), false)
        .map((name) -> new EventKey(name, subStep));

    Stream<EventKey> eventKeys;
    Optional<EngineValue> state = target.getAttributeValue("state");
    if (state.isPresent()) {
      String stateRealized = state.get().getAsString();
      Stream<EventKey> eventKeysState = StreamSupport.stream(attributeNames.spliterator(), false)
          .map((name) -> new EventKey(stateRealized, name, subStep));

      eventKeys = Stream.concat(eventKeysNoState, eventKeysState);
    } else {
      eventKeys = eventKeysNoState;
    }

    eventKeys.map((x) -> target.getEventHandlers(x))
        .filter((x) -> x.isPresent())
        .map((x) -> x.get())
        .map((x) -> x.getEventKey().getAttribute())
        .distinct()
        .map((x) -> target.getAttributeValue(x));

    return target;
  }

  /**
   * Iterable that first returns the simulation entity and then iterates through patches.
   */
  private static class SimAndPatchIterable implements Iterable<MutableEntity> {
    private final MutableEntity simulation;
    private final Iterable<ShadowingEntity> patches;

    /**
     * Create an iterable over a simulation followed by patches.
     *
     * @param simulation The simulation to return first before returning patches.
     * @param patches The patches to return after returning the simulation.
     */
    public SimAndPatchIterable(MutableEntity simulation, Iterable<ShadowingEntity> patches) {
      this.simulation = simulation;
      this.patches = patches;
    }

    @Override
    public Iterator<MutableEntity> iterator() {
      return new Iterator<MutableEntity>() {
        private boolean simulationReturned = false;
        private final Iterator<ShadowingEntity> patchIterator = patches.iterator();

        @Override
        public boolean hasNext() {
          return !simulationReturned || patchIterator.hasNext();
        }

        @Override
        public MutableEntity next() {
          if (!simulationReturned) {
            simulationReturned = true;
            return simulation;
          }
          return patchIterator.next();
        }
      };
    }
  }

}
