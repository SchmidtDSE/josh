/**
 * Structures to provide access to platform-specific input / output operations.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;


/**
 * Interface for a strategy providing platform-specific input / output operations.
 */
public interface InputOutputLayer {

  /**
   * Get the factory providing access to export functionality on the current platform.
   *
   * @return Factory which provides access to export options available on this platform.
   */
  ExportFacadeFactory getExportFacadeFactory();

  /**
   * Get the factory providing access to debug output functionality on the current platform.
   *
   * @return Factory which provides access to debug output writers available on this platform.
   */
  OutputWriterFactory getOutputWriterFactory();

  /**
   * Get the strategy for requesting external inputs available in this layer.
   *
   * @return Strategy for data inputs available in the current environment.
   */
  InputGetterStrategy getInputStrategy();

}
