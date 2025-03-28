package org.joshsim.lang.interpret.fragment;

import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.value.Conversion;
import org.joshsim.engine.value.Units;

public class IncompleteDirectConversion implements Conversion {

  private final Units destinationUnits;
  private final CompiledCallable callable;

  /**
   * Constructs a new DirectConversion with the specified units and conversion callable.
   *
   * @param destinationUnits the destination units for this conversion.
   * @param callable the callable that performs the conversion.
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
