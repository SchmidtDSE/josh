/**
 * Structures to simplify writing debug messages to memory.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.debug;

import java.io.IOException;
import java.io.OutputStream;
import org.joshsim.lang.io.OutputStreamStrategy;

/**
 * DebugFacade that writes debug messages to memory (for web editor).
 *
 * <p>No background thread needed since writes are synchronous to memory.
 * This is designed for use in the web-based editor where debug output
 * should be immediately available.</p>
 */
public class MemoryDebugFacade implements DebugFacade {

  private final OutputStream outputStream;

  /**
   * Constructs a MemoryDebugFacade with the specified output strategy.
   *
   * @param outputStrategy The strategy to provide a memory output stream.
   * @throws RuntimeException if an error occurs while opening the output stream.
   */
  public MemoryDebugFacade(OutputStreamStrategy outputStrategy) {
    try {
      this.outputStream = outputStrategy.open();
    } catch (IOException e) {
      throw new RuntimeException("Error opening memory debug stream", e);
    }
  }

  @Override
  public void start() {
    // No-op for memory - writes are synchronous
  }

  @Override
  public void join() {
    // No-op for memory - writes are synchronous
  }

  @Override
  public void write(String message, long step, String entityType, int replicateNumber) {
    String formatted = String.format("[Step %d, %s] %s\n", step, entityType, message);
    try {
      outputStream.write(formatted.getBytes());
      outputStream.flush();
    } catch (IOException e) {
      throw new RuntimeException("Error writing debug message to memory", e);
    }
  }

  @Override
  public void write(String message, long step, String entityType) {
    write(message, step, entityType, 0);
  }
}
