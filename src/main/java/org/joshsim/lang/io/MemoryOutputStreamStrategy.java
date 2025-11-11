/**
 * Output stream strategy for memory:// protocol in WebAssembly environments.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStreamStrategy implementation for memory:// protocol.
 *
 * <p>This strategy creates an OutputStream that writes to an in-memory callback,
 * typically used in WebAssembly environments where debug output needs to be captured
 * and sent to the browser for display.</p>
 *
 * <p>This strategy is designed to be WebAssembly-compatible and does not use any
 * JVM-specific threading or I/O classes.</p>
 */
public class MemoryOutputStreamStrategy implements OutputStreamStrategy {

  private final SandboxExportCallback callback;

  /**
   * Creates a new MemoryOutputStreamStrategy.
   *
   * @param callback The callback to receive output data
   */
  public MemoryOutputStreamStrategy(SandboxExportCallback callback) {
    this.callback = callback;
  }

  @Override
  public OutputStream open() throws IOException {
    // Return a RedirectOutputStream that writes to the callback
    // This is the same pattern used by SandboxExportFacadeFactory
    return new RedirectOutputStream(callback);
  }

  /**
   * OutputStream that redirects writes to a SandboxExportCallback.
   *
   * <p>This implementation is compatible with WebAssembly and does not use
   * any JVM-specific threading or I/O classes.</p>
   */
  private static class RedirectOutputStream extends OutputStream {

    private final SandboxExportCallback callback;
    private final StringBuilder buffer;

    public RedirectOutputStream(SandboxExportCallback callback) {
      this.callback = callback;
      this.buffer = new StringBuilder();
    }

    @Override
    public void write(int b) throws IOException {
      char c = (char) b;
      buffer.append(c);

      // Flush on newline for immediate output
      if (c == '\n') {
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
      flush(); // Flush any remaining data
    }
  }
}
