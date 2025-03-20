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
  * @param caster the caster to use for automatic type conversion.
  * @param innerValue the value of this StringScalar.
  * @param units the units of this StringScalar.
  */
  public StringScalar(EngineValueCaster caster, String innerValue, String units) {
    super(caster, units);
    this.innerValue = innerValue;
  }

  @Override
  public BigDecimal getAsDecimal() {
    return new BigDecimal(Double.parseDouble(innerValue));
  }

  @Override
  public boolean getAsBoolean() {
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
  public EngineValue add(StringScalar other) {
    return new StringScalar(getCaster(), getAsString() + other.getAsString(), getUnits());
  }
  
  /**
   * String subtraction operation is not supported.
   *
   * @param other the StringScalar that would be subtracted
   * @throws UnsupportedOperationException as strings cannot be subtracted
   */
  public EngineValue subtract(StringScalar other) {
    throw new UnsupportedOperationException("Cannot subtract strings.");
  }
  
  /**
   * String multiplication operation is not supported.
   *
   * @param other the StringScalar that would be multiplied
   * @throws UnsupportedOperationException as strings cannot be multiplied
   */
  public EngineValue multiply(StringScalar other) {
    throw new UnsupportedOperationException("Cannot multiply strings.");
  }
  
  /**
   * String division operation is not supported.
   *
   * @param other the StringScalar that would be the divisor
   * @throws UnsupportedOperationException as strings cannot be divided
   */
  public EngineValue divide(StringScalar other) {
    throw new UnsupportedOperationException("Cannot divide strings.");
  }
  
  /**
   * String exponentiation operation is not supported.
   *
   * @param other the StringScalar that would be the exponent
   * @throws UnsupportedOperationException as strings cannot be raised to powers
   */
  public EngineValue raiseToPower(StringScalar other) {
    throw new UnsupportedOperationException("Cannot raise strings to powers.");
  }

  @Override
  public Distribution getAsDistribution() {
    EngineValueFactory factory = new EngineValueFactory(getCaster());

    List<String> values = new ArrayList<>(1);
    values.add(innerValue);

    return factory.buildDistribution(values, getUnits());
  }
}
