/**
 * Structures to describe output locations.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;


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

  /**
   * Reconstructs the full URI string from this ExportTarget's components.
   *
   * <p>This method is the inverse of ExportTargetParser.parse(), reconstructing
   * a parseable URI string from the protocol, host, and path components.</p>
   *
   * <p>All paths parsed via URI (file://, minio://, memory://) have leading slashes,
   * ensuring consistent URI reconstruction without conditional logic.</p>
   *
   * @return the full URI string (e.g., "file:///tmp/output.csv", "minio://bucket/path.csv")
   */
  public String toUri() {
    // Handle special case: local path only (no protocol)
    if (protocol.isEmpty()) {
      return path;
    }

    // Build URI: protocol://host/path (or protocol://path if host is empty)
    // All paths from URI parsing include leading slash, so no separator needed
    if (host == null || host.isEmpty()) {
      return protocol + "://" + path;
    } else {
      return protocol + "://" + host + path;
    }
  }

}
