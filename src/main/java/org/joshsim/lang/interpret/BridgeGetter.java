/**
 * Strategy for getting the current replicate.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import java.util.Optional;
import org.joshsim.lang.bridge.EngineBridge;
import org.joshsim.lang.io.debug.CombinedDebugFacade;


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

  /**
   * Get the debug facade for this bridge.
   *
   * @return Optional debug facade for writing debug messages. Empty if debug output is not
   *     configured.
   */
  default Optional<CombinedDebugFacade> getDebugFacade() {
    return Optional.empty();
  }

  /**
   * Sets the debug facade to use for debug output.
   *
   * <p>This allows injecting the debug facade so that debug() function calls can produce
   * output. If not set, debug() calls will be no-ops.</p>
   *
   * @param debugFacade The debug facade to use for debug output.
   */
  default void setDebugFacade(Optional<CombinedDebugFacade> debugFacade) {
    throw new UnsupportedOperationException("setDebugFacade not supported by this implementation");
  }

}
