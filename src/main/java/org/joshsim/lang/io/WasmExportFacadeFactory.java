
/**
 * WebAssembly-specific implementation of ExportFacadeFactory.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.OutputStream;
import java.util.Optional;

/**
 * Factory implementation for creating ExportFacade instances in a WebAssembly environment.
 */
public class WasmExportFacadeFactory implements ExportFacadeFactory {

  @Override
  public ExportFacade build(ExportTarget target) {
    if (!target.getProtocol().equals("memory")) {
      throw new IllegalArgumentException("Only in-memory targets supported on WASM.");
    }

    if (!target.getHost().equals("editor")) {
      throw new IllegalArgumentException("Only editor targets supported on WASM.");
    }

    String path = target.getPath();
    return new MemoryExportFacade(, path);
  }

  @Override
  public ExportFacade build(ExportTarget target, Iterable<String> header) {
    return build(target);
  }

  @Override
  public ExportFacade build(ExportTarget target, Optional<Iterable<String>> header) {
    return build(target);
  }

  public interface WasmExportCallback {

    void onWrite(String value);
    
  }

  public class RedirectOutputStream implements OutputStream {

    private final WasmExportCallback callback;
    
    public RedirectOutputStream(WasmExportCallback callback) {
      this.callback = callback;
    }
    
  }
  
}
