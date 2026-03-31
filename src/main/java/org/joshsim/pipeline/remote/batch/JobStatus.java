/**
 * Status values for batch job lifecycle tracking.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.remote.batch;


/**
 * Represents the lifecycle status of a batch job submitted to a remote target.
 *
 * <p>Used by {@link RemoteBatchTarget#pollStatus(String)} to report job progress.
 * The lifecycle progresses from PENDING to RUNNING and then terminates as either
 * COMPLETE or FAILED.</p>
 */
public enum JobStatus {

  /** Job has been submitted but not yet started. */
  PENDING,

  /** Job is actively running on the target. */
  RUNNING,

  /** Job completed successfully (all replicates finished). */
  COMPLETE,

  /** Job failed (one or more replicates failed, or the job timed out). */
  FAILED;

  /**
   * Returns true if this status represents a terminal state.
   *
   * @return true if COMPLETE or FAILED
   */
  public boolean isTerminal() {
    return this == COMPLETE || this == FAILED;
  }
}
