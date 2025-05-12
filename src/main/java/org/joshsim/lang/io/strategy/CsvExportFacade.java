/**
 * Structures to simplify writing entities to CSV.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.QueueService;
import org.joshsim.compat.QueueServiceCallback;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.lang.io.ExportFacade;
import org.joshsim.lang.io.OutputStreamStrategy;


/**
 * Strategy implementing ExportFacade which writes entities to CSV in a writer thread.
 */
public class CsvExportFacade implements ExportFacade {

  private final OutputStreamStrategy outputStrategy;
  private final Optional<Iterable<String>> header;
  private final InnerWriter innerWriter;
  private final QueueService queueService;

  /**
   * Constructs a CsvExportFacade object with the specified export target / output stream strategy.
   *
   * @param outputStrategy The strategy to provide an output stream for writing the exported data.
   * @param serializeStrategy The strategy to use in serializing records before writing.
   */
  public CsvExportFacade(OutputStreamStrategy outputStrategy,
        MapExportSerializeStrategy serializeStrategy) {
    this.outputStrategy = outputStrategy;
    header = Optional.empty();
    innerWriter = new InnerWriter(header, outputStrategy, serializeStrategy);
    queueService = CompatibilityLayerKeeper.get().createQueueService(innerWriter);
  }

  /**
   * Constructs a CsvExportFacade with the specified export target / output stream and headers.
   *
   * @param outputStrategy The strategy to provide an output stream for writing the exported data.
   * @param serializeStrategy The strategy to use in serializing records before writing.
   * @param header Iterable over the header columns to use for the CSV output.
   */
  public CsvExportFacade(OutputStreamStrategy outputStrategy,
        MapExportSerializeStrategy serializeStrategy, Iterable<String> header) {
    this.outputStrategy = outputStrategy;
    this.header = Optional.of(header);
    innerWriter = new InnerWriter(Optional.of(header), outputStrategy, serializeStrategy);
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
  public void write(Entity entity, long step) {
    Task task = new Task(entity, step);
    write(task);
  }

  /**
   * Adds a task to the queue for processing while ensuring the export process is active.
   *
   * @param task The task containing an entity and step value to be queued for export processing.
   * @throws IllegalStateException If the export process is not active when this method is invoked.
   */
  public void write(Task task) {
    queueService.add(task);
  }

  /**
   * Represents a task containing an entity and a step value.
   */
  public static class Task {

    private final Entity entity;

    private final long step;

    /**
     * Constructs a new Task with the specified entity and step value.
     *
     * @param entity The entity associated with this task.
     * @param step   The step value representing additional metadata for this task.
     */
    public Task(Entity entity, long step) {
      this.entity = entity;
      this.step = step;
    }

    /**
     * Get the entity associated with this task.
     *
     * @return The entity object.
     */
    public Entity getEntity() {
      return entity;
    }

    /**
     * Get the step value for this task.
     *
     * @return The step value as a long.
     */
    public long getStep() {
      return step;
    }
  }

  /**
   * Callback to write to a CSV file.
   */
  private static class InnerWriter implements QueueServiceCallback {

    private final Optional<Iterable<String>> header;
    private final OutputStream outputStream;
    private final MapExportSerializeStrategy serializeStrategy;
    private final StringMapWriteStrategy writeStrategy;

    /**
     * Create a writer which writes to the CSV file as data become avialable.
     *
     * @param header an optional iterable containing the header values for the CSV output. If not
     *      present, the header will be inferred.
     * @param outputStrategy the strategy to provide an OutputStream instance. This determines where
     *      the data will be written.
     * @param serializeStrategy the strategy used to serialize data records into a string
     *      representation  suitable for writing to the output stream.
     * @throws RuntimeException if an exception occurs while attempting to open the OutputStream.
     */
    public InnerWriter(Optional<Iterable<String>> header, OutputStreamStrategy outputStrategy,
          MapExportSerializeStrategy serializeStrategy) {
      this.header = header;
      this.serializeStrategy = serializeStrategy;

      try {
        outputStream = outputStrategy.open();
      } catch (IOException e) {
        throw new RuntimeException("Error opening output stream", e);
      }

      if (header.isPresent()) {
        writeStrategy = new CsvWriteStrategy(header.get());
      } else {
        writeStrategy = new CsvWriteStrategy();
      }
    }

    @Override
    public void onStart() {}

    @Override
    public void onTask(Optional<Object> taskMaybe) {
      if (taskMaybe.isEmpty()) {
        writeStrategy.flush();
        return;
      }

      Task task = (Task) taskMaybe.get();

      Entity entity = task.getEntity();
      long step = task.getStep();

      try {
        Map<String, String> serialized = serializeStrategy.getRecord(entity);
        serialized.put("step", Long.toString(step));
        writeStrategy.write(serialized, outputStream);
      } catch (IOException e) {
        throw new RuntimeException("Error writing to output stream", e);
      }
    }

    @Override
    public void onEnd() {
      writeStrategy.close();

      try {
        outputStream.close();
      } catch (IOException e) {
        throw new RuntimeException("Error closing output stream", e);
      }
    }
  }

}
