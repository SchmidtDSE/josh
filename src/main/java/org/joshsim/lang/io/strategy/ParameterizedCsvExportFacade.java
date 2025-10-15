/**
 * Export facade for writing multi-replicate datasets to separate CSV files.
 *
 * <p>This facade creates separate CSV files per replicate, following the same
 * pattern as GeotiffExportFacade but for tabular data. Each replicate gets its
 * own file, while still including the replicate column for consistency.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.QueueService;
import org.joshsim.compat.QueueServiceCallback;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.lang.io.ExportFacade;
import org.joshsim.lang.io.ExportTask;
import org.joshsim.wire.NamedMap;

/**
 * Export facade which creates separate CSV files per replicate.
 *
 * <p>This facade enables memory-efficient processing by writing each replicate
 * to its own CSV file, allowing completed replicates to be freed from memory.
 * Each file still contains the replicate column for consistency with consolidated
 * CSV export behavior.</p>
 *
 * <p>Example output files for 3 replicates:
 * <ul>
 *   <li>data_1.csv - Contains replicate=1 records</li>
 *   <li>data_2.csv - Contains replicate=2 records</li>
 *   <li>data_3.csv - Contains replicate=3 records</li>
 * </ul>
 */
public class ParameterizedCsvExportFacade implements ExportFacade {

  private final ParameterizedOutputStreamGenerator streamGenerator;
  private final MapExportSerializeStrategy serializeStrategy;
  private final Optional<Iterable<String>> header;
  private final InnerWriter innerWriter;
  private final QueueService queueService;

  /**
   * Constructs a parameterized CSV export facade that manages multiple replicate files.
   *
   * @param streamGenerator Strategy to generate output streams per replicate
   * @param serializeStrategy The strategy to use in serializing records before writing
   */
  public ParameterizedCsvExportFacade(ParameterizedOutputStreamGenerator streamGenerator,
                                      MapExportSerializeStrategy serializeStrategy) {
    this.streamGenerator = streamGenerator;
    this.serializeStrategy = serializeStrategy;
    this.header = Optional.empty();
    this.innerWriter = new InnerWriter(streamGenerator, serializeStrategy, Optional.empty());
    this.queueService = CompatibilityLayerKeeper.get().createQueueService(innerWriter);
  }

  /**
   * Constructs a parameterized CSV export facade with specified headers.
   *
   * @param streamGenerator Strategy to generate output streams per replicate
   * @param serializeStrategy The strategy to use in serializing records before writing
   * @param header Iterable over the header columns to use for CSV output
   */
  public ParameterizedCsvExportFacade(
      ParameterizedOutputStreamGenerator streamGenerator,
      MapExportSerializeStrategy serializeStrategy,
      Iterable<String> header) {
    this.streamGenerator = streamGenerator;
    this.serializeStrategy = serializeStrategy;
    this.header = Optional.of(header);
    this.innerWriter = new InnerWriter(
        streamGenerator, serializeStrategy, Optional.of(header));
    this.queueService = CompatibilityLayerKeeper.get().createQueueService(innerWriter);
  }

  @Override
  public void start() {
    queueService.start();
  }

  @Override
  public void join() {
    queueService.join();
  }

  @Override
  public void write(Entity entity, long step, int replicateNumber) {
    ExportTask task = new ExportTask(entity, step, replicateNumber);
    write(task);
  }

  @Override
  public void write(NamedMap namedMap, long step, int replicateNumber) {
    ExportTask task = new ExportTask(namedMap, step, replicateNumber);
    write(task);
  }

  @Override
  public void write(Entity entity, long step) {
    write(entity, step, 0);
  }

  @Override
  public void write(NamedMap namedMap, long step) {
    write(namedMap, step, 0);
  }

  /**
   * Adds a task to the queue for processing.
   *
   * @param task The export task containing data to be queued for export processing
   */
  public void write(ExportTask task) {
    queueService.add(task);
  }

  @Override
  public Optional<MapExportSerializeStrategy> getSerializeStrategy() {
    return Optional.of(serializeStrategy);
  }

  /**
   * Interface for strategy which generates output streams based on replicate number.
   *
   * <p>This interface follows the same pattern as
   * GeotiffExportFacade.ParameterizedOutputStreamGenerator
   * but is specialized for replicate-based file generation.</p>
   */
  public interface ParameterizedOutputStreamGenerator {

    /**
     * Generate an output stream based on the provided StreamReference instance.
     *
     * @param reference The StreamReference object containing the replicate number
     *                  required to generate the file path
     * @return New output stream for this reference
     */
    OutputStream getStream(StreamReference reference);
  }

  /**
   * Key for a set of related data identified by replicate number.
   *
   * <p>Simple reference class containing the replicate number for stream identification.
   * This follows the same pattern as GeotiffExportFacade.StreamReference but simplified
   * for replicate-only parameterization.</p>
   */
  public static class StreamReference {

    private final int replicate;

