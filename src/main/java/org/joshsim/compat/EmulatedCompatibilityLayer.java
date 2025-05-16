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
public class EmulatedCompatibilityLayer extends CacheValueFactoryCompatibilityLayer {

  private static final BigDecimal TWO = new BigDecimal("2");

  /**
   * Create a new compatibility layer for emulated (like WASM or JS) environments.
   *
   * @param favorBigDecimal Flag indicating if BigDecimal should be used for numbers if type not
   *     specified. True for BigDecimal and false for double.
   */
  public EmulatedCompatibilityLayer(boolean favorBigDecimal) {
    super(favorBigDecimal);
  }

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
