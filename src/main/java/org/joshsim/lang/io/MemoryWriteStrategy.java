/**
 * Logic to write CSV files.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Implementation of the ExportWriteStrategy interface for writing writing to a callback.
 *
 * <p>Class responsible for serializing a stream of records, where each record is represented as a
 * Map (String, String), into an internal string format which can be passed between languages.</p>
 */
public class MemoryWriteStrategy implements ExportWriteStrategy<Map<String, String>> {

  private final String name;

  /**
   * Create a new strategy which writes to a target using the in-memory format.
   *
   * @param name The name of the target for which records are being written.
   */
  public MemoryWriteStrategy(String name) {
    this.name = name;
  }

  @Override
  public void write(Map<String, String> record, OutputStream output) throws IOException {
    String completeStr = MapToMemoryStringConverter.convert(name, record);
    output.write(completeStr.getBytes(StandardCharsets.UTF_8));
    output.flush();
  }

  @Override
  public void flush() {}

  @Override
  public void close() {}

}
