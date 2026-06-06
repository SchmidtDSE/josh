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
import java.util.concurrent.Callable;
import org.joshsim.util.MinioHandler;
import org.joshsim.util.MinioHandler.StagedState;
import org.joshsim.util.MinioOptions;
import org.joshsim.util.OutputOptions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;


/**
 * Uploads all files in a local directory to a MinIO prefix.
 *
 * <p>Each file is uploaded to {@code <bucket>/<prefix>/<relative-path>} where relative-path
 * preserves the directory structure under the input directory. A
 * {@link MinioHandler#STAGED_SENTINEL_FILENAME} sentinel is written at the root of the
 * prefix recording the staging lifecycle ({@code staging} → {@code complete}, or
 * {@code error} if the upload fails).</p>
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
      String normalizedPrefix = MinioHandler.normalizePrefix(prefix);

      minio.writeStagedSentinel(normalizedPrefix, StagedState.STAGING, null);
      int uploaded;
      try {
        uploaded = minio.uploadDirectory(inputDir, normalizedPrefix);
      } catch (IOException uploadError) {
        try {
          minio.writeStagedSentinel(normalizedPrefix, StagedState.ERROR, uploadError.getMessage());
        } catch (IOException sentinelError) {
          output.printError("Additionally failed to write error sentinel: "
              + sentinelError.getMessage());
        }
        throw uploadError;
      }
      minio.writeStagedSentinel(normalizedPrefix, StagedState.COMPLETE, null);
      output.printInfo("Staged " + uploaded + " file(s) to " + normalizedPrefix);
      return 0;

    } catch (Exception e) {
      output.printError("stageToMinio failed: " + e.getMessage());
      return MINIO_ERROR_CODE;
    }
  }
}
