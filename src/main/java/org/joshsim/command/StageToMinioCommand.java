/**
 * Command for staging local files to MinIO.
 *
 * <p>Uploads all files in a local directory to a MinIO prefix, preserving directory
 * structure. Works like rsync from local to MinIO — the prefix is the remote "directory"
 * and the local structure is mirrored under it.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import org.joshsim.util.MinioHandler;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;


/**
 * Uploads all files in a local directory to a MinIO prefix.
 *
 * <p>Each file is uploaded to {@code <bucket>/<prefix>/<relative-path>} where relative-path
 * preserves the directory structure under the input directory.</p>
 */
@Command(
    name = "stageToMinio",
    description = "Upload a local directory to a MinIO prefix"
)
public class StageToMinioCommand implements Callable<Integer> {

  private static final int MINIO_ERROR_CODE = 100;

  @Option(
      names = "--input-dir",
      description = "Local directory to upload",
      required = true
  )
  private File inputDir;

  @Option(
      names = "--prefix",
      description = "MinIO object prefix (e.g. batch-jobs/abc/inputs/)",
      required = true
  )
  private String prefix;

  @Mixin
  private MinioOptions minioOptions = new MinioOptions();

  @Mixin
  private OutputOptions output = new OutputOptions();

  @Override
  public Integer call() {
    try {
      if (!inputDir.exists()) {
        output.printError("Input directory not found: " + inputDir.getPath());
        return 1;
      }
      if (!inputDir.isDirectory()) {
        output.printError("Not a directory: " + inputDir.getPath());
        return 1;
      }

      MinioHandler minio = new MinioHandler(minioOptions, output);
      String normalizedPrefix = normalizePrefix(prefix);

      int uploaded = uploadDirectory(minio, inputDir, normalizedPrefix);
      output.printInfo("Staged " + uploaded + " file(s) to " + prefix);
      return 0;

    } catch (Exception e) {
      output.printError("stageToMinio failed: " + e.getMessage());
      return MINIO_ERROR_CODE;
    }
  }

  private int uploadDirectory(MinioHandler minio, File dir, String prefix) throws IOException {
    Path basePath = dir.toPath();
    int count = 0;
    try (Stream<Path> walker = Files.walk(basePath)) {
      List<Path> files = walker.filter(Files::isRegularFile).toList();
      for (Path file : files) {
        String relativePath = basePath.relativize(file).toString();
        String objectPath = prefix + relativePath;
        if (!minio.uploadFile(file.toFile(), objectPath)) {
          throw new IOException("Failed to upload " + file);
        }
        count++;
      }
    }
    return count;
  }

  private String normalizePrefix(String prefix) {
    if (prefix.endsWith("/")) {
      return prefix;
    }
    return prefix + "/";
  }
}
