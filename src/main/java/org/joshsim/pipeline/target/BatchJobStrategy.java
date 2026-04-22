/**
 * Orchestrates batch job dispatch and polling against a pre-staged MinIO prefix.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import java.util.UUID;
import org.joshsim.util.MinioHandler;
import org.joshsim.util.OutputOptions;


/**
 * Orchestrates remote batch execution: dispatch an already-staged MinIO prefix and poll.
 *
 * <p>This strategy assumes the caller (typically {@code BatchRemoteCommand}) has already
 * staged inputs to the MinIO prefix — whether by delegating upload to this same process
 * via {@code --stage-from-local-dir} or by an upstream {@code stageToMinio} invocation.
 * This class never uploads; it only dispatches to a {@link RemoteBatchTarget} and polls
 * via a {@link BatchPollingStrategy}.</p>
 */
public class BatchJobStrategy {

  private static final long DEFAULT_POLL_INTERVAL_MS = 5000;
  private static final long DEFAULT_TIMEOUT_MS = 3600000;

  private final RemoteBatchTarget target;
  private final BatchPollingStrategy poller;
  private final OutputOptions output;
  private final long pollIntervalMs;
  private final long timeoutMs;

  /**
   * Constructs a BatchJobStrategy with default poll interval and timeout.
   *
   * @param target The compute target to dispatch to.
   * @param poller The polling strategy for checking job status.
   * @param output For logging.
   */
  public BatchJobStrategy(RemoteBatchTarget target, BatchPollingStrategy poller,
      OutputOptions output) {
    this(target, poller, output, DEFAULT_POLL_INTERVAL_MS, DEFAULT_TIMEOUT_MS);
  }

  /**
   * Constructs a BatchJobStrategy with custom poll interval and timeout.
   *
   * @param target The compute target to dispatch to.
   * @param poller The polling strategy for checking job status.
   * @param output For logging.
   * @param pollIntervalMs Milliseconds between poll attempts.
   * @param timeoutMs Maximum milliseconds to wait before timing out.
   */
  public BatchJobStrategy(RemoteBatchTarget target, BatchPollingStrategy poller,
      OutputOptions output, long pollIntervalMs, long timeoutMs) {
    this.target = target;
    this.poller = poller;
    this.output = output;
    this.pollIntervalMs = pollIntervalMs;
    this.timeoutMs = timeoutMs;
  }

  /**
   * Dispatches a batch job against the given pre-staged MinIO prefix and polls until
   * the job reaches a terminal state.
   *
   * @param minioPrefix The MinIO object prefix where inputs are already staged.
   * @param simulation Name of the simulation to run.
   * @param replicates Number of replicates to execute.
   * @return The final job status.
   * @throws Exception If dispatch or polling fails.
   */
  public JobStatus execute(String minioPrefix, String simulation, int replicates)
      throws Exception {
    String jobId = UUID.randomUUID().toString();
    String prefix = MinioHandler.normalizePrefix(minioPrefix);

    output.printInfo("Dispatching to target (" + replicates + " replicates) against "
        + prefix + "...");
    target.dispatch(jobId, prefix, simulation, replicates);
    output.printInfo("Dispatched. Polling for completion...");

    return pollUntilTerminal(jobId);
  }

  /**
   * Dispatches without waiting for completion. Returns the jobId for manual tracking.
   *
   * @param minioPrefix The MinIO object prefix where inputs are already staged.
   * @param simulation Name of the simulation to run.
   * @param replicates Number of replicates to execute.
   * @return The jobId for manual status tracking.
   * @throws Exception If dispatch fails.
   */
  public String executeNoWait(String minioPrefix, String simulation, int replicates)
      throws Exception {
    String jobId = UUID.randomUUID().toString();
    String prefix = MinioHandler.normalizePrefix(minioPrefix);

    output.printInfo("Dispatching to target (" + replicates + " replicates) against "
        + prefix + "...");
    target.dispatch(jobId, prefix, simulation, replicates);
    output.printInfo("Dispatched. Status path: batch-status/" + jobId + "/status.json");

    return jobId;
  }

  private JobStatus pollUntilTerminal(String jobId) throws Exception {
    long startTime = System.currentTimeMillis();
    long elapsed = 0;

    while (elapsed < timeoutMs) {
      Thread.sleep(pollIntervalMs);
      elapsed = System.currentTimeMillis() - startTime;

      JobStatus status = poller.poll(jobId);
      output.printInfo("  [" + (elapsed / 1000) + "s] " + status.getState().name().toLowerCase()
          + status.getMessage().map(m -> " — " + m).orElse(""));

      if (status.isTerminal()) {
        return status;
      }
    }

    return new JobStatus(JobStatus.State.ERROR, "Polling timed out after " + (timeoutMs / 1000)
        + "s", null);
  }
}
