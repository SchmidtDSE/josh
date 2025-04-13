/**
 * Structures to simplify writing entities to CSV.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.export;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.joshsim.engine.entity.base.Entity;


/**
 * Strategy implementing ExportFacade which writes entities to CSV in a writer thread.
 */
public class CsvExportFacade implements ExportFacade {

  private final OutputStreamStrategy outputStrategy;

  private final Queue<Entity> entityQueue = new ConcurrentLinkedQueue<>();
  private final ExecutorService executorService = Executors.newSingleThreadExecutor();
  private final AtomicBoolean active = new AtomicBoolean(false);

  /**
   * Constructs a CsvExportFacade object with the specified export target / output stream strategy.
   *
   * @param outputStrategy The strategy to provide an output stream for writing the exported data.
   */
  public CsvExportFacade(OutputStreamStrategy outputStrategy) {
    this.outputStrategy = outputStrategy;
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
        ExportWriteStrategy<Map<String, String>> writeStrategy = new CsvWriteStrategy();

        while (active.get() || !entityQueue.isEmpty()) {
          Entity entity = entityQueue.poll();
          if (entity == null) {
            writeStrategy.flush();
            trySleep();
          } else {
            try {
              Map<String, String> serialized = serializeStrategy.getRecord(entity);
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
  public void write(Entity entity) {
    if (!active.get()) {
      throw new IllegalStateException("CsvExportFacade is not active. Cannot write entities.");
    }
    entityQueue.add(entity);
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
