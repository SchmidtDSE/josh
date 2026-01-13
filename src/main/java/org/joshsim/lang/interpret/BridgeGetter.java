/**
 * Strategy for getting the current replicate.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import java.util.Optional;
import org.joshsim.lang.bridge.EngineBridge;
import org.joshsim.lang.io.CombinedDebugOutputFacade;


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
   * Sets the debug output facade for writing debug messages.
   *
   * <p>This allows injecting a configured debug output facade so that debug()
   * function calls can write to the appropriate destinations.</p>
   *
   * @param debugOutputFacade The debug output facade to use.
   */
  default void setDebugOutputFacade(CombinedDebugOutputFacade debugOutputFacade) {
    throw new UnsupportedOperationException(
        "setDebugOutputFacade not supported by this implementation"
    );
  }

  /**
   * Get the optional debug output facade for writing debug output.
   *
   * @return Optional containing the debug output facade if configured, empty otherwise.
   */
  default Optional<CombinedDebugOutputFacade> getDebugOutputFacade() {
    return Optional.empty();
  }

}
