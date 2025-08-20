/**
 * Implementation providing mapped file input access.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Strategy providing mapped file access.
 *
 * <p>Implementation of JvmInputGetter that loads files from a predefined mapping of filenames
 * to actual file paths. This allows selective file access for scenarios where only specific
 * files should be available to the simulation, such as remote execution where network
 * transmission should be limited to explicitly specified files.</p>
 */
public class JvmMappedInputGetter extends JvmInputGetter {

  private final Map<String, String> fileMapping;

  /**
   * Create a new mapped input getter with the specified file mapping.
   *
   * @param fileMapping Map from filename to actual file path. This mapping defines which
   *                   files are accessible to the simulation.
   */
  public JvmMappedInputGetter(Map<String, String> fileMapping) {
    this.fileMapping = Collections.unmodifiableMap(new HashMap<>(fileMapping));
  }

  @Override
  protected InputStream readNamePath(String identifier) {
    return loadFromMappedFiles(identifier);
  }

  @Override
  protected boolean checkNamePathExists(String identifier) {
    return fileMapping.containsKey(identifier);
  }

  /**
   * Load a file from the mapped files.
   *
   * @param name The name of the file to load from the mapping.
   * @return The input stream for the file at the mapped path.
   * @throws RuntimeException raised if file not found in mapping or error in opening stream.
   */
  private InputStream loadFromMappedFiles(String name) {
    String actualPath = fileMapping.get(name);
    if (actualPath == null) {
      throw new RuntimeException("File not found in mapped files: " + name);
    }
    return loadFromWorkingDir(actualPath);
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

  /**
   * Get a copy of the file mapping used by this input getter.
   *
   * @return An unmodifiable map of the current file mapping.
   */
  public Map<String, String> getFileMapping() {
    return fileMapping;
  }

}