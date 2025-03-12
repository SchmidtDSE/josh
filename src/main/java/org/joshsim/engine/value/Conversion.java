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
public interface Conversion {
  /**
   * Get the source units for this conversion.
   *
   * @return the source units as a string
   */
  String getSourceUnits();
  
  /**
   * Get the destination units for this conversion.
   *
   * @return the destination units as a string
   */
  String getDestinationUnits();
  
  /**
   * Get the callable that performs the actual conversion.
   *
   * @return a compiled callable that performs the conversion
   */
  CompiledCallable getConversionCallable();
}