package org.joshsim.lang.bridge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
   * <p>Collects all unique event names from patch event handlers using direct iteration
   * instead of streams for better performance.</p>
   *
   * @param target EngineBridge in which to perform this operation.
   */
  public SimulationStepper(EngineBridge target) {
    this.target = target;

    MutableEntity simulation = target.getSimulation();
    Iterable<MutableEntity> patches = target.getCurrentPatches();

    // Collect unique event names from all patches
    Set<String> eventSet = new HashSet<>();
    for (MutableEntity patch : patches) {
      for (EventHandlerGroup group : patch.getEventHandlers()) {
        EventKey key = group.getEventKey();
        String event = key.getEvent();
        eventSet.add(event);
      }
    }
    events = eventSet;
  }

  /**
   * Operation to take a step within an EngineBridge.
   *
   * @param serialPatches If false, patches will be processed in parallel. If true, they will be
   *     processed serially.
   * @return The timestep completed.
   */
  public long perform(boolean serialPatches) {
    target.startStep();

    boolean isFirstStep = target.getAbsoluteTimestep() == 0;
    MutableEntity simulation = target.getSimulation();
    Iterable<MutableEntity> patches = target.getCurrentPatches();

    if (isFirstStep) {
      performStream(simulation, "init");
      performStream(patches, "init", serialPatches);
    }

    if (events.contains("start")) {
      performStream(simulation, "start");
      performStream(patches, "start", serialPatches);
    }

    if (events.contains("step")) {
      performStream(simulation, "step");
      performStream(patches, "step", serialPatches);
    }

    if (events.contains("end")) {
      performStream(simulation, "end");
      performStream(patches, "end", serialPatches);
    }

    long timestepCompleted = target.getCurrentTimestep();
    target.endStep();

    System.gc();

    return timestepCompleted;
  }

  /**
   * Performs a series of entity updates on entities from an iterable.
   *
   * <p>Uses direct iteration for serial execution instead of streams for better performance.
   * Parallel execution still uses streams as parallel iteration requires the stream API.</p>
   *
   * @param entities the iterable of entities to perform updates on
   * @param subStep the substep to perform
   * @param serial Flag indicating if entities should be executed in parallel. If false, will
   *     execute in parallel. Otherwise, will use serial iteration.
   */
  private void performStream(Iterable<MutableEntity> entities, String subStep, boolean serial) {
    if (serial) {
      // Use direct iteration for serial execution (better performance)
      long numCompleted = 0;
      for (MutableEntity entity : entities) {
        MutableEntity result = updateEntity(entity, subStep);
        if (result != null) {
          numCompleted++;
        }
      }
      assert numCompleted > 0;
    } else {
      // Keep parallel stream for parallel execution
      StreamSupport.stream(entities.spliterator(), true)
          .forEach(entity -> updateEntity(entity, subStep));
    }
  }

  /**
   * Performs a series of entity updates on a single entity.
   *
   * <p>Uses direct method call instead of creating a stream for a single entity.</p>
   *
   * @param entity the entity to perform updates on.
   * @param subStep the substep to perform
   */
  private void performStream(MutableEntity entity, String subStep) {
    MutableEntity result = updateEntity(entity, subStep);
    assert result != null;
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

  /**
   * Resolve all properties inside of a mutable entity, recursing to update on inner entities.
   *
   * <p>Resolve all attributes of a mutable entity and then look for inner entities found inside the
   * target. Recurse on those targets afterwards to update. This method assumes that a substep has
   * already started for target and its internal entities.</p>
   *
   * @param target The root MutableEntity to be updated and within which inner entities are
   *     recursively updated.
   */
  private void updateEntityUnsafe(MutableEntity target) {
    // Resolve all attributes and collect entities discovered during resolution.
    // This eliminates the need for a separate getInnerEntities() traversal.
    List<MutableEntity> innerEntities = new ArrayList<>();

    // OPTIMIZATION: Use integer-based iteration instead of string iteration
    // This avoids repeated HashMap lookups for attribute names
    Map<String, Integer> indexMap = target.getAttributeNameToIndex();
    int numAttributes = indexMap.size();

    for (int i = 0; i < numAttributes; i++) {
      Optional<EngineValue> value = target.getAttributeValue(i);
      if (value.isEmpty()) {
        continue;
      }

      EngineValue attributeValue = value.get();
      if (!attributeValue.getLanguageType().containsAttributes()) {
        continue;
      }

      Optional<Integer> size = attributeValue.getSize();
      if (size.isEmpty()) {
        continue;
      }

      int sizeValue = size.get();
      if (sizeValue == 1) {
        innerEntities.add(attributeValue.getAsMutableEntity());
      } else {
        Iterable<EngineValue> contents = attributeValue.getAsDistribution()
            .getContents(sizeValue, false);
        for (EngineValue entityValue : contents) {
          innerEntities.add(entityValue.getAsMutableEntity());
        }
      }
    }

    // Recurse into discovered entities
    for (MutableEntity innerEntity : innerEntities) {
      updateEntityUnsafe(innerEntity);
    }
  }

}
