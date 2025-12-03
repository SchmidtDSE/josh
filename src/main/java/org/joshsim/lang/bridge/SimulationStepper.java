package org.joshsim.lang.bridge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.joshsim.engine.entity.base.Entity;
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
  private final Optional<PatchExportCallback> exportCallback;

  /**
   * Create a new stepper around a bridge without export callback.
   *
   * <p>Creates a stepper with no export callback, falling back to bulk freeze mode.
   * This constructor provides backward compatibility.</p>
   *
   * @param target EngineBridge in which to perform this operation.
   */
  public SimulationStepper(EngineBridge target) {
    this(target, Optional.empty());
  }

  /**
   * Create a new stepper around a bridge with optional export callback.
   *
   * <p>Collects all unique event names from patch event handlers using direct iteration
   * instead of streams for better performance.</p>
   *
   * @param target EngineBridge in which to perform this operation.
   * @param exportCallback Optional callback for incremental patch export
   */
  public SimulationStepper(EngineBridge target, Optional<PatchExportCallback> exportCallback) {
    this.target = target;
    this.exportCallback = exportCallback;

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
    long currentStep = target.getCurrentTimestep();
    boolean shouldExport = exportCallback.isPresent() && shouldExportInSubstep(subStep);

    if (serial) {
      // Use direct iteration for serial execution (better performance)
      long numCompleted = 0;
      for (MutableEntity entity : entities) {
        MutableEntity result = updateEntity(entity, subStep);
        if (shouldExport) {
          Entity frozen = exportCallback.get().exportPatch(result, currentStep);
          saveFrozenPatchToReplicate(frozen, currentStep);
        }
        numCompleted++;
      }
      assert numCompleted > 0;
    } else {
      // Keep parallel stream for parallel execution
      StreamSupport.stream(entities.spliterator(), true)
          .forEach(entity -> {
            MutableEntity result = updateEntity(entity, subStep);
            if (shouldExport) {
              Entity frozen = exportCallback.get().exportPatch(result, currentStep);
              saveFrozenPatchToReplicate(frozen, currentStep);
            }
          });
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
   * <p>Resolves all attributes of a mutable entity, collecting any inner entities discovered during
   * resolution. Recurses on those inner entities afterwards to update them. This method assumes
   * that a substep has already started for target and its internal entities.</p>
   *
   * @param target The root MutableEntity to be updated and within which inner entities are
   *     recursively updated.
   * @param subStep The step to be executed.
   */
  private void updateEntityUnsafe(MutableEntity target) {

    // Use integer-based iteration
    Map<String, Integer> indexMap = target.getAttributeNameToIndex();
    int numAttributes = indexMap.size();

    // Pre-size ArrayList with numAttributes as upper bound
    List<MutableEntity> innerEntities = new ArrayList<>(numAttributes);

    for (int i = 0; i < numAttributes; i++) {
      Optional<EngineValue> value = target.getAttributeValue(i);
      boolean valueResolved = !value.isEmpty();
      if (!valueResolved) {
        continue;
      }

      EngineValue attributeValue = value.get();
      boolean innerIsEntity = attributeValue.getLanguageType().containsAttributes();
      if (!innerIsEntity) {
        continue;
      }

      Optional<Integer> size = attributeValue.getSize();
      boolean innerNoAttributes = size.isEmpty();
      if (innerNoAttributes) {
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

  /**
   * Determine if patches should be exported in the given substep.
   *
   * <p>Only export after the final substep to ensure the patch is fully resolved.
   * The final substep is determined by checking which events are defined, with
   * priority: end > step > start > init.</p>
   *
   * @param subStep the substep to check
   * @return true if patches should be exported after this substep
   */
  private boolean shouldExportInSubstep(String subStep) {
    // Only export after final substep (usually "step", but could be "end")
    if (events.contains("end")) {
      return subStep.equals("end");
    } else if (events.contains("step")) {
      return subStep.equals("step");
    } else if (events.contains("start")) {
      return subStep.equals("start");
    } else {
      return subStep.equals("init");
    }
  }

  /**
   * Save a frozen patch to the Replicate for prior state access.
   *
   * <p>Stores the frozen entity in Replicate so it can be accessed via prior
   * state queries in the next timestep.</p>
   *
   * @param frozen the frozen Entity to save
   * @param currentStep the current timestep number
   */
  private void saveFrozenPatchToReplicate(Entity frozen, long currentStep) {
    target.getReplicate().saveFrozenPatch(frozen, currentStep);
  }

}
