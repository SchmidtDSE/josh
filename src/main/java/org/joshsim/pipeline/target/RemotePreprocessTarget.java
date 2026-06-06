/**
 * Interface for dispatching preprocessing jobs to remote compute targets.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;


/**
 * Dispatch interface for remote preprocessing targets.
 *
 * <p>Mirrors {@link RemoteBatchTarget} but for preprocessing (converting external
 * data to .jshd format) rather than simulation execution. Each implementation
 * knows how to tell a specific compute backend to run a preprocessing job from
 * pre-staged MinIO inputs.</p>
 *
 * <p>Implementations should return after the job has been accepted by the target,
 * not after execution completes. Status tracking is handled separately by
 * {@link BatchPollingStrategy}.</p>
 */
public interface RemotePreprocessTarget {

  /**
   * Dispatches a preprocessing job to this compute target.
   *
   * <p>The caller is responsible for staging inputs to MinIO before calling this
   * method. The target is responsible for ensuring the worker can access those
   * inputs (e.g., by passing the MinIO prefix in the request or Job spec).</p>
   *
   * @param jobId Unique identifier for this job, used for status tracking.
   * @param minioPrefix The MinIO object prefix where inputs are staged
   *     (e.g., {@code batch-jobs/abc123/inputs/}).
   * @param simulation The name of the simulation (for grid/metadata extraction).
   * @param params Preprocess-specific parameters (data file, variable, units, etc.).
   * @throws Exception If the dispatch fails (e.g., network error, auth failure).
   */
  void dispatch(String jobId, String minioPrefix, String simulation,
      PreprocessParams params) throws Exception;
}
