/**
 * Unified model for output destinations supporting multiple protocols.
 *
 * <p>This class represents an output target that can be either a local file,
 * MinIO/S3 object, stdout stream, or memory location. It parses and stores URI
 * components (protocol, host, path) to enable uniform handling of different output
 * types.</p>
 *
 * <p>Supported URI formats:</p>
 * <ul>
 *   <li><b>file://</b> - Local file system (e.g., "file:///tmp/output.txt")</li>
 *   <li><b>minio://</b> - MinIO or S3-compatible storage
 *       (e.g., "minio://bucket/path/output.csv")</li>
 *   <li><b>stdout://</b> - Standard output stream (e.g., "stdout://output")</li>
 *   <li><b>memory://</b> - In-memory storage for web editor
 *       (e.g., "memory://editor/output")</li>
 *   <li><b>Local path</b> - Plain path without protocol (e.g., "/tmp/output.txt")</li>
 * </ul>
 *
 * <p>This class replaces the separate ExportTarget with a unified model that works for both
 * debug and export output systems. It provides the same API as ExportTarget for backward
 * compatibility while being used in the new unified OutputWriter architecture.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

/**
 * Record describing where output should be written.
 *
 * <p>This class encapsulates the components of an output destination URI and provides methods
 * to reconstruct the full URI and extract file type information. All paths parsed via URI
 * (file://, minio://, memory://, stdout://) include leading slashes for consistency.</p>
 */
public class OutputTarget {

  private final String protocol;
  private final String host;
  private final String path;

  /**
   * Constructs an OutputTarget with the specified protocol, host, and path.
   *
   * <p>This constructor is used for URIs that include both a host and path component,
   * such as MinIO URIs where the host represents the bucket name.</p>
   *
   * @param protocol The protocol used for the output target (e.g., "minio", "file", "stdout")
   * @param host The host of the output target, typically representing a bucket name or domain
   * @param path The path representing the location where output should be written
   */
  public OutputTarget(String protocol, String host, String path) {
    this.protocol = protocol;
    this.host = host;
    this.path = path;
  }

  /**
   * Constructs an OutputTarget with an empty host.
   *
   * <p>This constructor is used for URIs that don't have a host component, such as file://
   * and stdout:// URIs where only the protocol and path are meaningful.</p>
   *
   * @param protocol The protocol used for the output target (e.g., "file", "stdout")
   * @param path The path representing the location where output should be written
   */
  public OutputTarget(String protocol, String path) {
    this.protocol = protocol;
    this.host = "";
    this.path = path;
  }

  /**
   * Constructs a local OutputTarget with only a path.
   *
   * <p>This constructor is used for plain local file paths that don't include a protocol
   * prefix (e.g., "/tmp/output.txt" or "output.txt").</p>
   *
   * @param path The local file path where output should be written
   */
  public OutputTarget(String path) {
    this.protocol = "";
    this.host = "";
    this.path = path;
  }

  /**
   * Gets the protocol component.
   *
   * @return The protocol used by this output target (e.g., "file", "minio", "stdout"),
   *         or empty string for local paths
   */
  public String getProtocol() {
    return protocol;
  }

  /**
   * Gets the host component.
   *
   * @return The host of this output target (e.g., bucket name for MinIO),
   *         or empty string if no host is specified
   */
  public String getHost() {
    return host;
  }

  /**
   * Gets the path component.
   *
   * @return The path of this output target where data will be written
   */
  public String getPath() {
    return path;
  }

  /**
   * Gets the file type (extension) from the end of the path.
   *
   * <p>Extracts the file extension without the leading period. This is useful for determining
   * the output format (e.g., "csv", "txt") based on the file path.</p>
   *
   * @return The file extension without a period, or an empty string if no extension is present
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
   * Reconstructs the full URI string from this OutputTarget's components.
   *
   * <p>This method is the inverse of OutputTargetParser.parse() (or ExportTargetParser.parse()),
   * reconstructing a parseable URI string from the protocol, host, and path components.</p>
   *
   * <p>All paths parsed via URI (file://, minio://, memory://, stdout://) have leading slashes,
   * ensuring consistent URI reconstruction without conditional logic.</p>
   *
   * <p>Examples:</p>
   * <ul>
   *   <li>Local path: "/tmp/output.txt" → "/tmp/output.txt"</li>
   *   <li>File URI: file:// + "" + "/tmp/output.txt" → "file:///tmp/output.txt"</li>
   *   <li>MinIO URI: minio:// + "bucket" + "/path/output.csv"
   *       → "minio://bucket/path/output.csv"</li>
   *   <li>Stdout URI: stdout:// + "" + "/output" → "stdout:///output"</li>
   * </ul>
   *
   * @return The full URI string (e.g., "file:///tmp/output.csv", "minio://bucket/path.csv")
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

  /**
   * Checks if this target represents a MinIO/S3 destination.
   *
   * @return true if the protocol is "minio", false otherwise
   */
  public boolean isMinioTarget() {
    return "minio".equalsIgnoreCase(protocol);
  }

  /**
   * Checks if this target represents a local file destination.
   *
   * @return true if the protocol is "file" or empty (local path), false otherwise
   */
  public boolean isFileTarget() {
    return protocol.isEmpty() || "file".equalsIgnoreCase(protocol);
  }

  /**
   * Checks if this target represents stdout.
   *
   * @return true if the protocol is "stdout", false otherwise
   */
  public boolean isStdoutTarget() {
    return "stdout".equalsIgnoreCase(protocol);
  }

  /**
   * Checks if this target represents in-memory storage.
   *
   * @return true if the protocol is "memory", false otherwise
   */
  public boolean isMemoryTarget() {
    return "memory".equalsIgnoreCase(protocol);
  }

  @Override
  public String toString() {
    return "OutputTarget{protocol='" + protocol + "', host='" + host + "', path='" + path + "'}";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    OutputTarget that = (OutputTarget) o;

    if (!protocol.equals(that.protocol)) {
      return false;
    }
    if (!host.equals(that.host)) {
      return false;
    }
    return path.equals(that.path);
  }

  @Override
  public int hashCode() {
    int result = protocol.hashCode();
    result = 31 * result + host.hashCode();
    result = 31 * result + path.hashCode();
    return result;
  }
}
