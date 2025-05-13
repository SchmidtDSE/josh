package org.joshsim.engine.value.converter;


import org.joshsim.engine.func.CompiledCallable;

/**
 * Conversion which is the same as another conversion but with units flipped.
 *
 * <p>Conversion which acts as a decorator around another conversion in which the conversion
 * operates with the same callable but with units flipped.</p>
 */
public class InverseConversion implements Conversion{

  private final Conversion inner;

  /**
   * Constructs an InverseConversion that flips the units of the provided conversion.
   *
   * @param inner the conversion instance to be wrapped and inverted
   */
  public InverseConversion(Conversion inner) {
    this.inner = inner;
  }

  @Override
  public Units getSourceUnits() {
    return inner.getDestinationUnits();
  }

  @Override
  public Units getDestinationUnits() {
    return inner.getSourceUnits();
  }

  @Override
  public CompiledCallable getConversionCallable() {
    return inner.getConversionCallable();
  }

  @Override
  public boolean isCommunicativeSafe() {
    return inner.isCommunicativeSafe();
  }
}
