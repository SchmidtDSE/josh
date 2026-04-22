/**
 * Command for dispatching preprocessing jobs to remote compute targets.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.joshsim.pipeline.target.BatchPollingStrategy;
import org.joshsim.pipeline.target.HttpPreprocessTarget;
import org.joshsim.pipeline.target.JobStatus;
import org.joshsim.pipeline.target.KubernetesPollingStrategy;
import org.joshsim.pipeline.target.KubernetesPreprocessTarget;
import org.joshsim.pipeline.target.MinioPollingStrategy;
import org.joshsim.pipeline.target.PreprocessParams;
import org.joshsim.pipeline.target.RemotePreprocessTarget;
import org.joshsim.pipeline.target.TargetProfile;
import org.joshsim.pipeline.target.TargetProfileLoader;
import org.joshsim.util.MinioHandler;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


/**
 * Dispatches a preprocessing job to a remote compute target.
 *
 * <p>Stages local simulation files (Josh script + data file) to MinIO, dispatches
 * preprocessing to the configured target, polls for completion, and downloads the
 * resulting .jshd file from MinIO.</p>
 *
 * <p>Mirrors {@link BatchRemoteCommand} for preprocessing instead of simulation
 * execution. The key difference is that after completion, the result .jshd file
 * is downloaded from MinIO to the local output path.</p>
 */
@Command(
    name = "preprocessBatch",
    description = "Preprocess data on a remote batch target via MinIO staging"
)
public class PreprocessBatchCommand implements Callable<Integer> {

  private static final int TARGET_ERROR_CODE = 100;
  private static final int DISPATCH_ERROR_CODE = 101;

  @Parameters(index = "0", description = "Path to Josh simulation file or input directory")
  private File input;

  @Parameters(index = "1", description = "Simulation name (for grid/metadata extraction)")
  private String simulation;

  @Parameters(index = "2", description = "Data file name within the input directory")
  private String dataFile;

  @Parameters(index = "3", description = "Variable name or band number")
  private String variable;

  @Parameters(index = "4", description = "Units of the data for simulation use")
  private String units;

  @Parameters(index = "5", description = "Local output path for the resulting .jshd file")
  private File outputFile;

  @Option(
      names = "--target",
      description = "Target profile name (loads ~/.josh/targets/<name>.json)",
      required = true
  )
  private String targetName;

  @Option(
      names = "--no-wait",
      description = "Dispatch and exit without polling for completion",
      defaultValue = "false"
  )
  private boolean noWait = false;

  @Option(
      names = "--poll-interval",
      description = "Seconds between status polls (default: 5)",
      defaultValue = "5"
  )
  private int pollIntervalSeconds = 5;

  @Option(
      names = "--timeout",
      description = "Maximum seconds to wait for completion (default: 3600)",
      defaultValue = "3600"
  )
  private int timeoutSeconds = 3600;

  @Option(
      names = "--crs",
      description = "CRS to use in reading the file.",
      defaultValue = "EPSG:4326"
  )
  private String crs = "EPSG:4326";

  @Option(
      names = "--x-coord",
      description = "Name of X coordinate.",
      defaultValue = "lon"
  )
  private String horizCoordName = "lon";

  @Option(
      names = "--y-coord",
      description = "Name of Y coordinate.",
      defaultValue = "lat"
  )
  private String vertCoordName = "lat";

  @Option(
      names = "--time-dim",
      description = "Time dimension.",
      defaultValue = "calendar_year"
  )
  private String timeDim = "calendar_year";

  @Option(
      names = "--timestep",
      description = "The single timestep to process.",
      defaultValue = ""
  )
  private String timestep = "";

  @Option(
      names = "--default-value",
      description = "Default value to fill grid spaces before copying data from source file"
  )
  private String defaultValue;

  @Option(
      names = "--parallel",
      description = "Enable parallel processing of patches within each timestep",
      defaultValue = "false"
  )
  private boolean parallel = false;

