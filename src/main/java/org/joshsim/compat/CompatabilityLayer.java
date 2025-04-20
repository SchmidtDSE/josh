
package org.joshsim.compat;

import java.math.BigDecimal;

/**
 * The CompatabilityLayer interface provides platform-independent implementations
 * of common utilities and services. This allows the simulation engine to run
 * consistently across different environments (JVM, Web, etc.) by abstracting
 * platform-specific implementations.
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
