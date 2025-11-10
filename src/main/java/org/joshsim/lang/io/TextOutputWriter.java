/**
 * Output writer for plain text debug messages.
 *
 * <p>This writer implements OutputWriter&lt;String&gt; for writing plain text debug messages
 * to various destinations (file, MinIO, stdout, memory). It uses a background queue-based
 * approach to minimize impact on simulation performance.</p>
 *
 * <p>The writer supports both synchronous (stdout, memory) and asynchronous (file, MinIO)
 * writing strategies based on the destination type. For file and MinIO destinations, messages
 * are queued and written by a background thread to avoid blocking the simulation.</p>
 *
 * <p>Message Format:</p>
 *
 * <p>Each debug message is formatted with contextual information:</p>
 * <pre>[Step {step}, {entityType}] {message}</pre>
 *
 * <p>Example usage:</p>
 * <pre>
 * OutputTarget target = new OutputTarget("file", "", "/tmp/debug.txt", "txt");
 * OutputStreamStrategy strategy = new LocalOutputStreamStrategy("/tmp/debug.txt", true);
 * TextOutputWriter writer = new TextOutputWriter(target, strategy);
 * writer.start();
 * writer.write("Debug message", 1);
 * writer.join();
 * </pre>
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
 * OutputWriter implementation for writing plain text strings (debug output).
 *
 * <p>This implementation uses a QueueService for asynchronous writing to file and MinIO
 * destinations, while stdout and memory destinations use synchronous writes for immediate
 * output.</p>
 */
public class TextOutputWriter implements OutputWriter<String> {

  private final OutputTarget target;
  private final QueueService queueService;
  private final boolean isSynchronous;
  private final OutputStream synchronousStream;

  /**
   * Creates a text output writer for the specified target.
   *
   * <p>For file and MinIO destinations, this creates a background queue service for
   * asynchronous writing. For stdout and memory destinations, writes are synchronous.</p>
   *
   * @param target The output target describing the destination
   * @param outputStrategy The strategy for opening the output stream
   * @throws RuntimeException if the output stream cannot be opened
   */
  public TextOutputWriter(OutputTarget target, OutputStreamStrategy outputStrategy) {
    this.target = target;

    // Determine if this is a synchronous destination (stdout or memory)
    String protocol = target.getProtocol().toLowerCase();
    this.isSynchronous = protocol.equals("stdout") || protocol.equals("memory");

    if (isSynchronous) {
      // For stdout/memory, open stream immediately for synchronous writes
      try {
        this.synchronousStream = outputStrategy.open();
        this.queueService = null;
      } catch (IOException e) {
        throw new RuntimeException("Error opening output stream for " + target.toUri(), e);
      }
    } else {
      // For file/MinIO, use queue-based async writing
      this.synchronousStream = null;
      InnerWriter innerWriter = new InnerWriter(outputStrategy);
      this.queueService = CompatibilityLayerKeeper.get().createQueueService(innerWriter);
    }
  }

  @Override
  public void start() {
    if (!isSynchronous && queueService != null) {
      queueService.start();
    }
    // No-op for synchronous destinations
  }

  @Override
  public void join() {
    if (!isSynchronous && queueService != null) {
      queueService.join();
    }
    // No-op for synchronous destinations
  }

  @Override
  public void write(String data, long step) {
    if (isSynchronous) {
      // Synchronous write for stdout/memory
      writeSynchronous(data, step, "");
    } else {
      // Queue for async write
      WriteTask<String> task = new WriteTask<>(data, step);
      queueService.add(task);
    }
  }

  /**
   * Writes a debug message with entity type context.
   *
   * <p>This is an extended version of write() that includes entity type information
   * in the output. This is used by CombinedTextWriter when routing messages.</p>
   *
   * @param data The debug message to write
   * @param step The simulation step number
   * @param entityType The entity type name (e.g., "ForeverTree", "patch")
   */
  public void write(String data, long step, String entityType) {
    if (isSynchronous) {
      writeSynchronous(data, step, entityType);
    } else {
      WriteTask<String> task = new WriteTask<>(data, step, entityType);
      queueService.add(task);
    }
  }

  @Override
  public String getPath() {
    return target.getPath();
  }

  /**
   * Performs synchronous write for stdout/memory destinations.
   *
   * @param message The message to write
   * @param step The simulation step
   * @param entityType The entity type (may be empty)
   */
  private void writeSynchronous(String message, long step, String entityType) {
    String formatted = formatMessage(message, step, entityType);
    try {
      synchronousStream.write(formatted.getBytes());
      synchronousStream.flush();
    } catch (IOException e) {
      throw new RuntimeException("Error writing to " + target.toUri(), e);
    }
  }

  /**
   * Formats a debug message with step and entity type context.
   *
   * @param message The raw message
   * @param step The simulation step
   * @param entityType The entity type (may be empty)
   * @return Formatted message string
   */
  private static String formatMessage(String message, long step, String entityType) {
    if (entityType == null || entityType.isEmpty()) {
      return String.format("[Step %d] %s\n", step, message);
    } else {
      return String.format("[Step %d, %s] %s\n", step, entityType, message);
    }
  }

  /**
   * Callback for asynchronous writing via QueueService.
   *
   * <p>This inner class handles the actual I/O operations in a background thread,
   * processing WriteTask items from the queue.</p>
   */
  private static class InnerWriter implements QueueServiceCallback {
    private final PrintWriter writer;

    /**
     * Creates a writer that writes to the given output stream.
     *
     * @param outputStrategy The strategy to provide an output stream for writing
     * @throws RuntimeException if an error occurs while opening the output stream
     */
    public InnerWriter(OutputStreamStrategy outputStrategy) {
      try {
        OutputStream out = outputStrategy.open();
        this.writer = new PrintWriter(out, true); // auto-flush for debugging
      } catch (IOException e) {
        throw new RuntimeException("Error opening output stream", e);
      }
    }

    @Override
    public void onStart() {
      // No-op - writer is already initialized
    }

    @Override
    public void onTask(Optional<Object> taskMaybe) {
      if (taskMaybe.isEmpty()) {
        writer.flush();
        return;
      }

      @SuppressWarnings("unchecked")
      WriteTask<String> task = (WriteTask<String>) taskMaybe.get();
      String formatted = formatMessage(
          task.getData(),
          task.getStep(),
          task.getEntityType()
      );
      writer.print(formatted);
    }

    @Override
    public void onEnd() {
      writer.close();
    }
  }
}
