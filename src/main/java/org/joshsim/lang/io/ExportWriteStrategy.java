/**
 * Structures to describe strategies for writing exports to an output stream.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.IOException;
import java.io.OutputStream;


/**
 * Strategy to write a serialized form to a persistence mechanism.
 *
 * @param <T> The type of expected serialized form to be written.
 */
public interface ExportWriteStrategy<T> {

  /**
   * Write a record to the output stream.
   *
   * @param record The record be written to the given output stream.
   * @param outputStream The stream to which the record as serialized should be written.
   */
  void write(T record, OutputStream outputStream) throws IOException;

  /**
   * Recommend flushing.
   *
   * <p>Recommend that the write strategy flushes any waiting data like when a batch of data is
   * completed and there may be a delay before further records. This is optional and will be
   * interpreted by different strategies</p>
   */
  void flush();

  /**
   * Finish all operations on this export.
   */
  void close();

}
