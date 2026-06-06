/**
 * Strategy interface for polling batch job status.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;


/**
 * Strategy for polling the status of a dispatched batch job.
 *
 * <p>Different implementations poll different status sources. The default
 * {@link MinioPollingStrategy} reads {@code batch-status/<jobId>/status.json}
 * from MinIO, which works for all target types since the server writes status
 * there. Future implementations (e.g., {@code KubernetesPollingStrategy}) can
 * query platform-native status APIs for richer error information such as
 * OOMKill events or scheduling failures.</p>
 */
public interface BatchPollingStrategy {

  /**
   * Polls the current status of a batch job.
   *
   * @param jobId The unique job identifier to check.
   * @return The current status of the job.
   * @throws Exception If the polling operation itself fails (e.g., network error).
   */
  JobStatus poll(String jobId) throws Exception;
}
