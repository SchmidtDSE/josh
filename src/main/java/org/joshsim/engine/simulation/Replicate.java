/**
 * Structures describing a replicate of a simulation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.simulation;

import java.util.Optional;

import org.joshsim.engine.entity.Entity;


/**
 * A full simulation replicate.
 * 
 * <p>A single replicate of a simulation which, in Monte Carlo, may have multiple replicates. This
 * extends  across all timesteps in a replicate such that replicates may be created in distribution.
 * This provides methods to access time steps and query entities across time steps.
 * </p>
 */
public interface Replicate {
  /**
   * Add a time step to this replicate.
   *
   * @param timeStep the time step to add
   */
  void addTimeStep(TimeStep timeStep);

  /**
   * Get a time step by its step number.
   *
   * @param stepNumber the step number to retrieve
   * @return an Optional containing the time step if found, or empty if not found
   */
  Optional<TimeStep> getTimeStep(int stepNumber);

  /**
   * Query entities across time steps based on the provided query description.
   *
   * @param query the query defining spatial and temporal bounds
   * @return an iterable of matching entities
   */
  Iterable<Entity> query(Query query);
}