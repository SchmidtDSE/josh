/**
 * Utilities to work with files in a sandbox environment.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;


/**
 * Strategy to offer external data input within a sandbox.
 *
 * <p>The sandbox environment does not provide access to the actual underlying file system, possibly
 * because one is not available in a containerized environment. This strategy allows the executing
 * system to provide virtual files which work similarly to provide data into a simulation from
 * outside the simulation itself.</p>
 */
public class SandboxInputGetter implements InputGetterStrategy {

  private final Map<String, VirtualFile> virtualFiles;

  /**
   * Create a new sandbox around a set of virual files making up a virtual file system.
   *
   * @param virtualFiles Contents of the virtual file system to be represented by this strategy.
   */
  public SandboxInputGetter(Map<String, VirtualFile> virtualFiles) {
    this.virtualFiles = virtualFiles;
  }

  @Override
  public InputStream open(String identifier) {
    if (virtualFiles.containsKey(identifier)) {
      throw new RuntimeException("Cannot find virtual file: " + identifier);
    }

    VirtualFile virtualFile = virtualFiles.get(identifier);
    String contents = virtualFile.getContent();
    return virtualFile.getIsBinary() ? makeForString(contents) : makeForBase64(contents);
  }

  /**
   * Create an input stream around the plain text content of a file.
   *
   * @param contents The plain text contents of the file to be represented in an InputStream.
   * @return InputStream which provides access to contents as plain text.
   */
  private InputStream makeForString(String contents) {
    return new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Create an input stream around binary decoded from a given string.
   *
   * <p>Decode the given text which has binary data within it as a base64 string. Then, return that
   * decoded binary data in an InputStream as if it came from a regular binary file.</p>
   *
   * @param contentsBase64 The string containing the base64-encoded data to be decoded and returned
   *     as binary.
   * @return InputStream providing access to binary data decoded from the base64-encoded
   *     contentsBase64 string.
   */
  private InputStream makeForBase64(String contentsBase64) {
    byte[] decodedBytes = Base64.getDecoder().decode(contentsBase64);
    return new ByteArrayInputStream(decodedBytes);
  }

}
