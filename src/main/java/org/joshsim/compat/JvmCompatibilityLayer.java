/**
 * Logic for running a Compatibility layer on JVM.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;

import java.math.BigDecimal;

/**
 * Compatibility layer providing the ability to run simulations within JVM.
 *
 * <p>Compatibility layer providing access to Compatibility objects which afford the ability to run
 * simulations in different host virtual machines, in this case within a standard JVM.</p>
 */
public class JvmCompatibilityLayer extends CacheValueFactoryCompatibilityLayer {

  /**
   * Create a compatibility layer for processes running on the JVM itself.
   *
   * @param favorBigDecimal Flag indicating if BigDecimal should be used for numbers if type not
   *     specified. True for BigDecimal and false for double.
   */
  public JvmCompatibilityLayer(boolean favorBigDecimal) {
    super(favorBigDecimal);
  }

  @Override
  public CompatibleStringJoiner createStringJoiner(String delimiter) {
    return new JvmStringJoiner(delimiter);
  }

  @Override
  public BigDecimal getTwo() {
    return BigDecimal.TWO;
  }

  @Override
  public QueueService createQueueService(QueueServiceCallback callback) {
    return new JvmQueueService(callback);
  }

  @Override
  public CompatibleLock getLock() {
    return new JvmLock();
  }

}
