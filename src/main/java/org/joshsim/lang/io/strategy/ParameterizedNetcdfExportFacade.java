/**
 * Export facade for writing multi-replicate datasets to separate NetCDF files.
 *
 * <p>This facade creates separate NetCDF files per replicate, enabling memory-efficient
 * processing by allowing completed replicates to be freed from memory. Each file
 * still includes replicate dimension for consistency with consolidated NetCDF behavior.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
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
 * Export facade which creates separate NetCDF files per replicate.
 *
 * <p>This facade enables memory-efficient processing by writing each replicate
 * to its own NetCDF file. Unlike consolidated NetCDF export, this approach allows
 * completed replicates to be freed from memory while large simulations are running.
 * Each file still contains a replicate dimension for consistency.</p>
 *
 * <p>Example output files for 3 replicates:
 * <ul>
 *   <li>simulation_1.nc - Contains replicate=1 time series data</li>
 *   <li>simulation_2.nc - Contains replicate=2 time series data</li>
 *   <li>simulation_3.nc - Contains replicate=3 time series data</li>
 * </ul>
 */
public class ParameterizedNetcdfExportFacade implements ExportFacade {

  private final ParameterizedOutputStreamGenerator streamGenerator;
  private final MapExportSerializeStrategy serializeStrategy;
  private final List<String> variables;
  private final InnerWriter innerWriter;
  private final QueueService queueService;

  /**
   * Constructs a parameterized NetCDF export facade that manages multiple replicate files.
   *
   * @param streamGenerator Strategy to generate output streams per replicate
   * @param serializeStrategy The strategy to use in serializing records before writing
   * @param variables The list of variables to include in each NetCDF file
   */
  public ParameterizedNetcdfExportFacade(
      ParameterizedOutputStreamGenerator streamGenerator,
      MapExportSerializeStrategy serializeStrategy,
      List<String> variables) {
    this.streamGenerator = streamGenerator;
    this.serializeStrategy = serializeStrategy;
    this.variables = variables;
    this.innerWriter = new InnerWriter(
        streamGenerator, serializeStrategy, variables);
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
   * but is specialized for replicate-based NetCDF file generation.</p>
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
   * Key for a set of related NetCDF data identified by replicate number.
   *
   * <p>Simple reference class containing the replicate number for stream identification.
   * This follows the same pattern as ParameterizedCsvExportFacade.StreamReference.</p>
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
   * Callback to write to the different NetCDF output streams per replicate.
   *
   * <p>This inner writer manages multiple NetCDF streams, one per replicate number,
   * following the same pattern as ParameterizedCsvExportFacade.InnerWriter but
   * specialized for NetCDF output.</p>
   */
  private static class InnerWriter implements QueueServiceCallback {
    private final ParameterizedOutputStreamGenerator streamGenerator;
    private final MapExportSerializeStrategy serializeStrategy;
    private final List<String> variables;
    private final Map<StreamReference, OutputStream> outputStreams;
    private final Map<StreamReference, NetcdfWriteStrategy> writeStrategies;

    /**
     * Build InnerWriter for managing the writing of serialized records to NetCDF streams.
     *
     * @param streamGenerator Strategy to use in generating paths for new output files
     * @param serializeStrategy Strategy to use for serializing entity data
     * @param variables List of variables to include in each NetCDF file
     */
    public InnerWriter(ParameterizedOutputStreamGenerator streamGenerator,
                       MapExportSerializeStrategy serializeStrategy,
                       List<String> variables) {
      this.streamGenerator = streamGenerator;
      this.serializeStrategy = serializeStrategy;
      this.variables = variables;
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
        // Flush all write strategies
        for (NetcdfWriteStrategy writeStrategy : writeStrategies.values()) {
          writeStrategy.flush();
        }
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
          writeStrategies.put(reference, new NetcdfWriteStrategy(variables));
        }

        // Serialize the entity or use pre-serialized data
        Map<String, String> serialized;
        if (task.hasEntity()) {
          Entity entity = task.getEntity().get();
          serialized = serializeStrategy.getRecord(entity);
        } else {
          NamedMap namedMap = task.getNamedMap().get();
          serialized = new HashMap<>(namedMap.getTarget());
        }

        // Add step and replicate information
        serialized.put("step", Long.toString(step));
        serialized.put("replicate", Integer.toString(replicateNumber));

        // Write to the appropriate NetCDF file
        OutputStream outputStream = outputStreams.get(reference);
        NetcdfWriteStrategy writeStrategy = writeStrategies.get(reference);
        writeStrategy.write(serialized, outputStream);

      } catch (IOException e) {
        throw new RuntimeException("Error writing to NetCDF output stream for replicate "
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
      NetcdfWriteStrategy writeStrategy = writeStrategies.get(reference);
      if (writeStrategy != null) {
        writeStrategy.close();
      }

      try {
        OutputStream outputStream = outputStreams.get(reference);
        if (outputStream != null) {
          outputStream.close();
        }
      } catch (IOException e) {
        throw new RuntimeException("Error closing NetCDF output stream for replicate "
            + reference.getReplicate(), e);
      }
    }
  }
}
