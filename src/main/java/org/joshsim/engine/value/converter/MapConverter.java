
/**
 * Data structures describing a set of conversions.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.converter;

import java.util.Map;
import org.joshsim.engine.value.engine.EngineValueTuple;


/**
 * Store of available conversion operations between different units.
 */
public class MapConverter implements Converter {

  private final Map<EngineValueTuple.UnitsTuple, Conversion> conversions;

  /**
   * Constructs a new Converter with the specified conversion mappings.
   *
   * @param conversions a map of unit tuples to their corresponding conversion operations
   */
  public MapConverter(Map<EngineValueTuple.UnitsTuple, Conversion> conversions) {
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

    // First check if there's an explicit conversion (includes aliases via NoopConversion)
    if (conversions.containsKey(tuple)) {
      return conversions.get(tuple);
    }

    // If no explicit conversion exists, check if units are inherently compatible
    if (tuple.getAreCompatible()) {
      return new NoopConversion(newUnits);
    }

    // No conversion found
    String message = String.format(
        "No conversion exists between \"%s\" and \"%s\".",
        oldUnits,
        newUnits
    );
    System.err.println("Failed in conversion");
    for (EngineValueTuple.UnitsTuple key : conversions.keySet()) {
      System.err.println(key.getFirst() + " -> " + key.getSecond());
    }
    throw new IllegalArgumentException(message);
  }

}
