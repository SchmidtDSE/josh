
/**
 * WebAssembly-specific implementation of ExportFacadeFactory.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;
import org.joshsim.lang.io.strategy.MemoryExportFacade;

/**
 * Factory implementation for creating ExportFacade instances in a sandbox environment.
 *
 * <p>Factory implementation for creating ExportFacade instances in a sandbox environment which
 * includes WebAssembly.</p>
 */
public class SandboxExportFacadeFactory implements ExportFacadeFactory {

  private final SandboxExportCallback callback;

  /**
   * Constructs a new SandboxExportFacadeFactory with the specified callback.
   *
   * @param callback The WebAssembly callback interface used to handle output operations
   */
  public SandboxExportFacadeFactory(SandboxExportCallback callback) {
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

  @Override
  public String getPath(String path) {
    return path;
  }

  /**
   * An OutputStream implementation that redirects output through an in-memory callback.
   */
  public class RedirectOutputStream extends OutputStream {

    private final SandboxExportCallback callback;
    private final StringBuilder buffer;

    /**
     * Constructs a new RedirectOutputStream with the specified callback.
     *
     * @param callback The WebAssembly callback to use for output redirection
     */
    public RedirectOutputStream(SandboxExportCallback callback) {
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
      boolean containsNewline = buffer.indexOf("\n") != -1;
      if (containsNewline) {
        flush();
      }
    }

    @Override
    public void flush() throws IOException {
      if (buffer.length() == 0) {
        return;
      }

      callback.onWrite(buffer.toString());
      buffer.setLength(0);
    }

    @Override
    public void close() throws IOException {
      flush();
    }
  }

}
