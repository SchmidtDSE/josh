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


/**
 * Operation on an EngineBridge which completes a full step in a simulation.
 */
public class SimulationStepper {

  private static final ThreadLocal<Integer> currentTimestep = ThreadLocal.withInitial(() -> -1);

  /**
   * Get the current simulation timestep number.
   *
   * <p>Returns the current timestep during simulation execution or -1 if called
   * outside of simulation context (e.g., during cache building).</p>
   *
   * @return Current timestep number, or -1 if not in simulation context
   */
  public static int getCurrentTimestep() {
    return currentTimestep.get();
  }

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

    // Set current timestep for debug logging
    long absoluteTimestep = target.getAbsoluteTimestep();
    currentTimestep.set((int) absoluteTimestep);

    try {
      boolean isFirstStep = absoluteTimestep == 0;
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
    } finally {
      // Clean up thread-local after step completes
      currentTimestep.remove();
    }
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
        if (result != null) {
          // Export patch immediately after completion if callback present
          if (shouldExport) {
            Entity frozen = exportCallback.get().exportPatch(result, currentStep);
            // Store frozen entity in Replicate for prior state access
            saveFrozenPatchToReplicate(frozen, currentStep);
          }
          numCompleted++;
        }
      }
      assert numCompleted > 0;
    } else {
      // Keep parallel stream for parallel execution
      StreamSupport.stream(entities.spliterator(), true)
          .forEach(entity -> {
            MutableEntity result = updateEntity(entity, subStep);
            if (result != null && shouldExport) {
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
   * Updates the target entity for the given sub-step, managing organism lifecycle.
   *
   * <p>This method ensures organisms are discovered AFTER all attributes are resolved,
   * preventing cache coherency violations and instance identity mismatches.</p>
   *
   * <p><b>ARCHITECTURAL CHANGE</b>: Previously, organism lifecycle was managed by
   * ShadowingEntity during startSubstep/endSubstep. This created cache coherency issues
   * where organisms discovered from cached values differed from those in final resolved values.
   * Now, organism discovery occurs after updateEntityUnsafe() completes, ensuring consistent
   * instance identity throughout the lifecycle.</p>
   *
   * @param target the entity to update (typically a patch)
   * @param subStep the sub-step name
   * @return the updated entity
   */
  private MutableEntity updateEntity(MutableEntity target, String subStep) {
    // Start substep on patch (clears cache, does NOT discover organisms)
    target.startSubstep(subStep);

    // Resolve ALL patch attributes first
    updateEntityUnsafe(target);

    // NOW discover organisms from final attribute values and manage their lifecycle
    discoverAndProcessOrganisms(target, subStep);

    // End substep on patch
    target.endSubstep();

    return target;
  }

  /**
   * Discovers organisms from final attribute values and manages their lifecycle.
   *
   * <p>This method ensures consistent organism instance identity by discovering
   * organisms AFTER all patch attributes have been resolved. Each organism goes
   * through its complete lifecycle (startSubstep → updateEntityUnsafe → endSubstep)
   * before the next organism is processed.</p>
   *
   * <p><b>CRITICAL - Deduplication</b>: Organisms are deduplicated based on sequenceId
   * to prevent processing the same organism multiple times if it appears in multiple
   * attributes (e.g., via union operations like "prior.Trees | Trees").</p>
   *
   * <p><b>CRITICAL - Recursion</b>: This method is recursive to support nested organisms.
   * Organisms can contain other organisms as attributes (e.g., Trees containing Seeds).
   * The recursion ensures that nested organisms also get their lifecycle managed properly.</p>
   *
   * <p><b>CRITICAL - Substep Check</b>: When organisms are created during a handler
   * (e.g., via "create X of OrganismType"), they may inherit the parent patch's substep
   * context. This method checks if an organism already has a substep set before calling
   * startSubstep/endSubstep. This prevents IllegalStateException when trying to start
   * a substep that's already active. Organisms created this way still need their attributes
   * resolved (via updateEntityUnsafe) even if they already have a substep set.</p>
   *
   * <p>Examples of nested organisms in codebase:
   * <ul>
   *   <li>test_parent_geokey.josh - Parent/child with geographic relationships</li>
   *   <li>test_parent_lineage.josh - Multi-level parent-child lineage</li>
   *   <li>test_parent_synthetic.josh - Nested organisms with synthetic attributes</li>
   * </ul>
   * </p>
   *
   * @param target the patch entity containing organisms
   * @param subStep the current substep name
   */
  private void discoverAndProcessOrganisms(MutableEntity target, String subStep) {
    // Discover organisms from FINAL attribute values (after all handlers executed)
    List<MutableEntity> organisms = new ArrayList<>();
    for (MutableEntity organism : InnerEntityGetter.getInnerEntities(target)) {
      organisms.add(organism);
    }

    // Track organisms we've processed to avoid duplicates
    // Union operations (e.g., "prior.Trees | Trees") may create duplicate instances
    // of the same organism. We use sequenceId for deduplication:
    // - Patches/Simulation have sequenceId = 0L (never deduplicated)
    // - Organisms (MemberSpatialEntity) have unique sequenceId values
    Set<Long> processedIds = new HashSet<>();

    for (MutableEntity organism : organisms) {
      // Skip duplicates based on sequence ID
      long organismId = organism.getSequenceId();
      if (processedIds.contains(organismId)) {
        continue;
      }
      processedIds.add(organismId);

      // Check if organism already has substep set (happens when organism created during handler)
      Optional<String> currentSubstep = organism.getSubstep();
      boolean needsStartSubstep = currentSubstep.isEmpty();

      // Complete lifecycle for this organism
      if (needsStartSubstep) {
        organism.startSubstep(subStep);
      }
      updateEntityUnsafe(organism);

      // CRITICAL: Recursively discover and process nested organisms
      // (e.g., Trees containing Seeds, ParentTree containing ChildTree)
      // This ensures organisms at ALL levels of nesting get their lifecycle managed
      discoverAndProcessOrganisms(organism, subStep);

      // ALWAYS call endSubstep to clear substep state for next timestep
      // Even newly created organisms need this to clear their substep
      organism.endSubstep();
    }
  }

  /**
   * Resolve all properties inside of a mutable entity.
   *
   * <p>This method ONLY resolves attributes. Organism lifecycle management
   * is handled by discoverAndProcessOrganisms() for patches, or by the
   * recursive call in discoverAndProcessOrganisms() for nested organisms.</p>
   *
   * <p><b>ARCHITECTURAL CHANGE</b>: Previously, this method discovered and processed
   * organisms inline during attribute resolution. This caused cache coherency violations.
   * Now, organisms are processed separately after ALL attributes are resolved.</p>
   *
   * @param target The MutableEntity to resolve attributes for
   */
  private void updateEntityUnsafe(MutableEntity target) {
    // Use integer-based iteration
    Map<String, Integer> indexMap = target.getAttributeNameToIndex();
    int numAttributes = indexMap.size();

    // Resolve all attributes (no organism discovery here)
    for (int i = 0; i < numAttributes; i++) {
      target.getAttributeValue(i);
      // Attribute resolution triggers handlers but NOT organism lifecycle
    }

    // NOTE: No organism discovery or lifecycle management here
    // That is handled by discoverAndProcessOrganisms() called from updateEntity()
    // For organisms themselves, this method is called AFTER their startSubstep()
    // and BEFORE their endSubstep() by discoverAndProcessOrganisms()
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
