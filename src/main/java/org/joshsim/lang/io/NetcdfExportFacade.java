
/**
 * Structures to simplify writing entities to NetCDF.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.QueueService;
import org.joshsim.compat.QueueServiceCallback;
import org.joshsim.engine.entity.base.Entity;

/**
 * Strategy implementing ExportFacade which writes entities to NetCDF in a writer thread.
 */
public class NetcdfExportFacade implements ExportFacade {

  private final OutputStreamStrategy outputStrategy;
  private final List<String> variables;
  private final InnerWriter innerWriter;
  private final QueueService queueService;

  /**
   * Constructs a NetcdfExportFacade object with the specified export target and variables.
   *
   * @param outputStrategy The strategy to provide an output stream for writing the exported data.
   * @param serializeStrategy The strategy to use in serializing records before writing.
   * @param variables The list of variables to include in the NetCDF file.
   */
  public NetcdfExportFacade(OutputStreamStrategy outputStrategy,
      ExportSerializeStrategy<Map<String, String>> serializeStrategy,
      List<String> variables) {
    this.outputStrategy = outputStrategy;
    this.variables = variables;
    innerWriter = new InnerWriter(variables, outputStrategy, serializeStrategy);
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
   * Adds a task to the queue for processing.
   *
   * @param task The task containing an entity and step value to be queued for export processing.
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
     * Create a new task.
     *
     * @param entity the Entity to write
     * @param step the simulation timestep on which the entity is reported 
     */
    public Task(Entity entity, long step) {
      this.entity = entity;
      this.step = step;
    }
 
    /**
     * Retrieves the entity associated with this task.
     *
     * @return The entity to be written.
     */
    public Entity getEntity() {
      return entity;
    }

    /**
     * Retrieves the simulation step at which the entity is reported.
     *
     * @return The simulation timestep value associated with this task.
     */
    public long getStep() {
      return step;
    }
  }

  private static class InnerWriter implements QueueServiceCallback {
    private final List<String> variables;
    private final OutputStream outputStream;
    private final ExportSerializeStrategy<Map<String, String>> serializeStrategy;
    private final ExportWriteStrategy<Map<String, String>> writeStrategy;
    private final List<Map<String, String>> pendingRecords;

    public InnerWriter(List<String> variables, OutputStreamStrategy outputStrategy,
        ExportSerializeStrategy<Map<String, String>> serializeStrategy) {
      this.variables = variables;
      this.serializeStrategy = serializeStrategy;
      this.pendingRecords = new ArrayList<>();

      try {
        outputStream = outputStrategy.open();
      } catch (IOException e) {
        throw new RuntimeException("Error opening output stream", e);
      }

      writeStrategy = new NetcdfWriteStrategy(variables);
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
