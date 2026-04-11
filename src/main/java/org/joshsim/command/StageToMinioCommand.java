/**
 * Command for staging local files to MinIO.
 *
 * <p>Uploads files to a MinIO prefix, preserving directory structure relative to a base
 * directory. Works like rsync from local to MinIO — the prefix is the remote "directory"
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
import picocli.CommandLine.Parameters;


/**
 * Uploads local files or directory contents to a MinIO prefix.
 *
 * <p>If a directory is given, all files within it are uploaded recursively with their
 * relative paths preserved under the prefix. If individual files are given, they are
 * uploaded flat under the prefix.</p>
 */
@Command(
    name = "stageToMinio",
    description = "Upload local files to a MinIO prefix"
)
public class StageToMinioCommand implements Callable<Integer> {

  private static final int MINIO_ERROR_CODE = 100;

  @Parameters(description = "Local files or directory to upload")
  private List<File> paths;

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
      MinioHandler minio = new MinioHandler(minioOptions, output);
      String normalizedPrefix = normalizePrefix(prefix);

      int uploaded = 0;
      for (File path : paths) {
        if (!path.exists()) {
          output.printError("Path not found: " + path.getPath());
          return 1;
        }

        if (path.isDirectory()) {
          uploaded += uploadDirectory(minio, path, normalizedPrefix);
        } else {
          String objectPath = normalizedPrefix + path.getName();
          if (!minio.uploadFile(path, objectPath)) {
            return MINIO_ERROR_CODE;
          }
          uploaded++;
        }
      }

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
