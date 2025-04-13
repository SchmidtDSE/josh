/**
 * Logic to help construct ExportFacades.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.export;


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
    if (!target.getFileType().equals("csv")) {
      throw new IllegalArgumentException("Only CSV files are supported at this time.");
    }

    if (!target.getProtocol().equals("local")) {
      throw new IllegalArgumentException("Only local file system is supported at this time.");
    }

    String path = target.getPath();
    OutputStreamStrategy outputStreamStrategy = new LocalOutputStreamStrategy(path);
    return new CsvExportFacade(outputStreamStrategy);
  }

}
