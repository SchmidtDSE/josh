
/**
 * WebAssembly-specific implementation of ExportFacadeFactory.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

/**
 * Factory implementation for creating ExportFacade instances in a WebAssembly environment.
 */
/**
 * Factory implementation for creating ExportFacade instances in a WebAssembly environment.
 * This class provides functionality to build export facades that can redirect output
 * through WebAssembly callbacks.
 */
public class WasmExportFacadeFactory implements ExportFacadeFactory {

  private final WasmExportCallback callback;
  
  /**
   * Constructs a new WasmExportFacadeFactory with the specified callback.
   *
   * @param callback The WebAssembly callback interface used to handle output operations
   */
  public WasmExportFacadeFactory(WasmExportCallback callback) {
    this.callback = callback;
  }
  
  @Override
  public ExportFacade build(ExportTarget target) {
    if (!target.getProtocol().equals("memory")) {
      throw new IllegalArgumentException("Only in-memory targets supported on WASM.");
    }

    if (!target.getHost().equals("editor")) {
      throw new IllegalArgumentException("Only editor targets supported on WASM.");
    }

    String path = target.getPath();
    return new MemoryExportFacade(() -> new RedirectOutputStream(callback), path);
  }

  @Override
  public ExportFacade build(ExportTarget target, Iterable<String> header) {
    return build(target);
  }

  @Override
  public ExportFacade build(ExportTarget target, Optional<Iterable<String>> header) {
    return build(target);
  }

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

  /**
   * An OutputStream implementation that redirects output through a WebAssembly callback.
   * This class buffers output and forwards it to the callback when appropriate,
   * such as when encountering newlines or when explicitly flushed.
   */
  public class RedirectOutputStream extends OutputStream {

    private final WasmExportCallback callback;
    private final StringBuilder buffer;
    
    /**
     * Constructs a new RedirectOutputStream with the specified callback.
     *
     * @param callback The WebAssembly callback to use for output redirection
     */
    public RedirectOutputStream(WasmExportCallback callback) {
      this.callback = callback;
      this.buffer = new StringBuilder();
    }

    @Override
    public void write(int b) throws IOException {
      buffer.append((char) b);
      if (b == '\n') {
        flush();
      }
    }

    @Override
    public void write(byte[] b) throws IOException {
      write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      buffer.append(new String(b, off, len));
      if (buffer.indexOf("\n") != -1) {
        flush();
      }
    }

    @Override
    public void flush() throws IOException {
      if (buffer.length() > 0) {
        callback.onWrite(buffer.toString());
        buffer.setLength(0);
      }
    }

    @Override
    public void close() throws IOException {
      flush();
    }
  }
  
}
