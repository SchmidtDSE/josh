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
import org.joshsim.engine.geometry.Geometry;


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


  /**
   * Construct a replicate with the given entity builders.
   *
   * @param entityBuilders the entity builders to use for creating entities.
   */
  public Replicate(List<EntityBuilder> entityBuilders){ {
    this.entityBuilders = entityBuilders;
    this.presentTimeStep = timeStepBuilder.buildInitial(entityBuilders);
  }

  /**
   * Advance this replicate by one time step, saving the current timestep as a frozen
   * immutable copy into `pastTimeSteps`, and incrementing the current timestep.
   */
  public void incrementStep() {
    saveTimeStep(presentTimeStep.getStep());
    presentTimeStep.incrementStep();
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

    TimeStep timeStep = pastTimeSteps.get(query.getStep());
    if(timeStep == null){
      throw new IllegalArgumentException("No TimeStep found for step number " + query.getStep());
    }
    assert timeStep.getStep() == query.getStep();

    Optional<Geometry> geometry = query.getGeometry();
    if (query.getGeometry().isPresent()) {
      return timeStep.getEntities(geometry.get());
    } else {
      return timeStep.getEntities();
    }
  }

  /**
   * Lookup a Patch at the given step number. This is allowed for current patches as well as past
   * patches.
   *
   * @param key of the Patch to lookup.
   * @param stepNumber of the timestep at which to return the patch.
   */
  Entity getPatchByKey(GeoKey key, long stepNumber) {
    if(stepNumber == presentTimeStep.getStep()){
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
