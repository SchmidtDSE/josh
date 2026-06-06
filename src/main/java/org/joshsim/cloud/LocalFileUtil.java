/**
 * Utility methods for local file discovery used by server-side handlers.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * Provides file discovery utilities for handlers that operate on local directories.
 *
 * <p>These methods support handlers like {@link JoshSimBatchHandler} that expect
 * pre-staged simulation files in a local directory.</p>
 */
public class LocalFileUtil {

  /**
   * Builds a mapping of filenames to absolute paths for all files in a directory.
   *
   * <p>Only top-level files are included (not recursive). This mapping is suitable
   * for constructing a {@link org.joshsim.lang.io.JvmMappedInputGetter} so the
   * simulation can resolve input files by name.</p>
   *
   * @param directory The directory to scan for files.
   * @return A map from filename to absolute file path.
   */
  public static Map<String, String> buildFileMapping(File directory) {
    Map<String, String> mapping = new HashMap<>();
    File[] files = directory.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isFile()) {
          mapping.put(file.getName(), file.getAbsolutePath());
        }
      }
    }
    return mapping;
  }

  /**
   * Finds the single Josh script file in a directory.
   *
   * @param directory The directory to search for {@code .josh} files.
   * @return The Josh script file.
   * @throws IOException If no {@code .josh} file is found or multiple are present.
   */
  public static File findScriptFile(File directory) throws IOException {
    File[] joshFiles = directory.listFiles((dir, name) -> name.endsWith(".josh"));
    if (joshFiles == null || joshFiles.length == 0) {
      throw new IOException("No .josh script found in staged files");
    }
    if (joshFiles.length > 1) {
      throw new IOException(
          "Multiple .josh scripts found in staged files; expected exactly one"
      );
    }
    return joshFiles[0];
  }

  private LocalFileUtil() {
    // Utility class
  }
}
