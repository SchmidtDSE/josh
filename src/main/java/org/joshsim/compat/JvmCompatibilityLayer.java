/**
 * Logic for running a compatability layer on JVM.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;

import java.math.BigDecimal;

/**
 * Compatability layer providing the ability to run simulations within JVM.
 *
 * <p>Compatability layer providing access to compatability objects which afford the ability to run
 * simulations in different host virtual machines, in this case within a standard JVM.</p>
 */
public class JvmCompatibilityLayer implements CompatabilityLayer {

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