  @Option(
      names = "--amend",
      description = "Amend existing file rather than overwriting",
      defaultValue = "false"
  )
  private boolean amend = false;

  @Mixin
  private OutputOptions output = new OutputOptions();

  @Override
  public Integer call() {
    try {
      output.printInfo("Loading target profile: " + targetName);
      TargetProfileLoader loader = new TargetProfileLoader();
      TargetProfile profile = loader.load(targetName);

      MinioHandler minioHandler = profile.buildMinioHandler(output);

      RemotePreprocessTarget target;
      BatchPollingStrategy poller;
      String type = profile.getType();

      if ("kubernetes".equals(type)) {
        KubernetesPreprocessTarget k8sTarget = new KubernetesPreprocessTarget(
            profile.getKubernetesConfig(),
            profile.buildMinioOptions()
        );
        target = k8sTarget;
        poller = new KubernetesPollingStrategy(
            k8sTarget.getClient(),
            k8sTarget.getConfig().getNamespace()
        );
      } else if ("http".equals(type)) {
        target = new HttpPreprocessTarget(profile.getHttpConfig());
        poller = new MinioPollingStrategy(minioHandler);
      } else {
        throw new IllegalArgumentException(
            "Unsupported target type: " + type
            + ". Supported types: http, kubernetes"
        );
      }

      File inputDir = resolveInputDir();
      String jobId = UUID.randomUUID().toString();
      String minioPrefix = "batch-jobs/" + jobId + "/inputs/";

      // Stage
      output.printInfo("Staging " + inputDir.getName() + " to MinIO (" + minioPrefix + ")...");
      stageDirectory(minioHandler, inputDir, minioPrefix);
      output.printInfo("Staging complete.");

      // Dispatch
      output.printInfo("Dispatching to target...");
      final PreprocessParams params = new PreprocessParams(
          dataFile, variable, units, outputFile.getName(),
          crs, horizCoordName, vertCoordName, timeDim,
          timestep.isBlank() ? null : timestep,
          defaultValue, parallel, amend
      );
      target.dispatch(jobId, minioPrefix, simulation, params);
      output.printInfo("Dispatched.");

      if (noWait) {
        output.printInfo("Job dispatched: " + jobId);
        output.printInfo("Poll manually: batch-status/" + jobId + "/status.json");
        return 0;
      }

      // Poll
      output.printInfo("Polling for completion...");
      JobStatus finalStatus = pollUntilTerminal(poller, jobId);

      if (finalStatus.getState() == JobStatus.State.COMPLETE) {
        // Download result .jshd from MinIO
        String resultPath = "batch-jobs/" + jobId + "/outputs/" + outputFile.getName();
        output.printInfo("Downloading result from MinIO (" + resultPath + ")...");
        minioHandler.downloadFile(resultPath, outputFile);
        output.printInfo("Preprocessing complete. Result: " + outputFile);
        return 0;
      } else {
        output.printError("Preprocessing failed: "
            + finalStatus.getMessage().orElse("unknown error"));
        return DISPATCH_ERROR_CODE;
      }

    } catch (Exception e) {
      output.printError("preprocessBatch failed: " + e.getMessage());
      return TARGET_ERROR_CODE;
    }
  }

  private void stageDirectory(MinioHandler minioHandler, File inputDir, String prefix)
      throws IOException {
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

  private JobStatus pollUntilTerminal(BatchPollingStrategy poller, String jobId) throws Exception {
    long startTime = System.currentTimeMillis();
    long timeoutMs = timeoutSeconds * 1000L;
    long pollIntervalMs = pollIntervalSeconds * 1000L;
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

    return new JobStatus(JobStatus.State.ERROR, "Polling timed out after " + timeoutSeconds
        + "s", null);
  }

  private File resolveInputDir() {
    if (input.isDirectory()) {
      return input;
    }
    return input.getParentFile() != null ? input.getParentFile() : new File(".");
  }
}
