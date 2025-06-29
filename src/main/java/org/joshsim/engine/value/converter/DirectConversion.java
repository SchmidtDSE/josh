
/**
 * Structures describing a conversion between two units through a single callable.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.converter;

import org.joshsim.engine.func.CompiledCallable;


/**
 * A conversion rule between two unit types using a single conversion callable.
 */
public class DirectConversion implements Conversion {

  private final Units sourceUnits;
  private final Units destinationUnits;
  private final CompiledCallable callable;
  private final boolean isCommunicativeSafe;

  /**
   * Constructs a new DirectConversion with the specified units and conversion callable.
   *
   * @param sourceUnits the source units for this conversion.
   * @param destinationUnits the destination units for this conversion.
   * @param callable the callable that performs the conversion.
   */
  public DirectConversion(Units sourceUnits, Units destinationUnits, CompiledCallable callable) {
    this.sourceUnits = sourceUnits;
    this.destinationUnits = destinationUnits;
    this.callable = callable;
    this.isCommunicativeSafe = false; // Default for non-wrapped conversions
  }

  /**
   * Constructs a new DirectConversion with the specified units, conversion callable,
   * and communicative safety.
   *
   * @param sourceUnits the source units for this conversion.
   * @param destinationUnits the destination units for this conversion.
   * @param callable the callable that performs the conversion.
   * @param isCommunicativeSafe whether this conversion is safe for bidirectional use.
   */
  public DirectConversion(Units sourceUnits, Units destinationUnits, CompiledCallable callable,
      boolean isCommunicativeSafe) {
    this.sourceUnits = sourceUnits;
    this.destinationUnits = destinationUnits;
    this.callable = callable;
    this.isCommunicativeSafe = isCommunicativeSafe;
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

  @Override
  public boolean isCommunicativeSafe() {
    return isCommunicativeSafe;
  }

}
