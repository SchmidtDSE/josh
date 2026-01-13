/**
 * Facade for writing debug output messages to a single destination.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Optional;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.compat.QueueService;
import org.joshsim.compat.QueueServiceCallback;

/**
 * Writes debug messages to a single output destination (file, minio, stdout).
 *
 * <p>This facade follows the same architectural pattern as {@link ExportFacade} implementations
 * like {@link org.joshsim.lang.io.strategy.CsvExportFacade}:</p>
 * <ul>
 *   <li>Uses {@link QueueService} for asynchronous writes to avoid blocking simulation</li>
 *   <li>Uses {@link OutputStreamStrategy} for destination abstraction (file, minio, stdout)</li>
 *   <li>Has start/join lifecycle for background thread management</li>
 * </ul>
 *
 * <p><b>Key difference from ExportFacade:</b> This facade writes plain text strings (from the
 * {@code debug()} function), while ExportFacade implementations write serialized Entity data.
 * The input type difference (String vs Entity) is why this doesn't implement the ExportFacade
 * interface directly.</p>
 *
 * <p>Messages are formatted with step number and entity type context:</p>
 * <pre>[Step 5, patch] Your debug message here</pre>
 *
 * <p>Example usage:</p>
 * <pre>
 * OutputStreamStrategy strategy = new LocalOutputStreamStrategy("/tmp/debug.txt");
 * DebugOutputFacade facade = new DebugOutputFacade(strategy);
 * facade.start();
 * facade.write("Hello from simulation", 5, "patch");
 * facade.join();
 * </pre>
 *
 * @see ExportFacade
 * @see CombinedDebugOutputFacade
 */
public class DebugOutputFacade {

  private final QueueService queueService;
  private final InnerWriter innerWriter;

  /**
   * Creates a debug output facade for the specified output strategy.
   *
   * @param outputStrategy The strategy for opening the output stream.
   */
  public DebugOutputFacade(OutputStreamStrategy outputStrategy) {
    this.innerWriter = new InnerWriter(outputStrategy);
    this.queueService = CompatibilityLayerKeeper.get().createQueueService(innerWriter);
  }

  /**
   * Starts the facade's background queue service.
   *
   * <p>Must be called before {@link #write}. The background thread will process
   * queued messages until {@link #join} is called.</p>
   */
  public void start() {
    queueService.start();
  }

  /**
   * Waits for all queued messages to be written and shuts down.
   *
   * <p>Blocks until the background thread has processed all pending messages
   * and closed the output stream.</p>
   */
  public void join() {
    queueService.join();
  }

  /**
   * Writes a debug message with context information.
   *
   * <p>The message is queued for asynchronous writing. The actual write happens
   * in the background thread to avoid blocking the simulation.</p>
   *
   * @param message The debug message to write.
   * @param step The current simulation step number.
   * @param entityType The type of entity generating the message (e.g., "patch", "agent").
   */
  public void write(String message, long step, String entityType) {
    DebugMessage debugMessage = new DebugMessage(message, step, entityType);
    queueService.add(debugMessage);
  }

  /**
   * Internal record to hold debug message data for queue processing.
   */
  private record DebugMessage(String message, long step, String entityType) {}

  /**
   * Internal callback that handles writing debug messages to the output stream.
   *
   * <p>This follows the same pattern as the InnerWriter in CsvExportFacade.</p>
   */
  private static class InnerWriter implements QueueServiceCallback {

    private final OutputStream outputStream;
    private final PrintWriter writer;
    private final boolean isStdout;

    /**
     * Creates the inner writer with the specified output strategy.
     *
     * @param outputStrategy The strategy for opening the output stream.
     * @throws RuntimeException if the output stream cannot be opened.
     */
    public InnerWriter(OutputStreamStrategy outputStrategy) {
      try {
        this.outputStream = outputStrategy.open();
        this.writer = new PrintWriter(outputStream, true);
        this.isStdout = outputStrategy instanceof StdoutOutputStreamStrategy;
      } catch (IOException e) {
        throw new RuntimeException("Error opening debug output stream", e);
      }
    }

    @Override
    public void onStart() {
      // No initialization needed
    }

    @Override
    public void onTask(Optional<Object> taskMaybe) {
      if (taskMaybe.isEmpty()) {
        writer.flush();
        return;
      }

      DebugMessage msg = (DebugMessage) taskMaybe.get();
      String formatted = String.format("[Step %d, %s] %s",
          msg.step(), msg.entityType(), msg.message());
      writer.println(formatted);
    }

    @Override
    public void onEnd() {
      writer.flush();
      // Don't close stdout, but close files
      if (!isStdout) {
        writer.close();
        try {
          outputStream.close();
        } catch (IOException e) {
          throw new RuntimeException("Error closing debug output stream", e);
        }
      }
    }
  }

}
