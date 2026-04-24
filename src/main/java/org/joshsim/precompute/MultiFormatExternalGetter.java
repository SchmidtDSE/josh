/**
 * Composite external resource getter that supports both jshd and jshdz formats.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import java.io.FileNotFoundException;
import java.nio.file.NoSuchFileException;
import org.joshsim.lang.bridge.ExternalResourceGetter;


/**
 * Composite {@link ExternalResourceGetter} that routes requests by file extension.
 *
 * <p>Supports {@code .jshd} files via {@link JshdExternalGetter} and {@code .jshdz} (XZ-compressed)
 * files via {@link JshdzExternalGetter}. Names that end in a known extension route directly;
 * bare names (no extension) probe {@code .jshdz} first and fall back to {@code .jshd}, letting
 * simulation authors reference an external resource without committing to a compression choice.</p>
 *
 * <p>This class is JVM-only and must not be referenced from any code path compiled by TeaVM.</p>
 */
public class MultiFormatExternalGetter implements ExternalResourceGetter {

  private final JshdExternalGetter jshdGetter;
  private final JshdzExternalGetter jshdzGetter;

  /**
   * Construct a multi-format getter backed by the two format-specific getters.
   *
   * @param jshdGetter Getter for uncompressed {@code .jshd} files.
   * @param jshdzGetter Getter for XZ-compressed {@code .jshdz} files.
   */
  public MultiFormatExternalGetter(JshdExternalGetter jshdGetter, JshdzExternalGetter jshdzGetter) {
    this.jshdGetter = jshdGetter;
    this.jshdzGetter = jshdzGetter;
  }

  @Override
  public DataGridLayer getResource(String name) {
    if (name.endsWith(".jshdz")) {
      return jshdzGetter.getResource(name);
    }
    if (name.endsWith(".jshd")) {
      return jshdGetter.getResource(name);
    }
    // Bare name: try compressed first (authors typically compress when they can),
    // then fall back to uncompressed. Only swallow file-not-found — other failures
    // (corrupt file, decompression error, permission denied) must surface.
    try {
      return jshdzGetter.getResource(name + ".jshdz");
    } catch (RuntimeException compressedMiss) {
      if (!isFileNotFound(compressedMiss)) {
        throw compressedMiss;
      }
      try {
        return jshdGetter.getResource(name + ".jshd");
      } catch (RuntimeException uncompressedMiss) {
        if (!isFileNotFound(uncompressedMiss)) {
          throw uncompressedMiss;
        }
        throw new RuntimeException(
            "External resource not found as " + name + ".jshdz or " + name + ".jshd",
            compressedMiss
        );
      }
    }
  }

  private static boolean isFileNotFound(Throwable thrown) {
    for (Throwable cursor = thrown; cursor != null; cursor = cursor.getCause()) {
      if (cursor instanceof FileNotFoundException || cursor instanceof NoSuchFileException) {
        return true;
      }
    }
    return false;
  }

}
