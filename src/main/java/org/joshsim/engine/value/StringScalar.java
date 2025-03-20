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

  /**
   * Concatenates this StringScalar with another StringScalar.
   *
   * @param other the StringScalar to concatenate with this one
   * @return a new StringScalar that is the concatenation of this and the other StringScalar
   */
  protected EngineValue fulfillAdd(EngineValue other) {
    StringScalar otherScalar = (StringScalar) other;
    return new StringScalar(getCaster(), getAsString() + otherScalar.getAsString(), getUnits());
  }

  /**
   * String subtraction operation is not supported.
   *
   * @param other the StringScalar that would be subtracted
   * @throws UnsupportedOperationException as strings cannot be subtracted
   */
  protected EngineValue fulfillSubtract(EngineValue other) {
    throw new UnsupportedOperationException("Cannot subtract strings.");
  }

  /**
   * String multiplication operation is not supported.
   *
   * @param other the StringScalar that would be multiplied
   * @throws UnsupportedOperationException as strings cannot be multiplied
   */
  protected EngineValue fulfillMultiply(EngineValue other) {
    throw new UnsupportedOperationException("Cannot multiply strings.");
  }

  /**
   * String division operation is not supported.
   *
   * @param other the StringScalar that would be the divisor
   * @throws UnsupportedOperationException as strings cannot be divided
   */
  protected EngineValue fulfillDivide(EngineValue other) {
    throw new UnsupportedOperationException("Cannot divide strings.");
  }

  /**
   * String exponentiation operation is not supported.
   *
   * @param other the StringScalar that would be the exponent
   * @throws UnsupportedOperationException as strings cannot be raised to powers
   */
  protected EngineValue fulfillRaiseToPower(EngineValue other) {
    throw new UnsupportedOperationException("Cannot raise strings to powers.");
  }
}