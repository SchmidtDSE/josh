
/**
 * Data structures describing a set of conversions.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.converter;



/**
 * Store of available conversion operations between different units.
 */
public interface Converter {

  /**
   * Get a conversion between two unit types.
   *
   * @param oldUnits the source units
   * @param newUnits the destination units
   * @return a Conversion that can convert between the specified units
   * @throws IllegalArgumentException if no conversion exists between the units
   */
  Conversion getConversion(Units oldUnits, Units newUnits);

}
