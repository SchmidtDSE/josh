/**
 * A conversion that returns the input value without transformation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.ReturnCurrentCallable;

/**
 * A conversion that keeps the same units and returns the input value unchanged.
 */
public class NoopConversion implements Conversion {

  private final Units units;
  private final CompiledCallable callable;

  /**
   * Constructs a new NoopConversion with the specified units.
   *
   * @param units The units to use for both source and destination.
   */
  public NoopConversion(Units units) {
    this.units = units;
    this.callable = new ReturnCurrentCallable();
  }

  @Override
  public Units getSourceUnits() {
    return units;
  }

  @Override
  public Units getDestinationUnits() {
    return units;
  }

  @Override
  public CompiledCallable getConversionCallable() {
    return callable;
  }
}