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
    try {
      String targetClean = target.replaceAll("\"", "");
      URI uri = new URI(targetClean);
      String scheme = uri.getScheme();
      if ("file".equalsIgnoreCase(scheme)) {
        return new ExportTarget(uri.getPath());
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
