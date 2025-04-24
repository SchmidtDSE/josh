
/**
 * WebAssembly-specific implementation of ExportFacadeFactory.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.util.Optional;

/**
 * Factory implementation for creating ExportFacade instances in a WebAssembly environment.
 */
public class WasmExportFacadeFactory implements ExportFacadeFactory {

  @Override
  public ExportFacade build(ExportTarget target) {
    throw new UnsupportedOperationException("WASM export facade not yet implemented");
  }

  @Override
  public ExportFacade build(ExportTarget target, Iterable<String> header) {
    throw new UnsupportedOperationException("WASM export facade not yet implemented");
  }

  @Override
  public ExportFacade build(ExportTarget target, Optional<Iterable<String>> header) {
    throw new UnsupportedOperationException("WASM export facade not yet implemented");
  }
}
