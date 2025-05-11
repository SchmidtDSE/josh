/**
 * Structures to simplify writing entities to CSV.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.QueueService;
import org.joshsim.compat.QueueServiceCallback;
import org.joshsim.engine.entity.base.Entity;


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
        ExportSerializeStrategy<Map<String, String>> serializeStrategy) {
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
        ExportSerializeStrategy<Map<String, String>> serializeStrategy, Iterable<String> header) {
    this.outputStrategy = outputStrategy;
    this.header = Optional.of(header);
    innerWriter = new InnerWriter(Optional.of(header), outputStrategy);
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

  private static class InnerWriter implements QueueServiceCallback {

    private final Optional<Iterable<String>> header;
    private final OutputStream outputStream;
    private final ExportSerializeStrategy<Map<String, String>> serializeStrategy;
    private final ExportWriteStrategy<Map<String, String>> writeStrategy;

    public InnerWriter(Optional<Iterable<String>> header, OutputStreamStrategy outputStrategy,
          ExportSerializeStrategy<Map<String, String>> serializeStrategy) {
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
      writeStrategy.flush();

      try {
        outputStream.close();
      } catch (IOException e) {
        throw new RuntimeException("Error closing output stream", e);
      }
    }
  }

}
