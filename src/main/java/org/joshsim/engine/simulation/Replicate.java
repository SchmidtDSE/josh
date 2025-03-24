/**
 * Structures describing a replicate of a simulation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.simulation;

import java.util.Optional;
import org.joshsim.engine.entity.Patch;
import org.joshsim.engine.entity.PatchKey;


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
   * @param timeStep the new time step to this replicate.
   * @throws IllegalArgumentException if a TimeStep of the given step number already exists.
   */
  void addTimeStep(TimeStep timeStep);

  /**
   * Get a time step by its step number.
   *
   * @param stepNumber the unique step number corresponding to the TimeStep to retrieve.
   * @return an Optional containing the time step if found, or empty if not found.
   */
  Optional<TimeStep> getTimeStep(int stepNumber);

  /**
   * Query patches across space and / or time based on the provided query description.
   *
   * @param query the query defining spatial and / or temporal bounds.
   * @return an iterable of matching patches.
   */
  Iterable<Patch> query(Query query);

  /**
   * Lookup a Patch at the given step number.
   *
   * @param key of the Patch to lookup.
   * @param stepNumber of the timestep at which to return the patch.
   */
  Patch getPatchByKey(PatchKey key, int stepNumber);
}
