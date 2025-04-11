
/**
 * Implementation of an incomplete direct unit conversion.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.fragment;

import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.value.converter.Conversion;
import org.joshsim.engine.value.converter.Units;

/**
 * Represents an incomplete direct conversion between units.
 *
 * <p>This class implements the Conversion interface but represents a conversion that
 * is not yet fully defined, with only destination units and conversion callable set.</p>
 */
public class IncompleteDirectConversion implements Conversion {

  private final Units destinationUnits;
  private final CompiledCallable callable;

  /**
   * Constructs a new DirectConversion with the specified units and conversion callable.
   *
   * @param destinationUnits the destination units for this conversion
   * @param callable the callable that performs the conversion
   */
  public IncompleteDirectConversion(Units destinationUnits, CompiledCallable callable) {
    this.destinationUnits = destinationUnits;
    this.callable = callable;
  }

  @Override
  public Units getSourceUnits() {
    throw new RuntimeException("Conversion is incomplete");
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
