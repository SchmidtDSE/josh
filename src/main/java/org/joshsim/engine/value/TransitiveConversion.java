
/**
 * Structures describing a conversion between two units through two conversions.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import org.joshsim.engine.func.CompiledCallable;


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
   * @param first Conversion to apply second.
   */
  public DirectConversion(Conversion first, Conversion second) {
    this.first = first;
    this.second = second;

    callable = return new CompiledCallable() {
      @Override
      public EngineValue evaluate(Scope scope) {
        Scope firstScope = first.evaluate(scope);
        Scope scondScope = 
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
  public String getSourceUnits() {
    return first.getSourceUnits();
  }

  @Override
  public String getDestinationUnits() {
    return second.getDestinationUnits();
  }

  @Override
  public CompiledCallable getConversionCallable() {
    
  }

}
