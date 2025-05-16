/**
 * Logic to help build compatability layers that use a shared value factory.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.compat;


import org.joshsim.engine.value.engine.EngineValueFactory;

/**
 * Create a new compatability layer that caches a value factory.
 *
 * <p>Create a new compatability layer which caches a value factory that is to be used across all
 * operations.</p>
 */
public abstract class CacheValueFactoryCompatibilityLayer implements CompatibilityLayer {

  private final EngineValueFactory valueFactory;

  /**
   * Constructs a new layer with the specified configuration.
   *
   * @param favorBigDecimal Indicates whether decimal values produced by the internal
   *     EngineValueFactory should favor BigDecimal over double when the type is not specified. If
   *     true, BigDecimal is favored. If false, double is favored.
   */
  public CacheValueFactoryCompatibilityLayer(boolean favorBigDecimal) {
    this.valueFactory = new EngineValueFactory(favorBigDecimal);
  }

  @Override
  public EngineValueFactory getEngineValueFactory() {
    return valueFactory;
  }

}
