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

  private final Units sourceUnits;
  private final Units destinationUnits;
  private final CompiledCallable callable;

  /**
   * Constructs a new NoopConversion with the specified units.
   *
   * @param units The units to use for both source and destination.
   */
  public NoopConversion(Units units) {
    this.sourceUnits = units;
    this.destinationUnits = units;
    this.callable = new ReturnCurrentCallable();
  }

  /**
   * Constructs a new NoopConversion with the specified units.
   *
   * @param sourceUnits The units to alias to destination.
   * @param destinationUnits The units to alias from source;
   */
  public NoopConversion(Units sourceUnits, Units destinationUnits) {
    this.sourceUnits = sourceUnits;
    this.destinationUnits = destinationUnits;
    this.callable = new ReturnCurrentCallable();
  }

  @Override
  public Units getSourceUnits() {
    return sourceUnits;
  }

  @Override
  public Units getDestinationUnits() {
    return destinationUnits;
  }

  @Override
  public CompiledCallable getConversionCallable() {
    return callable;
  }
}
