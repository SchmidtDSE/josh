/**
 * Command for staging files from MinIO to a local directory.
 *
 * <p>Downloads all objects under a MinIO prefix to a local directory. Used on workers
 * to retrieve staged simulation inputs before running commands like {@code run} or
 * {@code preprocess} against them.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;
import org.joshsim.util.MinioHandler;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;


/**
 * Downloads all objects under a MinIO prefix to a local directory.
 *
 * <p>Each object is written to {@code <output-dir>/<filename>} where filename is the
 * object key with the prefix stripped. The output directory is created if it does not
 * exist.</p>
 */
@Command(
    name = "stageFromMinio",
    description = "Download all files under a MinIO prefix to a local directory"
)
public class StageFromMinioCommand implements Callable<Integer> {

  private static final int MINIO_ERROR_CODE = 100;

  @Option(
      names = "--prefix",
      description = "MinIO object prefix to download from (e.g. batch-jobs/abc/inputs/)",
      required = true
  )
  private String prefix;

  @Option(
      names = "--output-dir",
      description = "Local directory to download files into",
      required = true
  )
  private File outputDir;

  @Mixin
  private MinioOptions minioOptions = new MinioOptions();

  @Mixin
  private OutputOptions output = new OutputOptions();

  @Override
  public Integer call() {
    try {
      MinioHandler minio = new MinioHandler(minioOptions, output);

      if (!outputDir.exists() && !outputDir.mkdirs()) {
        output.printError("Failed to create output directory: " + outputDir.getPath());
        return 1;
      }

      String normalizedPrefix = normalizePrefix(prefix);
      List<String> keys = minio.listObjects(normalizedPrefix);

      if (keys.isEmpty()) {
        output.printError("No objects found under prefix: " + normalizedPrefix);
        return 1;
      }

      int downloaded = 0;
      for (String key : keys) {
        String relativePath = key.substring(normalizedPrefix.length());
        if (relativePath.isEmpty()) {
          continue;
        }
        File destination = new File(outputDir, relativePath);

        // Create parent directories for nested objects
        File parentDir = destination.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
          output.printError("Failed to create directory: " + parentDir.getPath());
          return 1;
        }

        minio.downloadFile(key, destination);
        downloaded++;
      }

      output.printInfo("Downloaded " + downloaded + " file(s) to " + outputDir.getPath());
      return 0;

    } catch (Exception e) {
      output.printError("stageFromMinio failed: " + e.getMessage());
      return MINIO_ERROR_CODE;
    }
  }

  private String normalizePrefix(String prefix) {
    if (prefix.endsWith("/")) {
      return prefix;
    }
    return prefix + "/";
  }
}
