/**
 * Interface for dispatching batch jobs to remote compute targets.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;


/**
 * Dispatch interface for remote batch execution targets.
 *
 * <p>Each implementation knows how to tell a specific compute backend to execute
 * a simulation from pre-staged MinIO inputs. The dispatch mechanism varies by
 * backend (HTTP POST for Cloud Run, K8s Job creation for Kubernetes, SSH for
 * remote servers), but the contract is the same: inputs are staged, dispatch
 * says "go", results land in MinIO.</p>
 *
 * <p>Implementations should return after the job has been accepted by the target,
 * not after execution completes. Status tracking is handled separately by
 * {@link BatchPollingStrategy}.</p>
 */
public interface RemoteBatchTarget {

  /**
   * Dispatches a batch job to this compute target.
   *
   * <p>The caller is responsible for staging inputs to MinIO before calling this
   * method. The target is responsible for ensuring the worker can access those
   * inputs (e.g., by passing the MinIO prefix in the request or Job spec).</p>
   *
   * <p>How replicates are executed depends on the target implementation. An HTTP
   * target may run all replicates in a single container (JIT warmup, shared memory).
   * A Kubernetes target may create an indexed Job with N pod completions. The caller
   * does not prescribe parallelism — the target decides.</p>
   *
   * @param jobId Unique identifier for this job, used for status tracking.
   * @param minioPrefix The MinIO object prefix where inputs are staged
   *     (e.g., {@code batch-jobs/abc123/inputs/}).
   * @param simulation The name of the simulation to run.
   * @param replicates The number of replicates to execute.
   * @throws Exception If the dispatch fails (e.g., network error, auth failure).
   */
  void dispatch(String jobId, String minioPrefix, String simulation, int replicates)
      throws Exception;
}
