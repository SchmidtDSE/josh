/**
 * Structures to simplify writing entities to CSV.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.lang.io.*;


/**
 * Strategy implementing ExportFacade which writes entities to memory using an internal text format.
 */
public class MemoryExportFacade implements ExportFacade {

  private final OutputStream outputStream;
  private final MapExportSerializeStrategy serializeStrategy;
  private final StringMapWriteStrategy writeStrategy;

  /**
   * Constructs a CsvExportFacade object with the specified export target / output stream strategy.
   *
   * @param outputStrategy The strategy to provide an output stream for writing the exported data.
   * @param target The name of this export like "patches" as it should be tagged in memory.
   */
  public MemoryExportFacade(OutputStreamStrategy outputStrategy, String target) {
    serializeStrategy = new MapSerializeStrategy();
    writeStrategy = new MemoryWriteStrategy(target);

    try {
      outputStream = outputStrategy.open();
    } catch (IOException e) {
      throw new RuntimeException("Error opening output stream", e);
    }
  }

  @Override
  public void start() {}

  @Override
  public void join() {}

  @Override
  public void write(Entity entity, long step) {
    try {
      Map<String, String> serialized = serializeStrategy.getRecord(entity);
      serialized.put("step", Long.toString(step));
      writeStrategy.write(serialized, outputStream);
    } catch (IOException e) {
      throw new RuntimeException("Error writing to output stream", e);
    }
  }
}
