/**
 * Logic to write CSV files.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.joshsim.compat.CompatibilityLayer;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.CompatibleStringJoiner;

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
    CompatibilityLayer compatibilityLayer = CompatibilityLayerKeeper.get();
    CompatibleStringJoiner joiner = compatibilityLayer.createStringJoiner("\t");
    
    for (String key : record.keySet()) {
      String value = record.get(key).toString();
      String valueSafe = value.replaceAll("\t", "    ").replaceAll("\n", "    ");
      String assignment = String.format("%s=%s", key, valueSafe);
      joiner.add(assignment);
    }

    String completeStr = String.format("%s:%s", name, joiner.toString());
    output.write(completeStr.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public void flush() {}

}
