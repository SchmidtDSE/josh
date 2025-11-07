/**
 * Logic to help construct DebugFacades.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.debug;

import org.joshsim.lang.io.ExportTarget;

/**
 * Factory responsible for creating instances of DebugFacade based on a provided target.
 *
 * <p>Similar to ExportFacadeFactory but simpler since debug output doesn't require
 * serialization strategies, headers, or format-specific handling.</p>
 */
public interface DebugFacadeFactory {

  /**
   * Build a DebugFacade which is capable of writing to the given target.
   *
   * @param target Record describing where the debug output should be written.
   * @return DebugFacade which, when given messages, will write to the location described by target.
   */
  DebugFacade build(ExportTarget target);

  /**
   * Replace template strings in the user provided template.
   *
   * @param template The template string provided by the user which may contain template tags.
   * @return The path with supported template tags replaced.
   */
  String getPath(String template);

  /**
   * Get the replicate number for this debug factory.
   *
   * <p>Returns the replicate number that should be used when writing debug messages.</p>
   *
   * @return The replicate number to use for debug output.
   */
  int getReplicateNumber();
}
