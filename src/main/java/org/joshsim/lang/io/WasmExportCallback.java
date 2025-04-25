
/**
 * Interface defining the callback mechanism for WebAssembly export operations.
 * This interface provides methods to handle output redirection in a WebAssembly context.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

/**
 * Interface defining the callback mechanism for WebAssembly export operations.
 * This interface provides methods to handle output redirection in a WebAssembly context.
 */
public interface WasmExportCallback {
    
    /**
     * Called when data needs to be written through the WebAssembly interface.
     *
     * @param value The string value to be written through the callback
     */
    void onWrite(String value);
    
}
