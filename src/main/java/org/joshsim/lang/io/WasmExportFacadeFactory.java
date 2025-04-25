
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
    private final StringBuilder buffer;
    
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
