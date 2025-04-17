/**
 * Logic to help construct ExportFacades.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.export;
import java.util.Optional;


/**
 * Factory responsible for creating instances of ExportFacade based on a provided target.
 */
public class ExportFacadeFactory {

  /**
   * Build an ExportFacade which is capable of writing to the given target.
   *
   * @param target Record describing where the export should be written and from which the format is
   *     inferred.
   * @return ExportFacade which, when given Entities, will write to the location described by
   *     target.
   */
  public static ExportFacade build(ExportTarget target) {
    return build(target, Optional.empty());
  }

  /**
   * Build an ExportFacade which is capable of writing to the given target with specified header.
   *
   * @param target Record describing where the export should be written and from which the format is
   *     inferred.
   * @param header The column names to use for the header.
   * @return ExportFacade which, when given Entities, will write to the location described by
   *     target.
   */
  public static ExportFacade build(ExportTarget target, Iterable<String> header) {
    return build(target, Optional.of(header));
  }

  /**
   * Build an ExportFacade which is capable of writing to the given target with specified headers.
   *
   * @param target Record describing where the export should be written and from which the format is
   *     inferred.
   * @param header The column names to use for the headers or empty optional if to be inferred.
   * @return ExportFacade which, when given Entities, will write to the location described by
   *     target.
   */
  public static ExportFacade build(ExportTarget target, Optional<Iterable<String>> header) {
    if (!target.getFileType().equals("csv")) {
      throw new IllegalArgumentException("Only CSV files are supported at this time.");
    }

    if (!target.getProtocol().isEmpty()) {
      throw new IllegalArgumentException("Only local file system is supported at this time.");
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
