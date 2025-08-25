/**
 * Generates output streams for replicate-based file path generation.
 *
 * <p>This class implements the ParameterizedOutputStreamGenerator pattern for
 * replicate-based file naming. It substitutes {replicate} templates in file paths
 * to create separate files for each replicate number.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Output stream generator that creates separate files per replicate number.
 *
 * <p>This generator follows the established pattern from GeotiffExportFacade
 * but is specialized for replicate-based file generation. It processes template
 * paths containing {replicate} placeholders and generates appropriate file streams.</p>
 *
 * <p>Example usage:
 * <pre>
 * String template = "file:///tmp/simulation_{replicate}.csv";
 * ReplicateOutputStreamGenerator generator = new ReplicateOutputStreamGenerator(template);
 * 
 * StreamReference ref = new StreamReference(1);
 * OutputStream stream = generator.getStream(ref);  // Creates: simulation_1.csv
 * </pre>
 */
public class ReplicateOutputStreamGenerator implements
    org.joshsim.lang.io.strategy.ParameterizedCsvExportFacade.ParameterizedOutputStreamGenerator,
    org.joshsim.lang.io.strategy.ParameterizedNetcdfExportFacade
        .ParameterizedOutputStreamGenerator {

  private final String pathTemplate;

  /**
   * Creates a new ReplicateOutputStreamGenerator with the specified path template.
   *
   * <p>The template should contain a {replicate} placeholder that will be replaced
   * with the actual replicate number when generating streams.</p>
   *
   * @param pathTemplate The file path template containing {replicate} placeholder
   * @throws IllegalArgumentException if pathTemplate is null or empty
   */
  public ReplicateOutputStreamGenerator(String pathTemplate) {
    if (pathTemplate == null || pathTemplate.trim().isEmpty()) {
      throw new IllegalArgumentException("Path template cannot be null or empty");
    }
    this.pathTemplate = pathTemplate;
  }

  /**
   * Generates an output stream for CSV export based on the replicate number.
   *
   * @param reference The StreamReference containing the replicate number
   * @return New FileOutputStream for the replicate-specific file path
   * @throws RuntimeException if the file cannot be created
   */
  @Override
  public OutputStream getStream(
      org.joshsim.lang.io.strategy.ParameterizedCsvExportFacade.StreamReference reference) {
    String path = pathTemplate.replaceAll("\\{replicate\\}", 
        Integer.toString(reference.getReplicate()));
    return createFileOutputStream(path);
  }

  /**
   * Generates an output stream for NetCDF export based on the replicate number.
   *
   * @param reference The StreamReference containing the replicate number
   * @return New FileOutputStream for the replicate-specific file path
   * @throws RuntimeException if the file cannot be created
   */
  @Override
  public OutputStream getStream(
      org.joshsim.lang.io.strategy.ParameterizedNetcdfExportFacade.StreamReference reference) {
    String path = pathTemplate.replaceAll("\\{replicate\\}", 
        Integer.toString(reference.getReplicate()));
    return createFileOutputStream(path);
  }

  /**
   * Creates a FileOutputStream for the specified path with error handling.
   *
   * @param path The file path to create stream for
   * @return New FileOutputStream for the specified path
   * @throws RuntimeException if the file cannot be created
   */
  private OutputStream createFileOutputStream(String path) {
    try {
      return new FileOutputStream(path);
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Could not create file for path: " + path, e);
    }
  }

  /**
   * Gets the path template used by this generator.
   *
   * @return The path template string containing {replicate} placeholder
   */
  public String getPathTemplate() {
    return pathTemplate;
  }

  /**
   * Tests if the template contains a replicate placeholder.
   *
   * @return True if the template contains {replicate}, false otherwise
   */
  public boolean hasReplicateTemplate() {
    return pathTemplate.contains("{replicate}");
  }

  @Override
  public String toString() {
    return "ReplicateOutputStreamGenerator{pathTemplate='" + pathTemplate + "'}";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ReplicateOutputStreamGenerator that = (ReplicateOutputStreamGenerator) obj;
    return pathTemplate.equals(that.pathTemplate);
  }

  @Override
  public int hashCode() {
    return pathTemplate.hashCode();
  }
}