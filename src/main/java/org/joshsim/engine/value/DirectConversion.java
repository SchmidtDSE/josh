
/**
 * Structures describing a conversion between two units through a single callable.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import org.joshsim.engine.func.CompiledCallable;


/**
 * A conversion rule between two unit types using a single conversion callable.
 */
public class DirectConversion implements Conversion {

  private final String sourceUnits;
  private final String destinationUnits;
  private final CompiledCallable callable;

  /**
   * Constructs a new DirectConversion with the specified units and conversion callable.
   *
   * @param sourceUnits the source units for this conversion.
   * @param destinationUnits the destination units for this conversion.
   * @param conversionCallable the callable that performs the conversion.
   */
  public DirectConversion(String sourceUnits, String destinationUnits, CompiledCallable callable) {
    this.sourceUnits = sourceUnits;
    this.destinationUnits = destinationUnits;
    this.callable = callable;
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
