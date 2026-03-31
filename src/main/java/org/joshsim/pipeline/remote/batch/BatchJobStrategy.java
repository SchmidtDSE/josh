/**
 * Batch job execution strategy for remote simulation runs.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.remote.batch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import org.joshsim.pipeline.remote.RunRemoteContext;
import org.joshsim.pipeline.remote.RunRemoteStrategy;
import org.joshsim.util.MinioHandler;
import org.joshsim.util.OutputOptions;


/**
 * RunRemoteStrategy that submits batch jobs to a remote target via MinIO staging.
 *
 * <p>This strategy mirrors the local-leader fan-out pattern but replaces HTTP streaming
 * with job submission and MinIO-based result delivery. The flow is:
 * validate exports, stage inputs to MinIO, submit job via target, poll until terminal,
 * clean up. Results are written directly to MinIO by each worker pod/process.</p>
 */
public class BatchJobStrategy implements RunRemoteStrategy {

  private static final long INITIAL_POLL_INTERVAL_MS = 2000;
  private static final long MAX_POLL_INTERVAL_MS = 30000;
  private static final double POLL_BACKOFF_FACTOR = 1.5;

  private final RemoteBatchTarget target;

  /**
   * Creates a new BatchJobStrategy.
   *
   * @param target the remote batch target to submit jobs to
   */
  public BatchJobStrategy(RemoteBatchTarget target) {
    this.target = target;
  }

  @Override
  public void execute(RunRemoteContext context) throws IOException, InterruptedException {
    OutputOptions output = context.getOutputOptions();
    String jobId = generateJobId();

    // 1. Validate export paths (client-side, fail fast before uploading)
    output.printInfo("Validating export paths for batch execution...");
    List<String> errors = BatchExportValidator.validate(
        context.getFile(), context.getSimulation(), output
    );
    if (!errors.isEmpty()) {
      for (String error : errors) {
        output.printError(error);
      }
      throw new IOException("Export path validation failed. See errors above.");
    }

    // 2. Stage inputs to MinIO
    String inputPrefix = jobId + "/input/";
    output.printInfo("Staging inputs to MinIO: " + inputPrefix);
    stageInputs(context, inputPrefix);

    // 3. Build job spec
    BatchJobSpec spec = buildSpec(context, jobId, inputPrefix);

    // 4. Submit job
    String submittedId = target.submitJob(spec);

    // 5. Poll until terminal
    try {
      JobStatus finalStatus = pollUntilTerminal(submittedId, output);

      if (finalStatus == JobStatus.FAILED) {
        String logs = target.getLogs(submittedId);
        throw new IOException("Batch job failed. Logs:\n" + logs);
      }

      output.printInfo("Batch job completed successfully: " + jobId);
      output.printInfo("Results available at: minio://"
          + context.getMinioOptions().getBucketName() + "/" + jobId + "/output/");
    } finally {
      // 6. Cleanup (always, even on failure)
      try {
        target.cleanup(submittedId);
      } catch (IOException e) {
        output.printError("Warning: cleanup failed: " + e.getMessage());
      }
    }
  }

  /**
   * Uploads simulation inputs to MinIO for worker consumption.
   *
   * @param context the execution context
   * @param inputPrefix the MinIO prefix for staged inputs
   * @throws IOException if upload fails
   */
  void stageInputs(RunRemoteContext context, String inputPrefix) throws IOException {
    OutputOptions output = context.getOutputOptions();

    try {
      MinioHandler handler = new MinioHandler(context.getMinioOptions(), output);

      // Upload .josh source file
      handler.uploadFile(context.getFile(), inputPrefix + context.getFile().getName());

      // Upload external data files from job file mappings
      for (var entry : context.getFilePaths().entrySet()) {
        File dataFile = new File(entry.getValue());
        if (dataFile.exists()) {
          handler.uploadFile(dataFile, inputPrefix + "data/" + entry.getKey());
        }
      }

      // Upload .jshc config files from working directory
      File workingDir = context.getFile().getParentFile();
      if (workingDir != null) {
        File[] configFiles = workingDir.listFiles((d, name) -> name.endsWith(".jshc"));
        if (configFiles != null) {
          for (File configFile : configFiles) {
            handler.uploadFile(configFile, inputPrefix + "config/" + configFile.getName());
          }
        }
      }

    } catch (Exception e) {
      throw new IOException("Failed to stage inputs to MinIO: " + e.getMessage(), e);
    }
  }

  /**
   * Builds a BatchJobSpec from the execution context.
   *
   * @param context the execution context
   * @param jobId the unique job identifier
   * @param inputPrefix the MinIO input prefix
   * @return the constructed spec
   */
  BatchJobSpec buildSpec(RunRemoteContext context, String jobId, String inputPrefix) {
    KubernetesConfig k8sConfig = context.getKubernetesConfig();

    int parallelism = k8sConfig.getParallelism();
    if (parallelism < 0) {
      parallelism = context.getReplicates();
    }

    return new BatchJobSpec.Builder()
        .setJobId(jobId)
        .setSimulation(context.getSimulation())
        .setImage(k8sConfig.getImage())
        .setInputPrefix(inputPrefix)
        .setOutputPrefix(jobId + "/output/")
        .setMinioOptions(context.getMinioOptions())
        .setMemory(k8sConfig.getMemory())
        .setCpu(k8sConfig.getCpu())
        .setGpu(k8sConfig.getGpu())
        .setTotalReplicates(context.getReplicates())
        .setReplicatesPerJob(context.getReplicatesPerJob())
        .setMaxParallelism(parallelism)
        .setTimeoutSeconds(k8sConfig.getTimeoutSeconds())
        .setUseFloat64(context.isUseFloat64())
        .setLabels(k8sConfig.getNodeSelector())
        .build();
  }

  /**
   * Polls job status with exponential backoff until a terminal state is reached.
   *
   * @param jobId the job identifier to poll
   * @param output for logging progress
   * @return the terminal job status
   * @throws IOException if polling fails
   * @throws InterruptedException if the thread is interrupted
   */
  JobStatus pollUntilTerminal(String jobId, OutputOptions output)
      throws IOException, InterruptedException {
    long pollInterval = INITIAL_POLL_INTERVAL_MS;
    JobStatus lastStatus = null;

    while (true) {
      JobStatus status = target.pollStatus(jobId);

      if (status != lastStatus) {
        output.printInfo("Job " + jobId + " status: " + status);
        lastStatus = status;
      }

      if (status.isTerminal()) {
        return status;
      }

      Thread.sleep(pollInterval);
      pollInterval = Math.min(
          (long) (pollInterval * POLL_BACKOFF_FACTOR),
          MAX_POLL_INTERVAL_MS
      );
    }
  }

  /**
   * Generates a unique job identifier.
   *
   * @return a short UUID-based job ID
   */
  static String generateJobId() {
    return UUID.randomUUID().toString().substring(0, 8);
  }
}
