/**
 * Structures to simplify writing entities to CSV.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.export;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.joshsim.compat.ExportFacadeTaskQueue;
import org.joshsim.compat.ThreadSafeExportFacadeTaskQueue;
import org.joshsim.engine.entity.base.Entity;


/**
 * Strategy implementing ExportFacade which writes entities to CSV in a writer thread.
 */
public class CsvExportFacade implements ExportFacade {

  private final OutputStreamStrategy outputStrategy;

  private final ExportFacadeTaskQueue entityQueue = new ThreadSafeExportFacadeTaskQueue();
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final AtomicBoolean active = new AtomicBoolean(false);
  private final Optional<Iterable<String>> header;

  /**
   * Constructs a CsvExportFacade object with the specified export target / output stream strategy.
   *
   * @param outputStrategy The strategy to provide an output stream for writing the exported data.
   */
  public CsvExportFacade(OutputStreamStrategy outputStrategy) {
    this.outputStrategy = outputStrategy;
    header = Optional.empty();
  }

  /**
   * Constructs a CsvExportFacade with the specified export target / output stream and headers.
   *
   * @param outputStrategy The strategy to provide an output stream for writing the exported data.
   * @param header Iterable over the header columns to use for the CSV output.
   */
  public CsvExportFacade(OutputStreamStrategy outputStrategy, Iterable<String> header) {
    this.outputStrategy = outputStrategy;
    this.header = Optional.of(header);
  }

  @Override
  public void start() {
    if (active.compareAndSet(false, true)) {
      executorService.submit(() -> {

        OutputStream outputStream;
        try {
          outputStream = outputStrategy.open();
        } catch (IOException e) {
          throw new RuntimeException("Error opening output stream", e);
        }

        ExportSerializeStrategy<Map<String, String>> serializeStrategy = new MapSerializeStrategy();

        ExportWriteStrategy<Map<String, String>> writeStrategy;
        if (header.isPresent()) {
          writeStrategy = new CsvWriteStrategy(header.get());
        } else {
          writeStrategy = new CsvWriteStrategy();
        }

        while (active.get()) {
          ExportTask task = entityQueue.dequeue();

          if (task == null) {
            writeStrategy.flush();
            trySleep();
          } else {
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
        }

        writeStrategy.flush();

        try {
          outputStream.close();
        } catch (IOException e) {
          throw new RuntimeException("Error closing output stream", e);
        }
      });
    }
  }

  @Override
  public void join() {
    active.set(false);
    executorService.shutdown();
    while (!executorService.isTerminated()) {
      trySleep();
    }
  }

  @Override
  public void write(Entity entity, long step) {
    ExportTask task = new ExportTask(entity, step);
    write(task);
  }

  /**
   * Adds a task to the queue for processing while ensuring the export process is active.
   *
   * @param task The task containing an entity and step value to be queued for export processing.
   * @throws IllegalStateException If the export process is not active when this method is invoked.
   */
  public void write(ExportTask task) {
    if (!active.get()) {
      throw new IllegalStateException("CsvExportFacade is not active. Cannot write entities.");
    }

    entityQueue.enqueue(task);
  }

  /**
   * Causes the executing thread to sleep to avoid thrashing.
   *
   * @throws RuntimeException If the thread is interrupted during the sleep operation.
   */
  private void trySleep() {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while sleeping", e);
    }
  }

}
