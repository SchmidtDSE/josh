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

}
