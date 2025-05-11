/**
 * Logic to help construct ExportFacades.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.util.Optional;


/**
 * Factory responsible for creating instances of ExportFacade based on a provided target.
 */
public interface ExportFacadeFactory {

  /**
   * Build an ExportFacade which is capable of writing to the given target.
   *
   * @param target Record describing where the export should be written and from which the format is
   *     inferred.
   * @return ExportFacade which, when given Entities, will write to the location described by
   *     target.
   */
  ExportFacade build(ExportTarget target);

  /**
   * Build an ExportFacade which is capable of writing to the given target with specified header.
   *
   * @param target Record describing where the export should be written and from which the format is
   *     inferred.
   * @param header The column names to use for the header.
   * @return ExportFacade which, when given Entities, will write to the location described by
   *     target.
   */
  ExportFacade build(ExportTarget target, Iterable<String> header);

  /**
   * Build an ExportFacade which is capable of writing to the given target with specified headers.
   *
   * @param target Record describing where the export should be written and from which the format is
   *     inferred.
   * @param header The column names to use for the headers or empty optional if to be inferred.
   * @return ExportFacade which, when given Entities, will write to the location described by
   *     target.
   */
  ExportFacade build(ExportTarget target, Optional<Iterable<String>> header);

  /**
   * Replace template strings in the user provided template.
   *
   * @param template The template string provided by the user which may contain template tags.
   * @returns The path with supported template tags replaced.
   */
  String getPath(String template);
}
