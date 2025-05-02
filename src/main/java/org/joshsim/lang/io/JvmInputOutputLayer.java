/**
 * Structures to provide access to input / output operations when running in the JVM.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;


/**
 * Interface for a strategy providing JVM-specific input / output operations.
 */
public class JvmInputOutputLayer implements InputOutputLayer {

  @Override
  public ExportFacadeFactory getExportFacadeFactory() {
    return new JvmExportFacadeFactory();
  }

}
