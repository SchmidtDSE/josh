/**
 * Logic to write to local files as part of Josh export.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Strategy which opens an OutputStream to a local file.
 *
 * <p>Note: File locking has been removed to maintain WebAssembly compatibility.
 * File locking was only needed for concurrent multi-replicate writes to the same file,
 * which is not a concern in WebAssembly environments (single replicate, no file:// access).
 * For JVM deployments with multiple concurrent replicates writing to the same file,
 * use separate output files per replicate using {replicate} template variable.</p>
 */
public class LocalOutputStreamStrategy implements OutputStreamStrategy {

  private final String location;
  private final boolean appendMode;

  /**
   * Constructs a LocalOutputStreamStrategy with the specified file location.
   *
   * @param location The local file path where the OutputStream will write data.
   */
  public LocalOutputStreamStrategy(String location) {
    this(location, false);
  }

  /**
   * Constructs a LocalOutputStreamStrategy with the specified file location and append mode.
   *
   * @param location The local file path where the OutputStream will write data.
   * @param appendMode If true, data will be appended to the file; if false, the file will be
   *     overwritten.
   */
  public LocalOutputStreamStrategy(String location, boolean appendMode) {
    this.location = location;
    this.appendMode = appendMode;
  }

  @Override
  public OutputStream open() throws IOException {
    File file = new File(location);

    // Create parent directories if they don't exist
    File parentDir = file.getParentFile();
    if (parentDir != null && !parentDir.exists()) {
      if (!parentDir.mkdirs()) {
        throw new IOException("Failed to create parent directories for: " + location);
      }
    }

    // Check if file exists and has content BEFORE opening for append
    boolean fileHasContent = false;
    if (appendMode) {
      fileHasContent = file.exists() && file.length() > 0;
    }

    FileOutputStream outputStream = new FileOutputStream(location, appendMode);
    if (appendMode && fileHasContent) {
      return new SimpleAppendOutputStream(outputStream);
    }
    return outputStream;
  }

  /**
   * Simple OutputStream wrapper that tracks append state.
   *
   * <p>This class wraps a FileOutputStream and implements AppendOutputStream to indicate
   * that data is being appended to an existing non-empty file, allowing downstream writers
   * (like CsvWriteStrategy) to skip writing headers when appropriate.</p>
   *
   * <p>Note: This implementation does NOT provide file locking. For concurrent multi-replicate
   * writes, use separate files per replicate with the {replicate} template variable.</p>
   */
  private static class SimpleAppendOutputStream extends OutputStream
      implements AppendOutputStream {
    private final OutputStream delegate;

    /**
     * Constructs a SimpleAppendOutputStream wrapping the given stream.
     *
     * @param delegate The underlying output stream.
     */
    public SimpleAppendOutputStream(OutputStream delegate) {
      this.delegate = delegate;
    }

    @Override
    public boolean isAppendingToExistingFile() {
      return true; // Always true for this wrapper
    }

    @Override
    public void write(int b) throws IOException {
      delegate.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
      delegate.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      delegate.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
      delegate.flush();
    }

    @Override
    public void close() throws IOException {
      delegate.close();
    }
  }

}
