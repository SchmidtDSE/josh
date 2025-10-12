/**
 * Marker interface for OutputStreams that are appending to existing files.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

/**
 * Interface implemented by OutputStream instances that are appending to existing files.
 *
 * <p>This interface allows downstream writers (such as CsvWriteStrategy) to determine
 * whether they should skip writing headers when the stream is appending to a file that
 * already has content.</p>
 */
public interface AppendOutputStream {

  /**
   * Returns true if this stream is appending to an existing non-empty file.
   *
   * @return true if appending to existing file, false otherwise
   */
  boolean isAppendingToExistingFile();

}
