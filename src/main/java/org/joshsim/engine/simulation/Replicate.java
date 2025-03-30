/**
 * Structures describing a replicate of a simulation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.EntityBuilder;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.type.Patch;


/**
 * A full simulation replicate.
 *
 * <p>A single replicate of a simulation which, in Monte Carlo, may have multiple replicates. This
 * extends across all timesteps in a replicate such that replicates may be created in distribution.
 * This provides methods to access time steps and query entities across time steps.
 * </p>
 */
public class Replicate {
  private TimeStepBuilder timeStepBuilder = new TimeStepBuilder();
  private List<EntityBuilder> entityBuilders;
  private HashMap<Long, TimeStep> pastTimeSteps = new HashMap<>();
  private TimeStep presentTimeStep;
 

  public Replicate(List<EntityBuilder> entityBuilders){ {
    this.entityBuilders = entityBuilders;
    this.presentTimeStep = timeStepBuilder.buildInitial(entityBuilders);
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
  void saveTimeStep(long stepNumber){
    if(pastTimeSteps.containsKey(stepNumber)){
      throw new IllegalArgumentException("TimeStep already exists for step number " + stepNumber);
    }
    TimeStep frozenTimeStep = presentTimeStep.freeze();
    pastTimeSteps.put(stepNumber, frozenTimeStep);
  }

  /**
   * Get a time step by its step number.
   *
   * @param stepNumber the unique step number corresponding to the TimeStep to retrieve.
   * @return an Optional containing the time step if found, or empty if not found.
   */
  Optional<TimeStep> getTimeStep(long stepNumber){
    return Optional.ofNullable(pastTimeSteps.get(stepNumber));
  }

  /**
   * Query patches across space and / or time based on the provided query description. This
   * is _only_ allowed for past patches.
   *
   * @param query the query defining spatial and / or temporal bounds.
   * @return an iterable of matching patches as immutable entities.
   */
  Iterable<Entity> query(Query query) {
    if(query.getStep() == presentTimeStep.getStep()){
      throw new IllegalArgumentException("Querying current state is not allowed.");
    }
    return pastTimeSteps.get(query.getStep()).getEntities(query.getGeometry());
  }

  /**
   * Lookup a Patch at the given step number. This is allowed for current patches as well as past
   * patches.
   *
   * @param key of the Patch to lookup.
   * @param stepNumber of the timestep at which to return the patch.
   */
  Entity getPatchByKey(GeoKey key, long stepNumber) {
    if(stepNumber == presentTimeStep.getStepNumber()){
      return presentTimeStep.getPatchByKey(key);
    }
    return pastTimeSteps.get(stepNumber).getPatchByKey(key);
  }

  /**
   * Get all patches in current state.
   *
   * @return Iterable over mutable Patches.
   */
  Iterable<Entity> getCurrentPatches(){
    return presentTimeStep.getEntities();
  }
}
