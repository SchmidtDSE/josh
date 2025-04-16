package org.joshsim.lang.bridge;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Operation on an EngineBridge which completes a full step in a simulation.
 */
public class SimulationStepper {

  private final EngineBridge target;
  private final Set<String> events;

  /**
   * Create a new stepper around a bridge.
   *
   * @param target EngineBridge in which to perform this operation.
   */
  public SimulationStepper(EngineBridge target) {
    this.target = target;

    MutableEntity simulation = target.getSimulation();
    Iterable<MutableEntity> patches = target.getCurrentPatches();

    events = StreamSupport.stream(patches.spliterator(), false)
        .flatMap((x) -> StreamSupport.stream(x.getEventHandlers().spliterator(), false))
        .map(EventHandlerGroup::getEventKey)
        .map(EventKey::getEvent)
        .collect(Collectors.toSet());
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
    Iterable<MutableEntity> patches = target.getCurrentPatches();

    if (isFirstStep) {
      performStream(simulation, "init");
      performStream(patches, "init");
    }

    if (events.contains("start")) {
      performStream(simulation, "start");
      performStream(patches, "start");
    }

    if (events.contains("step")) {
      performStream(simulation, "step");
      performStream(patches, "step");
    }

    if (events.contains("end")) {
      performStream(simulation, "end");
      performStream(patches, "end");
    }

    long timestepCompleted = target.getCurrentTimestep();
    target.endStep();

    System.gc();

    return timestepCompleted;
  }

  /**
   * Performs a series of entity updates on a stream of entities from an iterable.
   *
   * @param entities the iterable of entities to perform updates on
   * @param subStep the substep to perform
   */
  private void performStream(Iterable<MutableEntity> entities, String subStep) {
    Stream<MutableEntity> entityStream = StreamSupport.stream(entities.spliterator(), false);
    performStream(entityStream, subStep);
  }

  /**
   * Performs a series of entity updates on a single entity.
   *
   * @param entity the entity to perform updates on.
   * @param subStep the substep to perform
   */
  private void performStream(MutableEntity entity, String subStep) {
    performStream(Stream.of(entity), subStep);
  }

  /**
   * Performs a series of entity updates on a stream of entities.
   *
   * @param entityStream the stream of entities to perform updates on
   * @param subStep the substep to perform
   */
  private void performStream(Stream<MutableEntity> entityStream, String subStep) {
    Stream<MutableEntity> steppedStream = entityStream.map((x) -> updateEntity(x, subStep));
    long numCompleted = steppedStream.filter((x) -> x != null).count();
    assert numCompleted > 0;
  }

  /**
   * Updates the target shadowing entity based on the specified sub-step.
   *
   * @param target the shadowing entity to update
   * @param subStep the sub-step name, which can be "start", "step", or "end"
   * @return the updated shadowing entity
   */
  private MutableEntity updateEntity(MutableEntity target, String subStep) {
    target.startSubstep(subStep);
    updateEntityUnsafe(target);
    target.endSubstep();

    return target;
  }

  private void updateEntityUnsafe(MutableEntity target) {
    target.getAttributeNames().stream()
      .map(target::getAttributeValue)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .filter((x) -> x.getLanguageType().containsAttributes())
      .forEach((x) -> {
        Optional<Integer> sizeMaybe = x.getSize();
        if (sizeMaybe.isEmpty()) {
          return;
        }

        int size = sizeMaybe.get();
        if (size == 1) {
          updateEntityUnsafe(x.getAsMutableEntity());
        } else {
          Iterable<EngineValue> values = x.getAsDistribution().getContents(size, false);
          for (EngineValue value : values) {
            updateEntityUnsafe(value.getAsMutableEntity());
          }
        }
      });
  }

}
