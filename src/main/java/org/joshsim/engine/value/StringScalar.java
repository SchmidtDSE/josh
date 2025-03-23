/**
 * Structures describing an individual engine value.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.math.BigDecimal;


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
  public StringScalar(EngineValueCaster caster, String innerValue, Units units) {
    super(caster, units);
    this.innerValue = innerValue;
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
  public LanguageType getLanguageType() {
    return new LanguageType("string");
  }

  @Override
  public Comparable getInnerValue() {
    return innerValue;
  }

  @Override
  protected EngineValue unsafeAdd(EngineValue other) {
    assertScalarCompatible(other);
    return new StringScalar(getCaster(), getAsString() + other.getAsString(), getUnits());
  }

}
