/**
 * Logic for using non-sandboxed inputs.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;


/**
 * Strategy providing non-sandboxed input access.
 *
 * <p>Strategy which loads streams from regular resources through the JVM's utilities for working
 * with the native file system and with the network outside of the sandbox.</p>
 */
public class JvmInputGetter implements InputGetterStrategy {

  @Override
  public InputStream open(String identifier) {
    if (identifier.contains(":")) {
      URI uri = URI.create(identifier);
      return loadFromUri(uri);
    } else {
      return loadFromWorkingDir(identifier);
    }
  }

  @Override
  public boolean exists(String identifier) {
    if (identifier.contains(":")) {
      // For URIs, we would need to try to open it to check existence
      // For now, assume URIs exist (they might fail when actually opened)
      return true;
    } else {
      // For files, check if the file exists in the working directory
      File file = new File(identifier);
      return file.exists();
    }
  }

  /**
   * Load this resource from a URI.
   *
   * @param uri The URI from which to load the resource.
   * @return The input stream found at the given URI.
   * @throws RuntimeException raised if error encountered in opening the input stream.
   */
  private InputStream loadFromUri(URI uri) {
    try {
      return uri.toURL().openStream();
    } catch (Exception e) {
      throw new RuntimeException("Failed to open stream from URI: " + uri, e);
    }
  }

  /**
   * Load a file from the working directory.
   *
   * @param name The name of the file to load from working directory.
   * @return The input stream for the file at the working directory.
   * @throws RuntimeException raised if error encountered in opening the input stream.
   */
  private InputStream loadFromWorkingDir(String name) {
    try {
      return new FileInputStream(name);
    } catch (Exception e) {
      throw new RuntimeException("Failed to open stream from working directory: " + name, e);
    }
  }

}
