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
   * <p>Implementations always freeze the mutable patch and return it (so it can be saved to the
   * Replicate for "prior" state access in the next timestep), but only serialize and queue it to
   * the writer thread when {@code write} is true. This lets output filters (e.g.
   * {@code --output-steps} / {@code --output-phases}) suppress a step's output without breaking
   * state continuity.</p>
   *
   * @param patch The mutable patch that has just completed its substep
   * @param currentStep The current timestep number
   * @param write Whether to serialize and queue this patch to the writer; if false, only freeze it
   * @return The frozen Entity (for saving to Replicate.pastTimeSteps)
   */
  Entity exportPatch(MutableEntity patch, long currentStep, boolean write);
}
