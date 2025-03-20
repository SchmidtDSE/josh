
/**
 * Structures describing an individual engine value.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


/**
 * Engine value which only has a single discrete decimal value.
 */
public class DecimalScalar extends Scalar {

  private final BigDecimal innerValue;

  /**
  * Constructs a new DecimalScalar with the specified value.
  *
  * @param newCaster the caster to use for automatic type conversion.
  * @param newInnerValue value the value of this scalar.
  * @param newUnits the units of this scalar.
  */
  public DecimalScalar(EngineValueCaster newCaster, BigDecimal newInnerValue, String newUnits) {
    super(newCaster, newUnits);
    innerValue = newInnerValue;
  }

  @Override
  public BigDecimal getAsDecimal() {
    return innerValue;
  }

  @Override
  public boolean getAsBoolean() {
    throw new UnsupportedOperationException("Cannot convert decimal to boolean");
  }

  @Override
  public String getAsString() {
    return innerValue.toString();
  }

  @Override
  public long getAsInt() {
    return innerValue.intValue();
  }

  @Override
  public String getLanguageType() {
    return "decimal";
  }

  @Override
  public Comparable getInnerValue() {
    return innerValue;
  }

  @Override
  protected EngineValue fulfillAdd(Scalar other) {
    validateCommonUnits(other);
    return new DecimalScalar(getCaster(), getAsDecimal().add(((DecimalScalar)other).getAsDecimal()), getUnits());
  }

  @Override
  protected EngineValue fulfillSubtract(Scalar other) {
    validateCommonUnits(other);
    return new DecimalScalar(
        getCaster(),
        getAsDecimal().subtract(((DecimalScalar)other).getAsDecimal()),
        getUnits()
    );
  }

  @Override
  protected EngineValue fulfillMultiply(Scalar other) {
    return new DecimalScalar(
      getCaster(),
      getAsDecimal().multiply(((DecimalScalar)other).getAsDecimal()),
      determineMultipliedUnits(getUnits(), other.getUnits())
    );
  }

  @Override
  protected EngineValue fulfillDivide(Scalar other) {
    return new DecimalScalar(
      getCaster(),
      getAsDecimal().divide(((DecimalScalar)other).getAsDecimal()),
      determineDividedUnits(getUnits(), other.getUnits())
    );
  }

  @Override
  protected EngineValue fulfillRaiseToPower(Scalar other) {
    double base = getAsDecimal().doubleValue();
    double exponent = other.getAsInt();
    if (exponent != other.getAsDecimal().doubleValue()) {
      throw new UnsupportedOperationException("Non-integer exponents are not supported");
    }
    if (other.getUnits() != "") {
      throw new IllegalArgumentException("Cannot raise an int to a power with units.");
    }
    return new DecimalScalar(
      getCaster(),
      BigDecimal.valueOf(Math.pow(base, exponent)),
      determineRaisedUnits(getUnits(), other.getAsInt())
    );
  }
}
