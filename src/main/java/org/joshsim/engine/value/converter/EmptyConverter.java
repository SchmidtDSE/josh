/**
 * Structure to indicate that no conversions are available.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.converter;

import org.joshsim.engine.value.engine.EngineValueTuple;
import org.joshsim.engine.value.engine.EngineValueTuple.UnitsTuple;

/**
 * Converter which can only convert between identical units.
 */
public class EmptyConverter implements Converter {

  @Override
  public Conversion getConversion(Units oldUnits, Units newUnits) {
    EngineValueTuple.UnitsTuple tuple = new EngineValueTuple.UnitsTuple(oldUnits, newUnits);

    if (tuple.getAreCompatible()) {
      return new NoopConversion(newUnits);
    } else {
      String message = String.format(
          "No conversion exists between %s and %s.",
          oldUnits,
          newUnits
      );
      throw new IllegalArgumentException(message);
    }
  }

}
