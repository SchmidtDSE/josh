
/**
 * Data structures describing a set of conversions.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.util.Map;


/**
 * Store of available conversion operations between different units.
 */
public class Converter {

  private final Map<EngineValueTuple.UnitsTuple, Conversion> conversions;

  /**
   * Constructs a new Converter with the specified conversion mappings.
   *
   * @param conversions a map of unit tuples to their corresponding conversion operations
   */
  public Converter(Map<EngineValueTuple.UnitsTuple, Conversion> conversions) {
    this.conversions = conversions;
  }

  /**
   * Get a conversion between two unit types.
   *
   * @param oldUnits the source units
   * @param newUnits the destination units
   * @return a Conversion that can convert between the specified units
   * @throws IllegalArgumentException if no conversion exists between the units
   */
  public Conversion getConversion(Units oldUnits, Units newUnits) {
    EngineValueTuple.UnitsTuple tuple = new EngineValueTuple.UnitsTuple(oldUnits, newUnits);

    if (tuple.getAreCompatible()) {
      return new NoopConversion(newUnits);
    }

    if (!conversions.containsKey(tuple)) {
      String message = String.format(
          "No conversion exists between %s and %s.",
          oldUnits,
          newUnits
      );
      throw new IllegalArgumentException(message);
    }

    return conversions.get(tuple);
  }

}
