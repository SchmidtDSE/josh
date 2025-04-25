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

  private final WasmExportCallback callback;

  /**
   * Create a new I/O layer using the given callback for when export data are generated.
   */
  public WasmInputOutputLayer(WasmExportCallback callback) {
    this.callback = callback;
  }

  @Override
  public ExportFacadeFactory getExportFacadeFactory() {
    return new WasmExportFacadeFactory(callback);
  }

}
