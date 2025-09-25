/**
 * Strategy for getting the current replicate.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import org.joshsim.lang.bridge.EngineBridge;


/**
 * Lazy evaluator which returns the current replicate in the current thread.
 */
public interface BridgeGetter {

  /**
   * Get the current replicate executing.
   *
   * @return The replicate which is currently executing in this thread.
   */
  EngineBridge get();

  /**
   * Sets the bridge to use instead of building one.
   *
   * <p>This allows injecting a specific bridge instance so that external data
   * requests use the same bridge instance that gets updated during simulation steps.</p>
   *
   * @param bridge The bridge to use for all operations.
   */
  default void setBridge(EngineBridge bridge) {
    throw new UnsupportedOperationException("setBridge not supported by this implementation");
  }

}
