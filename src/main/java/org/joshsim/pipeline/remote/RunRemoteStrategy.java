/**
 * Strategy interface for different remote execution modes.
 *
 * <p>This interface defines the contract for executing Josh simulations remotely.
 * Different strategies can handle execution in different ways, such as offloading
 * to a remote leader or managing replicate execution locally.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.remote;

import java.io.IOException;

/**
 * Strategy interface for remote simulation execution.
 *
 * <p>Implementations of this interface handle the execution of remote simulations
 * in different ways. The strategy pattern allows the RunRemoteCommand to delegate
 * execution logic while maintaining a consistent interface.</p>
 */
public interface RunRemoteStrategy {

  /**
   * Executes a remote simulation using this strategy.
   *
   * <p>This method handles the entire execution process from sending requests
   * to processing responses and persisting results. The specific execution
   * approach depends on the strategy implementation.</p>
   *
   * @param context The execution context containing all necessary parameters
   * @throws IOException if network communication fails
   * @throws InterruptedException if the operation is interrupted
   * @throws RuntimeException if execution fails for other reasons
   */
  void execute(RunRemoteContext context) throws IOException, InterruptedException;
}
