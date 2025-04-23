/**
 * Structures to provide access to input / output operations when running in WASM.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;


/**
 * Interface for a strategy providing WebAssembly-specific input / output operations.
 */
public class WasmInputOutputLayer implements InputOutputLayer {

  @Override
  public ExportFacadeFactory getExportFacadeFactory() {
    return new WasmExportFacadeFactory();
  }

}
