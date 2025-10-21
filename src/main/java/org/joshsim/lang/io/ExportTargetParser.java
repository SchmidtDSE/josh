/**
 * Tools to parse an ExportTarget.
 */

package org.joshsim.lang.io;

import java.net.URI;
import java.net.URISyntaxException;


/**
 * Utility class for parsing export target strings into structured ExportTarget objects.
 *
 * <p>This class provides functionality to interpret and validate string descriptions of export
 * destinations, such as local paths or remote locations, and converts them into structured
 * ExportTarget instances.</p>
 */
public class ExportTargetParser {

  /**
   * Parse an export target from a string describing the location where records should be written.
   *
   * @param target String description indicating where the file should be written. This could be,
   *               for example, "file://path/to/file.geotiff" or "file:///path/to/file.avro" for
   *               local or "minio://host-name/bucket/path.csv" minio remote.
   * @return The parsed ExportTarget.
   * @throws IllegalArgumentException if the target string is invalid or unsupported.
   */
  public static ExportTarget parse(String target) {
    String targetClean = target.replaceAll("\"", "");

    if (targetClean.startsWith("memory://editor/")) {
      return parseMemory(targetClean);
    } else {
      return parseUri(targetClean);
    }
  }

  /**
   * Parses a memory-based export target string.
   *
   * @param target The target string starting with "memory://editor/" after which the path is
   *     found.
   * @return An ExportTarget configured for memory-based export.
   */
  private static ExportTarget parseMemory(String target) {
    // Use standard URI parsing for consistency with file:// and minio://
    // This ensures path always has leading slash, simplifying toUri() logic
    try {
      URI uri = new URI(target);
      String host = uri.getHost() != null ? uri.getHost() : "";
      return new ExportTarget("memory", host, uri.getPath());
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid memory target format: " + target, e);
    }
  }

  /**
   * Parses a non-memory URI-based export target string.
   *
   * @param target The target string in URI format (e.g., "file:/path/to/file" or
   *     "minio://host/path")
   * @return An ExportTarget configured based on the URI scheme.
   * @throws IllegalArgumentException if the URI scheme is unsupported or the URI syntax is
   *     invalid.
   */
  private static ExportTarget parseUri(String target) {
    try {
      URI uri = new URI(target);
      String scheme = uri.getScheme();
      if ("file".equalsIgnoreCase(scheme)) {
        String host = uri.getHost() != null ? uri.getHost() : "";
        return new ExportTarget("file", host, uri.getPath());
      } else if ("minio".equalsIgnoreCase(scheme)) {
        return new ExportTarget("minio", uri.getHost(), uri.getPath());
      } else {
        throw new IllegalArgumentException("Unsupported target scheme: " + scheme);
      }
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid target format: " + target, e);
    }
  }

}
