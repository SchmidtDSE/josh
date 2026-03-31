/**
 * Command line interface handler for running Josh simulations with inputs staged in MinIO.
 *
 * <p>This class implements the 'runFromMinio' command which downloads simulation inputs from
 * MinIO object storage, validates export path safety for batch execution, runs the simulation,
 * and writes a completion marker back to MinIO. This command is the worker-side bootstrap for
 * batch execution targets (Kubernetes, SSH).</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import io.minio.PutObjectArgs;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.joshsim.pipeline.remote.batch.BatchExportValidator;
import org.joshsim.util.MinioHandler;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;


/**
 * Command handler for batch worker execution with MinIO-staged inputs.
 *
 * <p>Downloads simulation inputs from MinIO, validates that export paths are safe for concurrent
 * batch execution (must contain {replicate} and use minio:// protocol), then delegates to the
 * standard run logic. On successful completion, writes a _SUCCESS marker to MinIO.</p>
 *
 * <p>This command is invoked by batch execution targets (K8s pods, SSH processes) and is not
 * typically called directly by users.</p>
 */
@Command(
    name = "runFromMinio",
    description = "Run a simulation with inputs staged in MinIO (batch worker bootstrap)"
)
public class RunFromMinioCommand implements java.util.concurrent.Callable<Integer> {

  private static final int VALIDATION_ERROR_CODE = 10;
  private static final int DOWNLOAD_ERROR_CODE = 11;
  private static final int SIMULATION_ERROR_CODE = 12;
  private static final int MARKER_ERROR_CODE = 13;

  @Option(
      names = "--input-prefix",
      description = "MinIO object prefix containing staged inputs "
                  + "(e.g., job-123/input/)",
      required = true
  )
  private String inputPrefix;

  @Option(
      names = "--replicate-id",
      description = "Replicate index for this worker (0-based)",
      required = true
  )
  private int replicateId;

  @Option(
      names = "--simulation",
      description = "Name of the simulation to execute",
      required = true
  )
  private String simulation;

  @Option(
      names = "--use-float-64",
      description = "Use double instead of BigDecimal, offering speed but lower precision.",
      defaultValue = "false"
  )
  private boolean useFloat64;

  @Option(
      names = "--serial-patches",
      description = "Run patches in serial instead of parallel",
      defaultValue = "false"
  )
  private boolean serialPatches;

  @Mixin
  private OutputOptions output = new OutputOptions();

  @Mixin
  private MinioOptions minioOptions = new MinioOptions();

  @Override
  public Integer call() {
    try {
      return executeWorker();
    } catch (Exception e) {
      output.printError("Worker failed: " + e.getMessage());
      return SIMULATION_ERROR_CODE;
    }
  }

  /**
   * Executes the full worker bootstrap flow.
   *
   * @return exit code (0 = success)
   * @throws Exception if any step fails
   */
  private Integer executeWorker() throws Exception {
    output.printInfo("RunFromMinio worker starting: replicate=" + replicateId
        + " prefix=" + inputPrefix);

    // 1. Download inputs from MinIO to temp directory
    File tempDir = downloadInputs();
    if (tempDir == null) {
      return DOWNLOAD_ERROR_CODE;
    }

    // 2. Find the josh file in the downloaded inputs
    File joshFile = findJoshFile(tempDir);
    if (joshFile == null) {
      output.printError("No .josh file found in downloaded inputs at " + tempDir.getPath());
      return DOWNLOAD_ERROR_CODE;
    }

    output.printInfo("Found simulation file: " + joshFile.getName());

    // 3. Validate export paths (worker-side safety net)
    Integer validationResult = validateExportPaths(joshFile);
    if (validationResult != 0) {
      return validationResult;
    }

    // 4. Run the simulation with replicates=1
    Integer simResult = runSimulation(joshFile, tempDir);
    if (simResult != 0) {
      return simResult;
    }

    // 5. Write _SUCCESS marker to MinIO
    Integer markerResult = writeSuccessMarker();
    if (markerResult != 0) {
      return markerResult;
    }

    output.printInfo("Worker completed successfully: replicate=" + replicateId);
    return 0;
  }

  /**
   * Downloads all staged inputs from MinIO to a local temp directory.
   *
   * @return the temp directory containing downloaded files, or null on failure
   */
  private File downloadInputs() {
    try {
      File tempDir = Files.createTempDirectory("josh-worker-" + replicateId).toFile();
      tempDir.deleteOnExit();

      MinioHandler handler = new MinioHandler(minioOptions, output);
      int downloaded = handler.downloadDirectory(inputPrefix, tempDir);

      if (downloaded == 0) {
        output.printError("No files downloaded from " + inputPrefix);
        return null;
      }

      output.printInfo("Downloaded " + downloaded + " files from MinIO");
      return tempDir;
    } catch (Exception e) {
      output.printError("Failed to download inputs: " + e.getMessage());
      return null;
    }
  }

