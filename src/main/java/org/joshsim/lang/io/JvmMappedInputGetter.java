/**
 * Implementation providing mapped file input access.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
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
    return resolveMappedPath(identifier) != null;
  }

  /**
   * Resolve a requested name to a mapped file path, or null when there is no acceptable mapping.
   *
   * <p>An exact key match always wins. Otherwise, when the name carries an extension, the
   * extension-stripped base is tried too, so a bare mapping key (e.g. {@code editor}) can satisfy
   * an extension-qualified lookup (e.g. {@code editor.jshc} produced by config resolution).</p>
   *
   * <p>The stripped fallback is suppressed when it would cross the {@code .jshd}/{@code .jshdz}
   * divide. {@link org.joshsim.precompute.MultiFormatExternalGetter} resolves a bare
   * {@code external} reference by probing {@code name.jshdz} then {@code name.jshd}; without this
   * guard a key mapped to a plain {@code .jshd} file would satisfy the {@code .jshdz} probe and
   * hand the XZ reader uncompressed bytes ("Input is not in the XZ format"). Returning null for
   * the mismatched probe lets the dispatcher fall through to the format that actually matches.</p>
   *
   * @param name The requested name.
   * @return The mapped path, or null if no acceptable mapping exists.
   */
  private String resolveMappedPath(String name) {
    String exact = fileMapping.get(name);
    if (exact != null) {
      return exact;
    }
    if (name.contains(".")) {
      String nameWithoutExt = name.substring(0, name.lastIndexOf('.'));
      String candidate = fileMapping.get(nameWithoutExt);
      if (candidate != null && stripFallbackAllowed(name, candidate)) {
        return candidate;
      }
    }
    return null;
  }

  /**
   * Whether the extension-stripped fallback may bind {@code requested} to {@code mappedPath}.
   *
   * <p>Only genuine {@code .jshd}/{@code .jshdz} format conflicts are suppressed; every other
   * lookup (e.g. config {@code .jshc}, or a bare working file) keeps the permissive behavior.</p>
   *
   * @param requested The extension-qualified name being looked up.
   * @param mappedPath The path the stripped base key points at.
   * @return True if the fallback may bind, false to force a miss.
   */
  private static boolean stripFallbackAllowed(String requested, String mappedPath) {
    if (requested.endsWith(".jshdz")) {
      return mappedPath.endsWith(".jshdz");   // XZ reader expects an actually-compressed file
    }
    if (requested.endsWith(".jshd")) {
      return !mappedPath.endsWith(".jshdz");  // raw reader: anything except a compressed file
    }
    return true;
  }

  /**
   * Load a file from the mapped files.
   *
   * @param name The name of the file to load from the mapping.
   * @return The input stream for the file at the mapped path.
   * @throws RuntimeException raised if file not found in mapping or error in opening stream.
   */
  private InputStream loadFromMappedFiles(String name) {
    String actualPath = resolveMappedPath(name);
    if (actualPath == null) {
      // Surface an unmapped name as a file-not-found so callers that probe multiple candidate
      // names (e.g. MultiFormatExternalGetter trying name.jshdz then name.jshd) can recognize the
      // miss and fall through, exactly as they do for the working-directory input strategy.
      throw new UncheckedIOException(
          "File not found in mapped files: " + name, new FileNotFoundException(name));
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
    } catch (IOException e) {
      throw new RuntimeException("Failed to open stream from working directory: " + name, e);
    } catch (SecurityException e) {
      throw new RuntimeException("Access denied to file: " + name, e);
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
