/**
 * Logic for running a Compatibility layer on WASM.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;

import java.math.BigDecimal;


/**
 * Compatibility layer providing the ability to run simulations within WebAssembly.
 *
 * <p>Compatibility layer providing access to Compatibility objects which afford the ability to run
 * simulations in different host virutal machines, in this case within WebAssembly.</p>
 */
public class EmulatedCompatibilityLayer implements CompatibilityLayer {

  private static final BigDecimal TWO = new BigDecimal("2");

  @Override
  public CompatibleStringJoiner createStringJoiner(String delimiter) {
    return new EmulatedStringJoiner(delimiter);
  }

  @Override
  public BigDecimal getTwo() {
    return TWO;
  }

  @Override
  public QueueService createQueueService(QueueServiceCallback callback) {
    return new EmulatedQueueService(callback);
  }

  @Override
  public CompatibleLock getLock() {
    return new EmulatedLock();
  }

}
