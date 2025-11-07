/**
 * Structures to simplify writing debug messages to stdout.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io.debug;

/**
 * DebugFacade that writes debug messages to stdout.
 *
 * <p>Synchronous writes to standard output, no background thread needed.
 * This is useful for quick debugging during development or when running
 * simulations from the command line.</p>
 */
public class StdoutDebugFacade implements DebugFacade {

  @Override
  public void start() {
    // No-op - stdout is always ready
  }

  @Override
  public void join() {
    // No-op - stdout is always ready
  }

  @Override
  public void write(String message, long step, String entityType, int replicateNumber) {
    System.out.printf("[Step %d, %s] %s%n", step, entityType, message);
  }

  @Override
  public void write(String message, long step, String entityType) {
    write(message, step, entityType, 0);
  }
}
