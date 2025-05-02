/**
 * Interface defining the callback mechanism for export operations from sandboxes like WASM.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

/**
 * Interface defining the callback mechanism for export from security controlled sandbox like WASM.
 */
public interface SandboxExportCallback {

  /**
   * Called when data needs to be written through the WebAssembly interface.
   *
   * @param value The string value to be written through the callback
   */
  void onWrite(String value);

}
