
/**
 * Structures describing a conversion between two units through two conversions.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.converter;

import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.func.SingleValueScope;
import org.joshsim.engine.value.type.EngineValue;


/**
 * A conversion rule between two unit types using two inner conversions.
 */
public class TransitiveConversion implements Conversion {

  private final Conversion first;
  private final Conversion second;
  private final CompiledCallable callable;

  /**
   * Constructs a new DirectConversion with the specified units and conversion callable.
   *
   * @param first Conversion to apply first.
   * @param second Conversion to apply second.
   */
  public TransitiveConversion(Conversion first, Conversion second) {
    this.first = first;
    this.second = second;

    callable = new CompiledCallable() {
      @Override
      public EngineValue evaluate(Scope scope) {
        EngineValue firstResult = first.getConversionCallable().evaluate(scope);
        Scope transitiveScope = new SingleValueScope(firstResult);
        return second.getConversionCallable().evaluate(transitiveScope);
      }
    };

    boolean innerUnitsSame = first.getDestinationUnits().equals(second.getSourceUnits());
    if (!innerUnitsSame) {
      String message = String.format(
          "Inner units mismatch in transitive conversion: %s, %s",
          first.getDestinationUnits(),
          second.getSourceUnits()
      );
      throw new IllegalArgumentException(message);
    }
  }

  @Override
  public Units getSourceUnits() {
    return first.getSourceUnits();
  }

  @Override
  public Units getDestinationUnits() {
    return second.getDestinationUnits();
  }

  @Override
  public CompiledCallable getConversionCallable() {
    return callable;
  }


}
