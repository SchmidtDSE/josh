/**
 * Logic to write to local files as part of Josh export.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * Strategy which opens an OutputStream to a local file.
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
    // Check if file exists and has content BEFORE opening for append
    boolean fileHasContent = false;
    if (appendMode) {
      File file = new File(location);
      fileHasContent = file.exists() && file.length() > 0;
    }

    FileOutputStream outputStream = new FileOutputStream(location, appendMode);
    if (appendMode) {
      FileChannel channel = outputStream.getChannel();
      FileLock lock = channel.lock(); // Exclusive lock, blocks until acquired
      return new LockingOutputStream(outputStream, lock, fileHasContent);
    }
    return outputStream;
  }

  /**
   * OutputStream wrapper that releases a file lock on close and tracks append state.
   *
   * <p>This class wraps a FileOutputStream with a FileLock, ensuring that the lock is properly
   * released when the stream is closed. This is essential for thread-safe append operations where
   * multiple replicates may attempt to write to the same file concurrently.</p>
   *
   * <p>Additionally, this class implements AppendOutputStream to indicate whether data is being
   * appended to an existing non-empty file, allowing downstream writers (like CsvWriteStrategy)
   * to skip writing headers when appropriate.</p>
   */
  private static class LockingOutputStream extends FilterOutputStream
      implements AppendOutputStream {
    private final FileLock lock;
    private final boolean appendingToExistingFile;

    /**
     * Constructs a LockingOutputStream with the specified stream, lock, and append state.
     *
     * @param out The underlying output stream.
     * @param lock The file lock to be released on close.
     * @param appendingToExistingFile True if appending to a non-empty existing file.
     */
    public LockingOutputStream(OutputStream out, FileLock lock, boolean appendingToExistingFile) {
      super(out);
      this.lock = lock;
      this.appendingToExistingFile = appendingToExistingFile;
    }

    @Override
    public boolean isAppendingToExistingFile() {
      return appendingToExistingFile;
    }

    @Override
    public void close() throws IOException {
      try {
        lock.release();
      } finally {
        super.close();
      }
    }
  }

}
