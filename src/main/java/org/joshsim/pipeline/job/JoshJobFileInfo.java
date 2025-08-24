/**
 * Immutable information about a file used in Josh job execution.
 *
 * <p>This class encapsulates both the logical name and actual path of a file,
 * enabling template processing and file metadata tracking while maintaining
 * backward compatibility with existing file access patterns.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.job;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Immutable file information for Josh job execution.
 *
 * <p>This class stores both the logical name (for template substitution) and actual path
 * of a file used in Josh simulations. The name is typically extracted from the filename
 * stem (filename without extension) and is used for template processing such as
 * {example} substitution in export paths.</p>
 */
public class JoshJobFileInfo {

  private final String name;
  private final String path;

  /**
   * Creates a new JoshJobFileInfo instance.
   *
   * @param name The logical name for template substitution (e.g., "example_1")
   * @param path The actual file path (e.g., "test_data/example_1.jshc")
   * @throws IllegalArgumentException if name or path is null or empty
   */
  public JoshJobFileInfo(String name, String path) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("File name cannot be null or empty");
    }
    if (path == null || path.trim().isEmpty()) {
      throw new IllegalArgumentException("File path cannot be null or empty");
    }
    
    this.name = name.trim();
    this.path = path.trim();
  }

  /**
   * Gets the logical name used for template substitution.
   *
   * <p>This name is used in template strings such as {example} → "example_1".</p>
   *
   * @return The logical name for template processing
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the actual file path for loading the file.
   *
   * @return The file path for loading
   */
  public String getPath() {
    return path;
  }

  /**
   * Creates a JoshJobFileInfo from a file path by extracting the filename stem.
   *
   * <p>This method extracts the filename from the path and removes the file extension
   * to create the logical name. For example:
   * "test_data/example_1.jshc" → name="example_1", path="test_data/example_1.jshc"</p>
   *
   * @param path The file path to extract name from
   * @return A new JoshJobFileInfo instance with extracted name
   * @throws IllegalArgumentException if path is null or empty
   */
  public static JoshJobFileInfo fromPath(String path) {
    if (path == null || path.trim().isEmpty()) {
      throw new IllegalArgumentException("Path cannot be null or empty");
    }
    
    String trimmedPath = path.trim();
    String extractedName = extractFilenameStem(trimmedPath);
    return new JoshJobFileInfo(extractedName, trimmedPath);
  }

  /**
   * Gets the path for backward compatibility with legacy code.
   *
   * <p>This method provides the same functionality as getPath() but with a name
   * that makes clear it's for legacy compatibility.</p>
   *
   * @return The file path
   * @deprecated Use {@link #getPath()} instead
   */
  @Deprecated
  public String getPathForLegacyAccess() {
    return path;
  }

  /**
   * Extracts the filename stem (filename without extension) from a file path.
   *
   * <p>Handles various path formats including Unix and Windows paths. Removes
   * the file extension to get a clean name suitable for template substitution.</p>
   *
   * @param filePath The file path to extract the stem from
   * @return The filename stem without extension
   */
  private static String extractFilenameStem(String filePath) {
    // Handle both Unix and Windows path separators manually
    // Using manual extraction to avoid platform-specific Path behavior
    String filename = filePath;
    
    // Extract the filename part after the last path separator
    int lastUnixSeparator = filename.lastIndexOf('/');
    int lastWindowsSeparator = filename.lastIndexOf('\\');
    int lastSeparator = Math.max(lastUnixSeparator, lastWindowsSeparator);
    
    if (lastSeparator >= 0) {
      filename = filename.substring(lastSeparator + 1);
    }
    
    // Remove the extension if present
    int lastDotIndex = filename.lastIndexOf('.');
    if (lastDotIndex > 0) {
      return filename.substring(0, lastDotIndex);
    } else {
      // No extension found, return the whole filename
      return filename;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    
    JoshJobFileInfo that = (JoshJobFileInfo) obj;
    return name.equals(that.name) && path.equals(that.path);
  }

  @Override
  public int hashCode() {
    return name.hashCode() * 31 + path.hashCode();
  }

  @Override
  public String toString() {
    return "JoshJobFileInfo{name='" + name + "', path='" + path + "'}";
  }
}