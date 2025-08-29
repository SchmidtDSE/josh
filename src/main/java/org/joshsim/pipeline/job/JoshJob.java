/**
 * Immutable representation of a Josh simulation job.
 *
 * <p>This class encapsulates the parameters for a single Josh simulation job,
 * including file mappings and replicate count. It follows immutable object principles
 * with final fields and defensive copying for collections.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.job;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Immutable job definition for Josh simulations.
 *
 * <p>This class represents a single simulation job with its associated file mappings
 * and replicate configuration. All fields are final and collections are defensively
 * copied to ensure immutability.</p>
 */
public class JoshJob {

  private final Map<String, JoshJobFileInfo> fileInfos;
  private final int replicates;
  private final Map<String, String> customParameters;

  /**
   * Creates a new JoshJob instance.
   *
   * <p>This constructor is package-private and should only be called from
   * JoshJobBuilder to ensure proper validation and construction.</p>
   *
   * @param fileInfos Map of logical file names to their JoshJobFileInfo objects
   * @param replicates Number of replicates to run
   * @throws IllegalArgumentException if replicates is less than or equal to 0
   */
  JoshJob(Map<String, JoshJobFileInfo> fileInfos, int replicates) {
    this(fileInfos, replicates, new HashMap<>());
  }

  /**
   * Creates a new JoshJob instance with custom parameters.
   *
   * <p>This constructor is package-private and should only be called from
   * JoshJobBuilder to ensure proper validation and construction.</p>
   *
   * @param fileInfos Map of logical file names to their JoshJobFileInfo objects
   * @param replicates Number of replicates to run
   * @param customParameters Map of custom parameter names to values for template processing
   * @throws IllegalArgumentException if replicates is less than or equal to 0
   */
  JoshJob(Map<String, JoshJobFileInfo> fileInfos, int replicates,
      Map<String, String> customParameters) {
    if (replicates <= 0) {
      throw new IllegalArgumentException("Number of replicates must be greater than 0, got: "
          + replicates);
    }

    // Defensive copy to ensure immutability
    this.fileInfos = new HashMap<>(fileInfos);
    this.replicates = replicates;
    this.customParameters = new HashMap<>(customParameters);
  }

  /**
   * Gets the file info for a given logical file name.
   *
   * @param logicalName The logical name of the file to look up
   * @return The JoshJobFileInfo for the given name, or null if not found
   */
  public JoshJobFileInfo getFileInfo(String logicalName) {
    return fileInfos.get(logicalName);
  }

  /**
   * Gets the file path for a given file name.
   *
   * @param name The name of the file to look up
   * @return The file path for the given name, or null if not found
   * @deprecated Use {@link #getFileInfo(String)} instead
   */
  @Deprecated
  public String getFilePath(String name) {
    JoshJobFileInfo fileInfo = fileInfos.get(name);
    return fileInfo != null ? fileInfo.getPath() : null;
  }

  /**
   * Gets the number of replicates for this job.
   *
   * @return The number of replicates to run
   */
  public int getReplicates() {
    return replicates;
  }

  /**
   * Gets all file names in this job.
   *
   * @return A set of all file names (defensive copy)
   */
  public Set<String> getFileNames() {
    return Set.copyOf(fileInfos.keySet());
  }

  /**
   * Gets all file information mappings.
   *
   * @return A defensive copy of the file info mappings
   */
  public Map<String, JoshJobFileInfo> getFileInfos() {
    return new HashMap<>(fileInfos);
  }

  /**
   * Gets all custom parameter mappings.
   *
   * @return A defensive copy of the custom parameter mappings
   */
  public Map<String, String> getCustomParameters() {
    return new HashMap<>(customParameters);
  }

  /**
   * Gets a copy of all file path mappings.
   *
   * @return A defensive copy of the file path mappings
   * @deprecated Use {@link #getFileInfos()} instead
   */
  @Deprecated
  public Map<String, String> getFilePaths() {
    Map<String, String> pathsMap = new HashMap<>();
    for (Map.Entry<String, JoshJobFileInfo> entry : fileInfos.entrySet()) {
      pathsMap.put(entry.getKey(), entry.getValue().getPath());
    }
    return pathsMap;
  }
}
