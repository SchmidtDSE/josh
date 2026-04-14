/**
 * Orchestrates batch job execution: stage, dispatch, poll.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.joshsim.util.MinioHandler;
import org.joshsim.util.OutputOptions;


/**
 * Orchestrates the full batch remote execution flow.
 *
 * <p>The strategy handles three phases:</p>
 * <ol>
 *   <li><b>Stage</b> — upload local input files to MinIO under a job-specific prefix</li>
 *   <li><b>Dispatch</b> — tell the target to execute the simulation</li>
 *   <li><b>Poll</b> — wait for completion by polling status via {@link BatchPollingStrategy}</li>
 * </ol>
 */
public class BatchJobStrategy {

  private static final long DEFAULT_POLL_INTERVAL_MS = 5000;
  private static final long DEFAULT_TIMEOUT_MS = 3600000;

  private final RemoteBatchTarget target;
  private final BatchPollingStrategy poller;
  private final MinioHandler minioHandler;
  private final OutputOptions output;
  private final long pollIntervalMs;
  private final long timeoutMs;

  /**
   * Constructs a BatchJobStrategy with default poll interval and timeout.
   *
   * @param target The compute target to dispatch to.
   * @param poller The polling strategy for checking job status.
   * @param minioHandler The MinIO handler for staging inputs (configured with target's creds).
   * @param output For logging.
   */
  public BatchJobStrategy(RemoteBatchTarget target, BatchPollingStrategy poller,
      MinioHandler minioHandler, OutputOptions output) {
    this(target, poller, minioHandler, output, DEFAULT_POLL_INTERVAL_MS, DEFAULT_TIMEOUT_MS);
  }

  /**
   * Constructs a BatchJobStrategy with custom poll interval and timeout.
   *
   * @param target The compute target to dispatch to.
   * @param poller The polling strategy for checking job status.
   * @param minioHandler The MinIO handler for staging inputs.
   * @param output For logging.
   * @param pollIntervalMs Milliseconds between poll attempts.
   * @param timeoutMs Maximum milliseconds to wait before timing out.
   */
  public BatchJobStrategy(RemoteBatchTarget target, BatchPollingStrategy poller,
      MinioHandler minioHandler, OutputOptions output, long pollIntervalMs, long timeoutMs) {
    this.target = target;
    this.poller = poller;
    this.minioHandler = minioHandler;
    this.output = output;
    this.pollIntervalMs = pollIntervalMs;
    this.timeoutMs = timeoutMs;
  }

  /**
   * Executes the full batch flow: stage, dispatch, poll until terminal.
   *
   * @param inputDir Local directory containing simulation files to stage.
   * @param simulation Name of the simulation to run.
   * @param replicates Number of replicates to execute.
   * @return The final job status.
   * @throws Exception If staging, dispatch, or polling fails.
   */
  public JobStatus execute(File inputDir, String simulation, int replicates) throws Exception {
    String jobId = UUID.randomUUID().toString();
    String minioPrefix = "batch-jobs/" + jobId + "/inputs/";

    output.printInfo("Staging " + inputDir.getName() + " to MinIO (" + minioPrefix + ")...");
    stageDirectory(inputDir, minioPrefix);
    output.printInfo("Staging complete.");

    output.printInfo("Dispatching to target (" + replicates + " replicates)...");
    target.dispatch(jobId, minioPrefix, simulation, replicates);
    output.printInfo("Dispatched. Polling for completion...");

    return pollUntilTerminal(jobId);
  }

  /**
   * Stages and dispatches without waiting for completion.
   *
   * @param inputDir Local directory containing simulation files to stage.
   * @param simulation Name of the simulation to run.
   * @param replicates Number of replicates to execute.
   * @return The jobId for manual status tracking.
   * @throws Exception If staging or dispatch fails.
   */
  public String executeNoWait(File inputDir, String simulation, int replicates) throws Exception {
    String jobId = UUID.randomUUID().toString();
    String minioPrefix = "batch-jobs/" + jobId + "/inputs/";

    output.printInfo("Staging " + inputDir.getName() + " to MinIO (" + minioPrefix + ")...");
    stageDirectory(inputDir, minioPrefix);
    output.printInfo("Staging complete.");

    output.printInfo("Dispatching to target (" + replicates + " replicates)...");
    target.dispatch(jobId, minioPrefix, simulation, replicates);
    output.printInfo("Dispatched. Status path: batch-status/" + jobId + "/status.json");

    return jobId;
  }

  private void stageDirectory(File inputDir, String prefix) throws IOException {
    Path basePath = inputDir.toPath();
    try (Stream<Path> walker = Files.walk(basePath)) {
      List<Path> files = walker.filter(Files::isRegularFile).toList();
      for (Path file : files) {
        String relativePath = basePath.relativize(file).toString();
        String objectPath = prefix + relativePath;
        if (!minioHandler.uploadFile(file.toFile(), objectPath)) {
          throw new IOException("Failed to upload " + file);
        }
      }
    }
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
