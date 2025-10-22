
/**
 * WebAssembly-specific implementation of ExportFacadeFactory.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.joshsim.lang.io.strategy.MemoryExportFacade;

/**
 * Factory implementation for creating ExportFacade instances in a sandbox environment.
 *
 * <p>Factory implementation for creating ExportFacade instances in a sandbox environment which
 * includes WebAssembly.</p>
 */
public class SandboxExportFacadeFactory implements ExportFacadeFactory {

  private final SandboxExportCallback callback;
  private final boolean allowTemplates;

  /**
   * Constructs a new SandboxExportFacadeFactory with the specified callback.
   *
   * @param callback The WebAssembly callback interface used to handle output operations
   */
  public SandboxExportFacadeFactory(SandboxExportCallback callback) {
    this(callback, false); // Default: reject templates for WebAssembly security
  }

  /**
   * Constructs a new SandboxExportFacadeFactory with the specified callback and template control.
   *
   * @param callback The sandbox callback interface used to handle output operations
   * @param allowTemplates If true, template variables in paths are allowed (for remote workers).
   *     If false, template variables will cause an exception (for WebAssembly security).
   */
  public SandboxExportFacadeFactory(SandboxExportCallback callback, boolean allowTemplates) {
    this.callback = callback;
    this.allowTemplates = allowTemplates;
  }

  @Override
  public ExportFacade build(ExportTarget target) {
    // Only validate target protocol/host if NOT allowing templates (WebAssembly mode)
    // Remote workers stream all output via callback, so target protocol doesn't matter
    if (!allowTemplates) {
      if (!target.getProtocol().equals("memory")) {
        throw new IllegalArgumentException("Only in-memory targets supported on WASM.");
      }

      if (!target.getHost().equals("editor")) {
        throw new IllegalArgumentException("Only editor targets supported on WASM.");
      }
    }

    // For remote workers, use full URI so client can parse it back
    // For WebAssembly, use just the path for backwards compatibility
    String name = allowTemplates ? target.toUri() : target.getPath();

    return new MemoryExportFacade(() -> new RedirectOutputStream(callback), name);
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
    // Only validate templates if configured to reject them (WebAssembly mode)
    if (!allowTemplates) {
      // Detect template variables using regex
      Pattern templatePattern = Pattern.compile("\\{([^}]+)\\}");
      Matcher matcher = templatePattern.matcher(path);
      List<String> templateVars = new ArrayList<>();

      while (matcher.find()) {
        templateVars.add("{" + matcher.group(1) + "}");
      }

      if (!templateVars.isEmpty()) {
        String varsString = String.join(", ", templateVars);
        throw new RuntimeException(
            "Template strings are not supported in sandbox/editor execution. "
                + "Found template variables: " + varsString);
      }
    }

    return path;
  }

  @Override
  public int getReplicateNumber() {
    return 0;
  }

  /**
   * An OutputStream implementation that redirects output through an in-memory callback.
   *
   * <p>This class is thread-safe and can be used concurrently by multiple threads when
   * parallel patch processing is enabled.</p>
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
    public synchronized void write(int b) throws IOException {
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
    public synchronized void write(byte[] b, int off, int len) throws IOException {
      buffer.append(new String(b, off, len));
      boolean containsNewline = buffer.indexOf("\n") != -1;
      if (containsNewline) {
        flush();
      }
    }

    @Override
    public synchronized void flush() throws IOException {
      if (buffer.length() == 0) {
        return;
      }

      callback.onWrite(buffer.toString());
      buffer.setLength(0);
    }

    @Override
    public synchronized void close() throws IOException {
      flush();
    }
  }

}
