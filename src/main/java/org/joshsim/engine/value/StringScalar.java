/**
 * Structures describing an individual engine value.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.lang.UnsupportedOperationException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


/**
 * Engine value which only has a single discrete string value.
 */
public class StringScalar extends Scalar {

  private final String innerValue;

  /**
  * Constructs a new DecimalScalar with the specified value.
  *
  * @param newCaster the caster to use for automatic type conversion.
  * @param newInnerValue the value of this StringScalar.
  * @param newUnits the units of this StringScalar.
  */
  public StringScalar(EngineValueCaster newCaster, String newInnerValue, String newUnits) {
    super(newCaster, newUnits);
    innerValue = newInnerValue;
  }

  @Override
  public BigDecimal getAsDecimal() {
    return new BigDecimal(Double.parseDouble(innerValue));
  }

  @Override
  public boolean getAsBoolean() {
    if (innerValue.equals("true")) {
      return true;
    } else if (innerValue.equals("false")) {
      return false;
    }
    throw new UnsupportedOperationException("Cannot convert a string to boolean.");
  }

  @Override
  public String getAsString() {
    return innerValue;
  }

  @Override
  public long getAsInt() {
    return Long.parseLong(innerValue);
  }

  @Override
  public String getLanguageType() {
    return "string";
  }

  @Override
  public Comparable getInnerValue() {
    return innerValue;
  }

  @Override
  protected EngineValue fulfillAdd(Scalar other) {
    validateCommonUnits(other);
    return new StringScalar(getCaster(), getAsString() + ((StringScalar)other).getAsString(), getUnits());
  }

  @Override
  protected EngineValue fulfillSubtract(Scalar other) {
    throw new UnsupportedOperationException("Cannot subtract strings.");
  }

  @Override
  protected EngineValue fulfillMultiply(Scalar other) {
    throw new UnsupportedOperationException("Cannot multiply strings.");
  }

  @Override
  protected EngineValue fulfillDivide(Scalar other) {
    throw new UnsupportedOperationException("Cannot divide strings.");
  }

  @Override
  protected EngineValue fulfillRaiseToPower(Scalar other) {
    throw new UnsupportedOperationException("Cannot raise strings to powers.");
  }
}