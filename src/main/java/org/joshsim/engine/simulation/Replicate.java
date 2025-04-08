/**
 * Structures describing a replicate of a simulation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.simulation;

import java.util.HashMap;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.type.Patch;
import org.joshsim.engine.geometry.EngineGeometry;



/**
 * A full simulation replicate.
 *
 * <p>A single replicate of a simulation which, in Monte Carlo, may have multiple replicates. This
 * extends across all timesteps in a replicate such that replicates may be created in distribution.
 * This provides methods to access time steps and query entities across time steps.
 * </p>
 */
public class Replicate {
  private HashMap<Long, TimeStep> pastTimeSteps = new HashMap<>();
  private HashMap<GeoKey, Patch> presentTimeStep;
  private long stepNumber = 0;

  /**
   * Construct a replicate with the given entity builders.
   *
   * @param patches the patches to be included in the replicate.
   */
  public Replicate(HashMap<GeoKey, Patch> patches) {
    this.presentTimeStep = patches;
  }

  /**
   * Get the current step number.
   *
   * @return the current step number.
   */
  public long getStepNumber() {
    return stepNumber;
  }

  /**
   * Save the current state as the given step number.
   *
   * <p>Save the current state as the given step number, freezing all entities as immutable such
   * that further changes are not reflected into those immutable copies.</p>
   *
   * @param stepNumber the number of the step to have current state saved as.
   * @throws IllegalArgumentException if a TimeStep of the given step number already exists.
   */
  public void saveTimeStep(long stepNumber) {
    if (pastTimeSteps.containsKey(stepNumber)) {
      throw new IllegalArgumentException("TimeStep already exists for step number " + stepNumber);
    }
    HashMap<GeoKey, Entity> frozenPatches = new HashMap<>();
    for (Patch patch : presentTimeStep.values()) {
      frozenPatches.put(patch.getKey().orElseThrow(), patch.freeze());
    }
    TimeStep frozenTimeStep = new TimeStep(stepNumber, frozenPatches);
    pastTimeSteps.put(stepNumber, frozenTimeStep);
  }

  /**
   * Get a time step by its step number.
   *
   * @param stepNumber the unique step number corresponding to the TimeStep to retrieve.
   * @return an Optional containing the time step if found, or empty if not found.
   */
  public Optional<TimeStep> getTimeStep(long stepNumber) {
    return Optional.ofNullable(pastTimeSteps.get(stepNumber));
  }

  /**
   * Query patches across space and / or time based on the provided query description. This
   * is _only_ allowed for past patches.
   *
   * @param query the query defining spatial and / or temporal bounds.
   * @return an iterable of matching patches as immutable entities.
   */
  public Iterable<Entity> query(Query query) {
    if (query.getStep() == getStepNumber()) {
      throw new IllegalArgumentException("Querying current state is not allowed.");
    }

    TimeStep timeStep = pastTimeSteps.get(query.getStep());
    if (timeStep == null) {
      throw new IllegalArgumentException("No TimeStep found for step number " + query.getStep());
    }
    assert timeStep.getStep() == query.getStep();

    Optional<EngineGeometry> geometry = query.getGeometry();
    if (query.getGeometry().isPresent()) {
      return timeStep.getPatches(geometry.get());
    } else {
      return timeStep.getPatches();
    }
  }

  /**
   * Lookup a Patch at the given step number. This is only allowed for current patches.
   * Past patches will be queried using the query method.
   *
   * @param key of the Patch to lookup.
   * @param stepNumber of the timestep at which to return the patch.
   */
  public Patch getPatchByKey(GeoKey key, long stepNumber) {
    if (stepNumber != getStepNumber()) {
      throw new IllegalArgumentException(
        "Cannot lookup Patch at past time steps using `getPatchByKey`, use `query` instead."
      );
    }
    return presentTimeStep.get(key);
  }

  /**
   * Get all patches in current state. This is functionally equivalent to querying all patches
   * for the current step number, except that this returns mutable Patch objects.
   *
   * @return Iterable over mutable Patches.
   */
  public Iterable<Patch> getCurrentPatches() {
    return presentTimeStep.values();
  }
}
