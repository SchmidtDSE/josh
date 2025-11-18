/**
 * Strategy for getting the current replicate.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import java.util.Optional;
import org.joshsim.lang.bridge.EngineBridge;
import org.joshsim.lang.io.CombinedTextWriter;


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
   * Get the debug writer for this bridge.
   *
   * @return Optional debug writer for writing debug messages. Empty if debug output is not
   *     configured.
   */
  default Optional<CombinedTextWriter> getDebugWriter() {
    return Optional.empty();
  }

  /**
   * Sets the debug writer to use for debug output.
   *
   * <p>This allows injecting the debug writer so that debug() function calls can produce
   * output. If not set, debug() calls will be no-ops.</p>
   *
   * @param debugWriter The debug writer to use for debug output.
   */
  default void setDebugWriter(Optional<CombinedTextWriter> debugWriter) {
    throw new UnsupportedOperationException("setDebugWriter not supported by this implementation");
  }

  /**
   * Sets the seed for random number generation.
   *
   * <p>This allows injecting a seed value for deterministic random number generation
   * in testing scenarios. If not set, random number generators will use system time.</p>
   *
   * @param seed Optional seed value. If present, all Random instances should use this seed.
   */
  default void setSeed(Optional<Long> seed) {
    throw new UnsupportedOperationException("setSeed not supported by this implementation");
  }

  /**
   * Gets the seed for random number generation.
   *
   * @return Optional seed value. Empty if no seed has been set (truly random behavior).
   */
  default Optional<Long> getSeed() {
    return Optional.empty();
  }

}
