/**
 * Logic to perisist a multi-variate dataset to multiple geotiffs.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.QueueService;
import org.joshsim.compat.QueueServiceCallback;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.lang.io.ExportFacade;
import org.joshsim.wire.NamedMap;


/**
 * Export facade which splits a dataset across many geotiffs.
 *
 * <p>Export facade which creates one geotiff per combination of variable and step though this may
 * be used to also create those per replicate as well.</p>
 */
public class GeotiffExportFacade implements ExportFacade {

  private final MapExportSerializeStrategy serializeStrategy;
  private final List<String> variables;
  private final InnerWriter innerWriter;
  private final QueueService queueService;

  /**
   * Constructs a geotiff export facade that manages multiple files.
   *
   * @param streamGenerator Strategy to build output streams per geotiff.
   * @param serializeStrategy The strategy to use in serializing records before writing.
   * @param variables The list of variables to include across geotiffs with one geotiff per
   *     variable.
   * @param extents The extents in Earth-space for the simulation.
   * @param width The width and height of each patch and, thus, each pixel in meters.
   */
  public GeotiffExportFacade(ParameterizedOutputStreamGenerator streamGenerator,
        MapExportSerializeStrategy serializeStrategy, List<String> variables,
        PatchBuilderExtents extents, BigDecimal width) {
    this.serializeStrategy = serializeStrategy;
    this.variables = variables;
    innerWriter = new InnerWriter(streamGenerator, extents, width);
    queueService = CompatibilityLayerKeeper.get().createQueueService(innerWriter);
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
    // For GeoTIFF, replicate number is handled by file separation, not as data column
    Map<String, String> serialized = serializeStrategy.getRecord(entity);
    writeFromSerializedData(serialized, step);
  }

  @Override
  public void write(NamedMap namedMap, long step, int replicateNumber) {
    // For GeoTIFF, replicate number is handled by file separation, not as data column
    writeFromSerializedData(namedMap.getTarget(), step);
  }

  /**
   * Adds a task to the queue for processing.
   *
   * @param task The task containing coordinates and value data to be queued for export processing.
   */
  public void write(Task task) {
    queueService.add(task);
  }

  /**
   * Common method to write from serialized data regardless of source (Entity or NamedMap).
   *
   * @param serialized The serialized data map containing coordinates and variable values
   * @param step The simulation step number
   */
  private void writeFromSerializedData(Map<String, String> serialized, long step) {
    String longitude = serialized.get("position.longitude");
    String latitude = serialized.get("position.latitude");
    String stepStr = "" + step;

    for (String varName : variables) {
      StreamReference reference = new StreamReference(stepStr, varName);
      String valueRaw = serialized.get(varName);
      String value = valueRaw == null ? "0" : valueRaw;
      Task task = new Task(reference, longitude, latitude, value);
      write(task);
    }
  }

  /**
   * Interface for strategy which generates output streams based on a provided StreamReference.
   */
  public interface ParameterizedOutputStreamGenerator {

    /**
     * Generate an output stream based on the provided StreamReference instance.
     *
     * @param reference The StreamReference object containing the data (step and variable)
     *     required to generate the file path.
     * @return New output stream for this reference.
     */
    OutputStream getStream(StreamReference reference);
  }

  /**
   * Key for a set of related data.
   *
   * <p>Description of a set of related data where each are written to an individual geotiff.
   * Specifically, each unique combination of step, variable, and replicate number are written
   * to an individual geotiff but replicate is managed outside this utility.</p>
   */
  public static class StreamReference {

    private final String step;
    private final String variable;

    /**
     * Constructs a new StreamReference key with the specified step and variable.
     *
     * @param step The step value associated with the stream reference.
     * @param variable The variable name associated with the stream reference.
     */
    public StreamReference(String step, String variable) {
      this.step = step;
      this.variable = variable;
    }

    /**
     * Retrieves the timestep value associated with this key.
     *
     * @return The timestep associated with this key.
     */
    public String getStep() {
      return step;
    }

    /**
     * Retrieves the variable name associated with this key.
     *
     * @return The variable name as a String.
     */
    public String getVariable() {
      return variable;
    }

    /**
     * Get a string representation of this key.
     *
     * @return String which uniquely identifies this key's group.
     */
    @Override
    public String toString() {
      return "StreamReference [step=" + step + ", variable=" + variable + "]";
    }

    @Override
    public int hashCode() {
      return toString().hashCode();
    }

