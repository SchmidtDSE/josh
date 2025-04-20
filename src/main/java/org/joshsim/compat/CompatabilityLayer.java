/**
 * Logic for a cross-VM compatability layer.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;

import java.math.BigDecimal;

/**
 * Layer which offers access to platform-specific functionality (JVM vs WASM).
 *
 * <p>Layer which allows abstraction over different runtime virtual machines, specifically offering
 * compatability objects which work on either a plain old JVM or in WebAssembly.</p>
 */
public interface CompatabilityLayer {

  /**
   * Creates a platform-specific string joiner implementation.
   *
   * @param delimiter The string to use as a delimiter between joined elements
   * @return A new CompatibleStringJoiner instance
   */
  CompatibleStringJoiner createStringJoiner(String delimiter);

  /**
   * Returns the BigDecimal value of 2, handling platform-specific precision.
   *
   * @return BigDecimal representation of 2
   */
  BigDecimal getTwo();

  /**
   * Creates a platform-specific queue service implementation.
   *
   * @param callback The callback to handle queue events
   * @return A new QueueService instance
   */
  QueueService createQueueService(QueueServiceCallback callback);

  /**
   * Returns a platform-specific lock implementation for thread synchronization.
   *
   * @return A new CompatibleLock instance
   */
  CompatibleLock getLock();

}