  /**
   * Finds the .josh simulation file in the given directory.
   *
   * @param dir the directory to search
   * @return the .josh file, or null if not found
   */
  private File findJoshFile(File dir) {
    File[] joshFiles = dir.listFiles((d, name) -> name.endsWith(".josh"));
    if (joshFiles != null && joshFiles.length > 0) {
      return joshFiles[0];
    }

    // Check subdirectories (inputs may be in nested structure)
    File[] subdirs = dir.listFiles(File::isDirectory);
    if (subdirs != null) {
      for (File subdir : subdirs) {
        File found = findJoshFile(subdir);
        if (found != null) {
          return found;
        }
      }
    }

    return null;
  }

  /**
   * Validates that all export paths in the simulation are safe for batch execution.
   *
   * <p>Delegates to {@link BatchExportValidator} which checks that all export paths
   * use minio:// protocol and contain {replicate}.</p>
   *
   * @param joshFile the simulation file to validate
   * @return 0 if valid, error code otherwise
   */
  Integer validateExportPaths(File joshFile) {
    List<String> errors = BatchExportValidator.validate(joshFile, simulation, output);

    for (String error : errors) {
      output.printError(error);
    }

    if (!errors.isEmpty()) {
      return VALIDATION_ERROR_CODE;
    }

    output.printInfo("Export path validation passed");
    return 0;
  }

  /**
   * Runs the simulation by invoking RunCommand programmatically.
   *
   * @param joshFile the simulation file
   * @param workDir the working directory containing data files
   * @return exit code from RunCommand
   */
  private Integer runSimulation(File joshFile, File workDir) {
    output.printInfo("Starting simulation: " + simulation + " replicate=" + replicateId);

    // Build CLI args for RunCommand
    java.util.List<String> args = new java.util.ArrayList<>();
    args.add(joshFile.getAbsolutePath());
    args.add(simulation);
    args.add("--replicates=1");

    if (useFloat64) {
      args.add("--use-float-64");
    }

    if (serialPatches) {
      args.add("--serial-patches");
    }

    // Pass through MinIO options for export path resolution
    addMinioArgs(args);

    // Set custom tag for replicate so {replicate} resolves correctly
    args.add("--custom-tag=replicate=" + replicateId);

    output.printInfo("Delegating to run command with args: " + String.join(" ", args));

    // Execute via picocli to reuse RunCommand's full logic
    picocli.CommandLine cmd = new picocli.CommandLine(new RunCommand());
    int exitCode = cmd.execute(args.toArray(new String[0]));

    if (exitCode != 0) {
      output.printError("Simulation failed with exit code: " + exitCode);
      return SIMULATION_ERROR_CODE;
    }

    return 0;
  }

  /**
   * Adds MinIO connection arguments to the command args list.
   *
   * @param args the args list to append to
   */
  private void addMinioArgs(java.util.List<String> args) {
    try {
      String endpoint = minioOptions.getMinioEndpoint();
      if (endpoint != null && !endpoint.isEmpty()) {
        args.add("--minio-endpoint=" + endpoint);
      }
    } catch (IllegalStateException e) {
      // Endpoint not configured
    }

    try {
      String bucket = minioOptions.getBucketName();
      if (bucket != null && !bucket.isEmpty()) {
        args.add("--minio-bucket=" + bucket);
      }
    } catch (IllegalStateException e) {
      // Bucket not configured
    }

    String objectPath = minioOptions.getObjectPath();
    if (objectPath != null && !objectPath.isEmpty()) {
      args.add("--minio-path=" + objectPath);
    }
  }

  /**
   * Writes a _SUCCESS marker to MinIO indicating this replicate completed.
   *
   * @return 0 if successful, error code otherwise
   */
  private Integer writeSuccessMarker() {
    try {
      String markerPath = inputPrefix;
      if (!markerPath.endsWith("/")) {
        markerPath += "/";
      }
      // Write marker alongside outputs, not inputs
      markerPath = markerPath.replace("/input/", "/output/replicate-" + replicateId + "/");
      markerPath += "_SUCCESS";

      byte[] content = ("replicate=" + replicateId + "\n").getBytes(StandardCharsets.UTF_8);

      minioOptions.getMinioClient().putObject(
          PutObjectArgs.builder()
              .bucket(minioOptions.getBucketName())
              .object(markerPath)
              .stream(new ByteArrayInputStream(content), content.length, -1)
              .contentType("text/plain")
              .build()
      );

      output.printInfo("Wrote success marker: minio://" + minioOptions.getBucketName()
          + "/" + markerPath);
      return 0;
    } catch (Exception e) {
      output.printError("Failed to write success marker: " + e.getMessage());
      return MARKER_ERROR_CODE;
    }
  }
}
