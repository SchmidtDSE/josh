/**
 * Structures to simplify writing entities to NetCDF.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.strategy;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.QueueService;
import org.joshsim.compat.QueueServiceCallback;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.lang.io.ExportFacade;
import org.joshsim.lang.io.OutputStreamStrategy;

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
      MapExportSerializeStrategy serializeStrategy, List<String> variables) {
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

  /**
   * Callback for writing to a netCDF file.
   */
  private static class InnerWriter implements QueueServiceCallback {
    private final List<String> variables;
    private final OutputStream outputStream;
    private final MapExportSerializeStrategy serializeStrategy;
    private final StringMapWriteStrategy writeStrategy;

    /**
     * Constructs InnerWriter for writing data to an output stream.
     *
     * @param variables the list of variable names to be processed and written.
     * @param outputStrategy the strategy for opening and managing the output stream.
     * @param serializeStrategy the strategy for serializing data into an exportable format.
     * @throws RuntimeException if there is an error opening the output stream.
     */
    public InnerWriter(List<String> variables, OutputStreamStrategy outputStrategy,
        MapExportSerializeStrategy serializeStrategy) {
      this.variables = variables;
      this.serializeStrategy = serializeStrategy;

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
