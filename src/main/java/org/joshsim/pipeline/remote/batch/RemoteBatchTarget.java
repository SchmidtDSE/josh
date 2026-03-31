/**
 * Interface for remote batch execution targets.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.remote.batch;

import java.io.IOException;


/**
 * Contract for submitting and managing batch jobs on a remote execution target.
 *
 * <p>Implementations handle the platform-specific details of job submission, status
 * polling, log retrieval, and cleanup. The {@link BatchJobStrategy} delegates to this
 * interface for all target-specific operations.</p>
 *
 * <p>Current implementations: {@link KubernetesTarget}. SSH target planned for Phase 3.</p>
 */
public interface RemoteBatchTarget {

  /**
   * Submits a batch job to the remote target.
   *
   * @param spec the job specification containing image, resources, replicates, etc.
   * @return a job identifier used for subsequent poll/log/cleanup calls
   * @throws IOException if submission fails (network error, auth error, etc.)
   */
  String submitJob(BatchJobSpec spec) throws IOException;

  /**
   * Polls the current status of a previously submitted job.
   *
   * @param jobId the identifier returned by {@link #submitJob(BatchJobSpec)}
   * @return the current job status
   * @throws IOException if the status query fails
   */
  JobStatus pollStatus(String jobId) throws IOException;

  /**
   * Retrieves logs from a batch job for debugging and error reporting.
   *
   * @param jobId the identifier returned by {@link #submitJob(BatchJobSpec)}
   * @return log output as a string (may be truncated for large jobs)
   * @throws IOException if log retrieval fails
   */
  String getLogs(String jobId) throws IOException;

  /**
   * Cleans up resources associated with a completed or failed job.
   *
   * @param jobId the identifier returned by {@link #submitJob(BatchJobSpec)}
   * @throws IOException if cleanup fails
   */
  void cleanup(String jobId) throws IOException;
}
