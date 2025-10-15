/**
 * Callback interface for exporting patches incrementally as they complete.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.bridge;

import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MutableEntity;


/**
 * Callback interface for exporting patches incrementally as they complete.
 *
 * <p>This interface allows patches to be frozen, serialized, and queued to the
 * writer thread immediately after completing their substep, reducing peak memory
 * usage by avoiding bulk freeze operations.</p>
 */
public interface PatchExportCallback {

  /**
   * Export a single patch after it completes its substep.
   *
   * <p>Implementations should freeze the mutable patch, serialize it, and queue it
   * to the writer thread. The frozen entity should also be saved to the Replicate
   * for "prior" state access in the next timestep.</p>
   *
   * @param patch The mutable patch that has just completed its substep
   * @param currentStep The current timestep number
   * @return The frozen Entity (for saving to Replicate.pastTimeSteps)
   */
  Entity exportPatch(MutableEntity patch, long currentStep);
}
