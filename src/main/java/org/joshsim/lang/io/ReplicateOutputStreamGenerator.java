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
 * ReplicateOutputStreamGenerator generator = new ReplicateOutputStreamGenerator(template, ...);
 *
 * StreamReference ref = new StreamReference(1);
 * OutputStream stream = generator.getStream(ref);  // Creates: simulation_1.csv
 * </pre>
 */
public class ReplicateOutputStreamGenerator implements
    org.joshsim.lang.io.strategy.ParameterizedCsvExportFacade.ParameterizedOutputStreamGenerator,
    org.joshsim.lang.io.strategy.ParameterizedNetcdfExportFacade
        .ParameterizedOutputStreamGenerator {

  private final ExportTarget targetTemplate;
  private final JvmExportFacadeFactory factory;

  /**
   * Creates a new ReplicateOutputStreamGenerator with the specified target template.
   *
   * <p>The template's path should contain a {replicate} placeholder that will be replaced
   * with the actual replicate number when generating streams.</p>
   *
   * @param targetTemplate The export target template containing {replicate} placeholder in path
   * @param factory Factory to create OutputStreamStrategy for a given target
   * @throws IllegalArgumentException if targetTemplate is null or has null/empty path
   */
  public ReplicateOutputStreamGenerator(ExportTarget targetTemplate,
                                        JvmExportFacadeFactory factory) {
    if (targetTemplate == null) {
      throw new IllegalArgumentException("Target template cannot be null");
    }
    if (targetTemplate.getPath() == null || targetTemplate.getPath().trim().isEmpty()) {
      throw new IllegalArgumentException("Target template path cannot be null or empty");
    }
    this.targetTemplate = targetTemplate;
    this.factory = factory;
  }

  /**
   * Generates an output stream for CSV export based on the replicate number.
   *
   * @param reference The StreamReference containing the replicate number
   * @return New OutputStream for the replicate-specific file path
   * @throws RuntimeException if the stream cannot be created
   */
  @Override
  public OutputStream getStream(
      org.joshsim.lang.io.strategy.ParameterizedCsvExportFacade.StreamReference reference) {
    ExportTarget target = substituteReplicate(reference.getReplicate());
    return createOutputStream(target);
  }

  /**
   * Generates an output stream for NetCDF export based on the replicate number.
   *
   * @param reference The StreamReference containing the replicate number
   * @return New OutputStream for the replicate-specific file path
   * @throws RuntimeException if the stream cannot be created
   */
  @Override
  public OutputStream getStream(
      org.joshsim.lang.io.strategy.ParameterizedNetcdfExportFacade.StreamReference reference) {
    ExportTarget target = substituteReplicate(reference.getReplicate());
    return createOutputStream(target);
  }

  /**
   * Substitutes the {replicate} placeholder in the target template.
   *
   * @param replicate The replicate number to substitute
   * @return New ExportTarget with replicate substituted in path
   */
  private ExportTarget substituteReplicate(int replicate) {
    String originalPath = targetTemplate.getPath();
    String substitutedPath = originalPath.replaceAll(
        "\\{replicate\\}",
        Integer.toString(replicate)
    );

    return new ExportTarget(
        targetTemplate.getProtocol(),
        targetTemplate.getHost(),
        substitutedPath
    );
  }

  /**
   * Creates an OutputStream for the specified target using the factory.
   *
   * @param target The export target to create stream for
   * @return New OutputStream for the specified target
   * @throws RuntimeException if the stream cannot be created
   */
  private OutputStream createOutputStream(ExportTarget target) {
    try {
      OutputStreamStrategy strategy = factory.createOutputStreamStrategy(target);
      return strategy.open();
    } catch (Exception e) {
      throw new RuntimeException(
          "Could not create output stream for target: "
              + target.getProtocol() + "://" + target.getHost() + target.getPath(),
          e
      );
    }
  }

  /**
   * Gets the target template used by this generator.
   *
   * @return The ExportTarget template containing {replicate} placeholder in path
   */
  public ExportTarget getTargetTemplate() {
    return targetTemplate;
  }

  /**
   * Gets the path template for backward compatibility.
   *
   * @return The path from the target template
   * @deprecated Use getTargetTemplate() instead
   */
  @Deprecated
  public String getPathTemplate() {
    return reconstructPath(targetTemplate);
  }

  /**
   * Tests if the template contains a replicate placeholder.
   *
   * @return True if the template contains {replicate}, false otherwise
   */
  public boolean hasReplicateTemplate() {
    return targetTemplate.getPath().contains("{replicate}");
  }

  @Override
  public String toString() {
    return "ReplicateOutputStreamGenerator{targetTemplate='"
        + reconstructPath(targetTemplate) + "'}";
  }

  /**
   * Reconstructs full path string from ExportTarget for display purposes.
   *
   * @param target The export target
   * @return Full path string with protocol
   */
  private String reconstructPath(ExportTarget target) {
    String protocol = target.getProtocol();
    if (protocol.isEmpty()) {
      return target.getPath();
    } else if (protocol.equals("file")) {
      return "file://" + target.getHost() + target.getPath();
    } else {
      return protocol + "://" + target.getHost() + target.getPath();
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
    ReplicateOutputStreamGenerator that = (ReplicateOutputStreamGenerator) obj;
    return targetTemplate.getProtocol().equals(that.targetTemplate.getProtocol())
        && targetTemplate.getHost().equals(that.targetTemplate.getHost())
        && targetTemplate.getPath().equals(that.targetTemplate.getPath());
  }

  @Override
  public int hashCode() {
    int result = targetTemplate.getProtocol().hashCode();
    result = 31 * result + targetTemplate.getHost().hashCode();
    result = 31 * result + targetTemplate.getPath().hashCode();
    return result;
  }
}
