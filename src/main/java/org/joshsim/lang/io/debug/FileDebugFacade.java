/**
 * Structures to simplify writing debug messages to files.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.debug;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Optional;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.QueueService;
import org.joshsim.compat.QueueServiceCallback;
import org.joshsim.lang.io.OutputStreamStrategy;

/**
 * DebugFacade that writes debug messages to a file using a background writer thread.
 *
 * <p>This facade uses a QueueService to process debug messages in a separate thread,
 * ensuring minimal impact on simulation performance.</p>
 */
public class FileDebugFacade implements DebugFacade {

  private final QueueService queueService;

  /**
   * Constructs a FileDebugFacade with the specified output strategy.
   *
   * @param outputStrategy The strategy to provide an output stream for writing debug messages.
   */
  public FileDebugFacade(OutputStreamStrategy outputStrategy) {
    InnerWriter innerWriter = new InnerWriter(outputStrategy);
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
  public void write(String message, long step, String entityType, int replicateNumber) {
    DebugTask task = new DebugTask(message, step, entityType, replicateNumber);
    queueService.add(task);
  }

  @Override
  public void write(String message, long step, String entityType) {
    write(message, step, entityType, 0);
  }

  /**
   * Callback to write debug messages to output stream.
   */
  private static class InnerWriter implements QueueServiceCallback {
    private final PrintWriter writer;

    /**
     * Creates a writer that writes to the given output stream.
     *
     * @param outputStrategy The strategy to provide an output stream for writing.
     * @throws RuntimeException if an error occurs while opening the output stream.
     */
    public InnerWriter(OutputStreamStrategy outputStrategy) {
      try {
        OutputStream out = outputStrategy.open();
        this.writer = new PrintWriter(out, true); // auto-flush for debugging
      } catch (IOException e) {
        throw new RuntimeException("Error opening debug output stream", e);
      }
    }

    @Override
    public void onStart() {
      // No-op
    }

    @Override
    public void onTask(Optional<Object> taskMaybe) {
      if (taskMaybe.isEmpty()) {
        writer.flush();
        return;
      }

      DebugTask debugTask = (DebugTask) taskMaybe.get();
      String formatted = String.format("[Step %d, %s] %s",
          debugTask.getStep(),
          debugTask.getEntityType(),
          debugTask.getMessage());
      writer.println(formatted);
    }

    @Override
    public void onEnd() {
      writer.close();
    }
  }
}
