/**
 * Structures to describe strategies for writing exports to an output stream.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.export;

import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Stream;


/**
 * Strategy to write a serialized form to a persistence mechanism.
 *
 * @param <T> The type of expected serialized form to be written.
 */
public interface ExportWriteStrategy<T> {

  /**
   * Write the given set of records to the given output stream.
   *
   * @param records The collection of serialized records to be written to the given output stream.
   * @param outputStream The stream to which the records as serialized should be written.
   */
  void write(Stream<T> records, OutputStream outputStream) throws IOException;

}
