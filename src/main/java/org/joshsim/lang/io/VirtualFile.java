/**
 * Structures to support use of sandboxed files.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;


/**
 * Record describing a file available in the Sandbox.
 *
 * <p>A virtual or simulated file available as external input or external resources within a Josh
 * sandbox environment.</p>
 */
public class VirtualFile {

  private final String path;
  private final String content;
  private final boolean isBinary;

  /**
   * Create a new record of a virtual file.
   *
   * @param path The path at which this file can be found or the path that this occupies.
   * @param content The content of the file where binary files are encoded in base64 and text files
   *     are provided as it should be reported to client code, both as strings.
   * @param isBinary Flag indicating if the content is base64 encoded binary or regular text files.
   *     True if content is binary reported as a string through base64 encoding and false if the
   *     the file is a regular text file.
   */
  public VirtualFile(String path, String content, boolean isBinary) {
    this.path = path;
    this.content = content;
    this.isBinary = isBinary;
  }

  /**
   * Retrieves the path associated with this virtual file.
   *
   * @return the path representing the location of this virtual file within the sandbox environment.
   */
  public String getPath() {
    return path;
  }

  /**
   * Retrieves the content of the virtual file.
   *
   * @return the content of the file as a string, where binary files are encoded in base64 or text
   *     files are returned as plain text.
   */
  public String getContent() {
    return content;
  }

  /**
   * Determines whether the virtual file represents binary data.
   *
   * @return true if the file content is binary (encoded as base64), false if it is a regular text
   *     file.
   */
  public boolean getIsBinary() {
    return isBinary;
  }

}
