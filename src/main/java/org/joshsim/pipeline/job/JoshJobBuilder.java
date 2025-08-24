/**
 * Builder for constructing JoshJob instances.
 *
 * <p>This class follows the builder pattern to construct JoshJob objects
 * with a fluent interface. It provides validation during construction and
 * ensures proper initialization of the immutable job object.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.job;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder for creating JoshJob instances with a fluent interface.
 *
 * <p>This builder enables step-by-step construction of job definitions with
 * validation and method chaining. All setter methods return the builder
 * instance to support fluent interface patterns.</p>
 */
public class JoshJobBuilder {

  private final Map<String, JoshJobFileInfo> fileInfos = new HashMap<>();
  private int replicates = 1;

  /**
   * Sets file information for this job using a JoshJobFileInfo object.
   *
   * @param logicalName The logical name of the file (e.g., "example.jshc")
   * @param fileInfo The JoshJobFileInfo containing name and path information
   * @return This builder instance for method chaining
   * @throws IllegalArgumentException if logicalName or fileInfo is null or empty
   */
  public JoshJobBuilder setFileInfo(String logicalName, JoshJobFileInfo fileInfo) {
    if (logicalName == null || logicalName.trim().isEmpty()) {
      throw new IllegalArgumentException("Logical file name cannot be null or empty");
    }
    if (fileInfo == null) {
      throw new IllegalArgumentException("File info cannot be null");
    }
    
    fileInfos.put(logicalName.trim(), fileInfo);
    return this;
  }

  /**
   * Sets file information for this job using name and path components.
   *
   * @param logicalName The logical name of the file (e.g., "example.jshc")
   * @param templateName The name for template substitution (e.g., "example_1")
   * @param path The path to the file (e.g., "test_data/example_1.jshc")
   * @return This builder instance for method chaining
   * @throws IllegalArgumentException if any parameter is null or empty
   */
  public JoshJobBuilder setFileInfo(String logicalName, String templateName, String path) {
    JoshJobFileInfo fileInfo = new JoshJobFileInfo(templateName, path);
    return setFileInfo(logicalName, fileInfo);
  }

  /**
   * Sets a file mapping for this job using path extraction for name.
   *
   * @param logicalName The logical name of the file (e.g., "example.jshc")
   * @param path The path to the file (e.g., "test_data/example_1.jshc")
   * @return This builder instance for method chaining
   * @throws IllegalArgumentException if name or path is null or empty
   * @deprecated Use {@link #setFileInfo(String, String, String)} or 
   *             {@link #setFileInfo(String, JoshJobFileInfo)} instead
   */
  @Deprecated
  public JoshJobBuilder setFilePath(String logicalName, String path) {
    JoshJobFileInfo fileInfo = JoshJobFileInfo.fromPath(path);
    return setFileInfo(logicalName, fileInfo);
  }

  /**
   * Sets the number of replicates for this job.
   *
   * @param replicates The number of replicates to run
   * @return This builder instance for method chaining
   * @throws IllegalArgumentException if replicates is less than or equal to 0
   */
  public JoshJobBuilder setReplicates(int replicates) {
    if (replicates <= 0) {
      throw new IllegalArgumentException("Number of replicates must be greater than 0, got: " 
          + replicates);
    }
    
    this.replicates = replicates;
    return this;
  }

  /**
   * Builds and returns the immutable JoshJob instance.
   *
   * @return A new JoshJob instance with the configured parameters
   * @throws IllegalArgumentException if the job configuration is invalid
   */
  public JoshJob build() {
    // Additional validation could be added here if needed
    return new JoshJob(fileInfos, replicates);
  }
}