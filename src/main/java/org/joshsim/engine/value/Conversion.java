/**
 * Structures describing callabales which can be used to convert between units.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import org.joshsim.engine.func.CompiledCallable;


/**
 * A conversion rule between two unit types.
 *
 * <p>A conversion rule between two unit types that may be composed with other rules to create
 * emergent transitive conversions. Specifically, this uses CompiledCallables to move between
 * EngineValues of different units.
 * </p>
 */
public class Conversion {

  private final Units sourceUnits;
  private final Units destinationUnits;
  private final CompiledCallable callable;

  /**
   * Create a new conversion.
   *
   * @param sourceUnits which is what is expected of the input EngineValue.
   * @param destinationUnits which is what is expected of the output EngineValue.
   * @param callable which implements this conversion.
   */
  public Conversion(Units sourceUnits, Units destinationUnits, CompiledCallable callable) {
    this.sourceUnits = sourceUnits;
    this.destinationUnits = destinationUnits;
    this.callable = callable;
  }

  /**
   * Get the source units for this conversion.
   *
   * @return the source units as a string
   */
  public Units getSourceUnits() {
    return sourceUnits;
  }

  /**
   * Get the destination units for this conversion.
   *
   * @return the destination units as a string
   */
  public Units getDestinationUnits() {
    return destinationUnits;
  }

  /**
   * Get the callable that performs the actual conversion.
   *
   * @return a compiled callable that performs the conversion
   */
  public CompiledCallable getConversionCallable() {
    return callable;
  }
}
