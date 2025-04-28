
/**
 * JVM-specific implementation of ExportFacadeFactory.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.util.Optional;

/**
 * Factory implementation for creating ExportFacade instances in a JVM environment.
 */
public class JvmExportFacadeFactory implements ExportFacadeFactory {

  @Override
  public ExportFacade build(ExportTarget target) {
    return build(target, Optional.empty());
  }

  @Override
  public ExportFacade build(ExportTarget target, Iterable<String> header) {
    return build(target, Optional.of(header));
  }

  @Override
  public ExportFacade build(ExportTarget target, Optional<Iterable<String>> header) {
    return switch (target.getFileType()) {
      case "csv" -> buildForCsv(target, header);
      default -> throw new IllegalArgumentException("Not supported: " + target.getFileType());
    };
  }

  /**
   * Build an ExportFacade that writes to a CSV file.
   *
   * @param target Record describing where the export should be written and format details.
   *     The protocol must be empty, as only the local file system is supported for CSV exports.
   * @param header An optional list of column headers to include in the CSV. If empty, headers are
   *     not included.
   * @return CsvExportFacade configured to write to the file path specified in the target.
   * @throws IllegalArgumentException if the target's protocol is not empty or the target is
   *     invalid.
   */
  private static ExportFacade buildForCsv(ExportTarget target, Optional<Iterable<String>> header) {
    if (!target.getProtocol().isEmpty()) {
      String message = "Only local file system is supported for CSV at this time.";
      throw new IllegalArgumentException(message);
    }

    String path = target.getPath();
    OutputStreamStrategy outputStreamStrategy = new LocalOutputStreamStrategy(path);

    if (header.isPresent()) {
      return new CsvExportFacade(outputStreamStrategy, header.get());
    } else {
      return new CsvExportFacade(outputStreamStrategy);
    }
  }
}
