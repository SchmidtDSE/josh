/**
 * Structures to describe output locations.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.export;


/**
 * Record describing where an export should be written.
 */
public class ExportTarget {

  private final String protocol;
  private final String host;
  private final String path;

  /**
   * Constructs an ExportTarget object with the specified protocol, host, and path.
   *
   * @param protocol The protocol used for the export target (e.g., "minio", "file").
   * @param host The host of the export target, typically representing the domain or IP address.
   * @param path The path representing the location on the export target where the export should
   *     occur.
   */
  public ExportTarget(String protocol, String host, String path) {
    this.protocol = protocol;
    this.host = host;
    this.path = path;
  }

  /**
   * Constructs a target with an empty host.
   *
   * @param protocol The protocol used for the export target (e.g., "minio", "file").
   * @param path The path representing the location on the export target where the export should
   *     occur.
   */
  public ExportTarget(String protocol, String path) {
    this.protocol = protocol;
    this.host = "";
    this.path = path;
  }

  /**
   * Constructs a local target with only a path.
   *
   * @param path The path representing the location on the export target where the export should
   *     occur.
   */
  public ExportTarget(String path) {
    this.protocol = "";
    this.host = "";
    this.path = path;
  }

  /**
   * Get the protocol.
   *
   * @return the protocol used by this export target.
   */
  public String getProtocol() {
    return protocol;
  }

  /**
   * Get the host.
   *
   * @return the host of this export target.
   */
  public String getHost() {
    return host;
  }

  /**
   * Get the path.
   *
   * @return the path of this export target.
   */
  public String getPath() {
    return path;
  }

  /**
   * Get the file type (extension) from the end of the path.
   *
   * @return the file extension without a period, or an empty string if no extension is present.
   */
  public String getFileType() {
    int lastDotIndex = path.lastIndexOf('.');
    if (lastDotIndex == -1) {
      return "";
    }

    if (lastDotIndex > path.length() - 1) {
      return "";
    }

    return path.substring(lastDotIndex + 1);
  }

}
