/**
 * Implementation providing working directory input access.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;


/**
 * Strategy providing working directory file access.
 *
 * <p>Implementation of JvmInputGetter that loads files from the working directory,
 * preserving the original behavior of the JvmInputGetter class. This is the default
 * strategy for all commands except RunCommand with --data option.</p>
 */
public class JvmWorkingDirInputGetter extends JvmInputGetter {

  @Override
  protected InputStream readImpliedPath(String identifier) {
    return loadFromWorkingDir(identifier);
  }

  @Override
  protected boolean checkImpliedPathExists(String identifier) {
    File file = new File(identifier);
    return file.exists();
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