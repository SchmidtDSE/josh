package org.joshsim.lang.bridge;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
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
    Iterable<MutableEntity> patches = target.getCurrentPatches();

    Stream<MutableEntity> simStream = Stream.of(simulation);
    Stream<MutableEntity> entityStream = StreamSupport.stream(patches.spliterator(), true);

    performStream(simStream, isFirstStep);
    performStream(entityStream, isFirstStep);

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
    target.startSubstep(subStep);

    StreamSupport.stream(target.getAttributeNames().spliterator(), false)
        .map((x) -> {
          return target.getAttributeValue(x);
        })
        .filter((x) -> x.isPresent())
        .map((x) -> x.get())
        .filter((x) -> x.getLanguageType().containsAttributes())
        .forEach((x) -> {
          Optional<Integer> sizeMaybe = x.getSize();
          if (sizeMaybe.isEmpty()) {
            return;
          }

          int size = sizeMaybe.get();
          if (size == 1) {
            updateEntity(x.getAsMutableEntity(), subStep);
          } else {
            Iterable<EngineValue> values = x.getAsDistribution().getContents(size, false);
            for (EngineValue value : values) {
              updateEntity(value.getAsMutableEntity(), subStep);
            }
          }
        });

    target.endSubstep();
    
    return target;
  }

  
  /**
   * Performs a series of entity updates on a stream of entities.
   *
   * <p>If it is the first step of the simulation, each entity in the stream
   * will be initialized. The stream is processed in parallel using Java Streams.
   * Each entity undergoes a sequence of updates corresponding to sub-steps
   * "start", "step", and "end".</p>
   *
   * @param entityStream the stream of entities to perform updates on
   * @param isFirstStep boolean indicating if this is the first step of the simulation
   */
  private void performStream(Stream<MutableEntity> entityStream, boolean isFirstStep) {
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
  }

}
