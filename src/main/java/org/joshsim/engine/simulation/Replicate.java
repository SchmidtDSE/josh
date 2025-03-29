/**
 * Structures describing a replicate of a simulation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.simulation;

import java.util.Optional;
import org.joshsim.engine.entity.Entity;
import org.joshsim.engine.entity.Patch;
import org.joshsim.engine.entity.GeoKey;


/**
 * A full simulation replicate.
 *
 * <p>A single replicate of a simulation which, in Monte Carlo, may have multiple replicates. This
 * extends  across all timesteps in a replicate such that replicates may be created in distribution.
 * This provides methods to access time steps and query entities across time steps.
 * </p>
 */
public interface Replicate {

  // TODO

  /**
   * Save the current state as the given step number.
   *
   * <p>Save the current state as the given step number, freezing all entities as immutable such
   * that further changes are not reflected into those immutable copies.</p>
   *
   * @param stepNumber the number of the step to have current state saved as.
   * @throws IllegalArgumentException if a TimeStep of the given step number already exists.
   */
  void saveTimestep(long stepNumber);

  /**
   * Get a time step by its step number.
   *
   * @param stepNumber the unique step number corresponding to the TimeStep to retrieve.
   * @return an Optional containing the time step if found, or empty if not found.
   */
  Optional<TimeStep> getTimeStep(long stepNumber);

  /**
   * Query patches across space and / or time based on the provided query description.
   *
   * @param query the query defining spatial and / or temporal bounds.
   * @return an iterable of matching patches as immutable entities.
   */
  Iterable<Entity> query(Query query);

  /**
   * Lookup a Patch at the given step number.
   *
   * @param key of the Patch to lookup.
   * @param stepNumber of the timestep at which to return the patch.
   */
  Patch getPatchByKey(GeoKey key, long stepNumber);

  /**
   * Get all patches in current state.
   *
   * @return Iterable over mutable Patches.
   */
  Iterable<Patch> getCurrentPatches();
}