    @Override
    public boolean equals(Object other) {
      if (other == null) {
        return false;
      }
      if (!(other instanceof StreamReference)) {
        return false;
      }
      return toString().equals(other.toString());
    }

  }

  /**
   * Represents a task containing position, step, variable, and value.
   */
  public static class Task {
    private final StreamReference reference;
    private final String longitude;
    private final String latitude;
    private final String value;

    /**
     * Create Task with the specified geographical coordinates, a step value, and a numeric value.
     *
     * @param reference Information about the data point enclosed.
     * @param longitude The longitude at which this value is being reported in degrees.
     * @param latitude The latitude at which this value is being reported in degrees.
     * @param value The value of the data point to be written.
     */
    public Task(StreamReference reference, String longitude, String latitude, String value) {
      this.reference = reference;
      this.longitude = longitude;
      this.latitude = latitude;
      this.value = value;
    }

    /**
     * Retrieves the stream reference associated with this task.
     *
     * @return The StreamReference object containing the related data information.
     */
    public StreamReference getReference() {
      return reference;
    }

    /**
     * Retrieves the longitude coordinate associated with this task.
     *
     * @return The longitude value in degrees as a string.
     */
    public String getLongitude() {
      return longitude;
    }

    /**
     * Retrieves the latitude coordinate associated with this task.
     *
     * @return The latitude value in degrees as a string.
     */
    public String getLatitude() {
      return latitude;
    }


    /**
     * Retrieves the numeric value associated with this task to write at this position.
     *
     * @return The value as a string to be reported at this position.
     */
    public String getValue() {
      return value;
    }

  }

  /**
   * Callback to write to the different output streams and write strategies.
   *
   * <p>Callback to write to the different output streams and write strategies with one per geotiff
   * which corresponds to combination of step, variable, and replicate.</p>
   */
  private static class InnerWriter implements QueueServiceCallback {
    private final GeotiffDimensions dimensions;
    private final ParameterizedOutputStreamGenerator streamGenerator;
    private Map<StreamReference, OutputStream> outputStreams;
    private Map<StreamReference, StringMapWriteStrategy> writeStrategies;

    /**
     * Build InnerWriter for managing the writing of serialized records to an output stream.
     *
     * @param streamGenerator Strategy to use in generating paths for new output files.
     * @param extents The extents in Earth-space for the simulation.
     * @param width The width and height of each patch and, thus, each pixel in meters.
     */
    public InnerWriter(ParameterizedOutputStreamGenerator streamGenerator,
          PatchBuilderExtents extents, BigDecimal width) {
      this.streamGenerator = streamGenerator;
      dimensions = new GeotiffDimensions(extents, width);
      outputStreams = new HashMap<>();
      writeStrategies = new HashMap<>();
    }

    @Override
    public void onStart() {}

    @Override
    public void onTask(Optional<Object> taskMaybe) {
      if (taskMaybe.isEmpty()) {
        return;
      }

      Task task = (Task) taskMaybe.get();
      StreamReference reference = task.getReference();

      try {
        String variableName = reference.getVariable();

        if (!outputStreams.containsKey(reference)) {
          outputStreams.put(reference, streamGenerator.getStream(reference));
        }

        if (!writeStrategies.containsKey(reference)) {
          writeStrategies.put(reference, new GeotiffWriteStrategy(variableName, dimensions));
        }

        Map<String, String> serialized = new HashMap<>();
        serialized.put("step", reference.getStep());
        serialized.put("position.longitude", task.getLongitude());
        serialized.put("position.latitude", task.getLatitude());
        serialized.put(variableName, task.getValue());

        OutputStream outputStream = outputStreams.get(reference);
        StringMapWriteStrategy writeStrategy = writeStrategies.get(reference);
        writeStrategy.write(serialized, outputStream);
      } catch (IOException e) {
        throw new RuntimeException("Error writing to output stream", e);
      }
    }

    @Override
    public void onEnd() {
      for (StreamReference reference : outputStreams.keySet()) {
        onEnd(reference);
      }

      outputStreams = new HashMap<>();
      writeStrategies = new HashMap<>();
    }

    /**
     * Perform onEnd responsibilities for an individual stream.
     *
     * @param reference The stream reference whose related resources should be closed.
     */
    private void onEnd(StreamReference reference) {
      StringMapWriteStrategy writeStrategy = writeStrategies.get(reference);
      writeStrategy.close();

      try {
        OutputStream outputStream = outputStreams.get(reference);
        outputStream.close();
      } catch (IOException e) {
        throw new RuntimeException("Error closing output stream", e);
      }
    }
  }

}
