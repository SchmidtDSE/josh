/**
 * Shared utility for staging files from MinIO to a local directory.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.util;

import java.io.File;
import java.io.IOException;
import java.util.List;


/**
 * Downloads all objects under a MinIO prefix to a local directory.
 *
 * <p>Used by both {@link org.joshsim.command.StageFromMinioCommand} (CLI) and
 * {@link org.joshsim.cloud.JoshSimBatchHandler} (server endpoint) to avoid
 * duplicating staging logic.</p>
 */
public class MinioStagingUtil {

  /**
   * Downloads all objects under a MinIO prefix to a local directory.
   *
   * <p>Each object is written to {@code outputDir/<relative-path>} where the relative path
   * is the object key with the prefix stripped. Parent directories are created as needed.</p>
   *
   * @param minio The MinIO handler for download operations.
   * @param prefix The object prefix to download from (e.g., {@code batch-jobs/abc/inputs/}).
   * @param outputDir The local directory to download files into. Must exist.
   * @param output For logging progress and errors.
   * @return The number of files downloaded.
   * @throws IOException If directory creation or file download fails.
   * @throws IllegalArgumentException If no objects are found under the prefix.
   */
  public static int stageFromMinio(MinioHandler minio, String prefix, File outputDir,
      OutputOptions output) throws Exception {
    String normalizedPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
    List<String> keys = minio.listObjects(normalizedPrefix);

    if (keys.isEmpty()) {
      throw new IllegalArgumentException("No objects found under prefix: " + normalizedPrefix);
    }

    int downloaded = 0;
    for (String key : keys) {
      String relativePath = key.substring(normalizedPrefix.length());
      if (relativePath.isEmpty()) {
        continue;
      }
      File destination = new File(outputDir, relativePath);

      File parentDir = destination.getParentFile();
      if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
        throw new IOException("Failed to create directory: " + parentDir.getPath());
      }

      minio.downloadFile(key, destination);
      downloaded++;
    }

    output.printInfo("Downloaded " + downloaded + " file(s) to " + outputDir.getPath());
    return downloaded;
  }

  private MinioStagingUtil() {
    // Utility class
  }
}
