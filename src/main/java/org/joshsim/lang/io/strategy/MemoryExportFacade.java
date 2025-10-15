/**
 * Structures to simplify writing entities to CSV.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.lang.io.ExportFacade;
import org.joshsim.lang.io.MapSerializeStrategy;
import org.joshsim.lang.io.OutputStreamStrategy;
import org.joshsim.lang.io.strategy.MapExportSerializeStrategy;
import org.joshsim.wire.NamedMap;


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
  public void write(Entity entity, long step, int replicateNumber) {
    try {
      Map<String, String> original = serializeStrategy.getRecord(entity);

      // Create a LinkedHashMap to preserve ordering and ensure replicate is last
      Map<String, String> serialized = new LinkedHashMap<>();

      // Add all original data first
      original.entrySet().forEach(entry -> serialized.put(entry.getKey(), entry.getValue()));

      // Add step column (before replicate)
      serialized.put("step", Long.toString(step));

      // Add replicate column as the last column
      serialized.put("replicate", Integer.toString(replicateNumber));
      writeStrategy.write(serialized, outputStream);
    } catch (IOException e) {
      throw new RuntimeException("Error writing to output stream", e);
    }
  }

  @Override
  public void write(NamedMap namedMap, long step, int replicateNumber) {
    try {
      // Create a LinkedHashMap to preserve ordering and ensure replicate is last
      Map<String, String> serialized = new LinkedHashMap<>();

      // Add all original data first
      namedMap.getTarget().entrySet().forEach(entry ->
          serialized.put(entry.getKey(), entry.getValue()));

      // Add step column (before replicate)
      serialized.put("step", Long.toString(step));

      // Add replicate column as the last column
      serialized.put("replicate", Integer.toString(replicateNumber));
      writeStrategy.write(serialized, outputStream);
    } catch (IOException e) {
      throw new RuntimeException("Error writing to output stream", e);
    }
  }

  @Override
  public void write(Entity entity, long step) {
    write(entity, step, 0);
  }

  @Override
  public void write(NamedMap namedMap, long step) {
    write(namedMap, step, 0);
  }

  @Override
  public Optional<MapExportSerializeStrategy> getSerializeStrategy() {
    return Optional.empty();
  }
}