    /**
     * Constructs a new StreamReference key with the specified replicate number.
     *
     * @param replicate The replicate number associated with the stream reference
     */
    public StreamReference(int replicate) {
      this.replicate = replicate;
    }

    /**
     * Retrieves the replicate number associated with this key.
     *
     * @return The replicate number associated with this key
     */
    public int getReplicate() {
      return replicate;
    }

    @Override
    public String toString() {
      return "StreamReference [replicate=" + replicate + "]";
    }

    @Override
    public int hashCode() {
      return Integer.hashCode(replicate);
    }

    @Override
    public boolean equals(Object other) {
      if (other == null) {
        return false;
      }
      if (!(other instanceof StreamReference)) {
        return false;
      }
      return replicate == ((StreamReference) other).replicate;
    }
  }

  /**
   * Callback to write to the different CSV output streams per replicate.
   *
   * <p>This inner writer manages multiple CSV streams, one per replicate number,
   * following the same pattern as GeotiffExportFacade.InnerWriter but specialized
   * for CSV output.</p>
   */
  private static class InnerWriter implements QueueServiceCallback {
    private final ParameterizedOutputStreamGenerator streamGenerator;
    private final MapExportSerializeStrategy serializeStrategy;
    private final Optional<Iterable<String>> header;
    private final Map<StreamReference, OutputStream> outputStreams;
    private final Map<StreamReference, CsvWriteStrategy> writeStrategies;

    /**
     * Build InnerWriter for managing the writing of serialized records to CSV streams.
     *
     * @param streamGenerator Strategy to use in generating paths for new output files
     * @param serializeStrategy Strategy to use for serializing entity data
     * @param header Optional header columns for CSV output
     */
    public InnerWriter(ParameterizedOutputStreamGenerator streamGenerator,
                       MapExportSerializeStrategy serializeStrategy,
                       Optional<Iterable<String>> header) {
      this.streamGenerator = streamGenerator;
      this.serializeStrategy = serializeStrategy;
      this.header = header;
      this.outputStreams = new HashMap<>();
      this.writeStrategies = new HashMap<>();
    }

    @Override
    public void onStart() {
      // No initialization needed
    }

    @Override
    public void onTask(Optional<Object> taskMaybe) {
      if (taskMaybe.isEmpty()) {
        return;
      }

      ExportTask task = (ExportTask) taskMaybe.get();
      int replicateNumber = task.getReplicateNumber();
      long step = task.getStep();
      StreamReference reference = new StreamReference(replicateNumber);

      try {
        // Ensure we have output stream and write strategy for this replicate
        if (!outputStreams.containsKey(reference)) {
          outputStreams.put(reference, streamGenerator.getStream(reference));
        }

        if (!writeStrategies.containsKey(reference)) {
          if (header.isPresent()) {
            writeStrategies.put(reference, new CsvWriteStrategy(header.get()));
          } else {
            writeStrategies.put(reference, new CsvWriteStrategy());
          }
        }

        // Serialize the entity or use pre-serialized data
        Map<String, String> original;
        if (task.hasEntity()) {
          Entity entity = task.getEntity().get();
          original = serializeStrategy.getRecord(entity);
        } else {
          NamedMap namedMap = task.getNamedMap().get();
          original = new HashMap<>(namedMap.getTarget());
        }

        // Create output record with proper column ordering
        Map<String, String> serialized = new LinkedHashMap<>();

        // Add all original data first
        original.entrySet().forEach(entry -> serialized.put(entry.getKey(), entry.getValue()));

        // Add step column (before replicate to match consolidated CSV behavior)
        serialized.put("step", Long.toString(step));

        // Add replicate column as last column (for consistency with consolidated export)
        serialized.put("replicate", Integer.toString(replicateNumber));

        // Write to the appropriate CSV file
        OutputStream outputStream = outputStreams.get(reference);
        CsvWriteStrategy writeStrategy = writeStrategies.get(reference);
        writeStrategy.write(serialized, outputStream);

      } catch (IOException e) {
        throw new RuntimeException("Error writing to CSV output stream for replicate "
            + replicateNumber, e);
      }
    }

    @Override
    public void onEnd() {
      for (StreamReference reference : outputStreams.keySet()) {
        onEnd(reference);
      }

      outputStreams.clear();
      writeStrategies.clear();
    }

    /**
     * Perform cleanup for an individual replicate stream.
     *
     * @param reference The stream reference whose related resources should be closed
     */
    private void onEnd(StreamReference reference) {
      CsvWriteStrategy writeStrategy = writeStrategies.get(reference);
      if (writeStrategy != null) {
        writeStrategy.close();
      }

      try {
        OutputStream outputStream = outputStreams.get(reference);
        if (outputStream != null) {
          outputStream.close();
        }
      } catch (IOException e) {
        throw new RuntimeException("Error closing CSV output stream for replicate "
            + reference.getReplicate(), e);
      }
    }
  }
}
