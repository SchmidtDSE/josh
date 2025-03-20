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

  /**
   * Adds this DecimalScalar with another DecimalScalar.
   *
   * @param other the DecimalScalar to add to this one
   * @return a new DecimalScalar that is the sum of this and the other DecimalScalar
   */
  protected EngineValue fulfillAdd(EngineValue other) {
    DecimalScalar otherScalar = (DecimalScalar) other;
    validateCommonUnits(otherScalar);
    return new DecimalScalar(getCaster(), getAsDecimal().add(otherScalar.getAsDecimal()), getUnits());
  }

  /**
   * Subtracts another DecimalScalar from this DecimalScalar.
   *
   * @param other the DecimalScalar to subtract from this one
   * @return a new DecimalScalar that is the difference between this and the other DecimalScalar
   */
  protected EngineValue fulfillSubtract(EngineValue other) {
    DecimalScalar otherScalar = (DecimalScalar) other;
    validateCommonUnits(otherScalar);
    return new DecimalScalar(
        getCaster(),
        getAsDecimal().subtract(otherScalar.getAsDecimal()),
        getUnits()
    );
  }

  /**
   * Multiplies this DecimalScalar with another DecimalScalar.
   *
   * @param other the DecimalScalar to multiply with this one
   * @return a new DecimalScalar that is the product of this and the other DecimalScalar
   */
  protected EngineValue fulfillMultiply(EngineValue other) {
    DecimalScalar otherScalar = (DecimalScalar) other;
    return new DecimalScalar(
      getCaster(),
      getAsDecimal().multiply(otherScalar.getAsDecimal()),
      determineMultipliedUnits(getUnits(), otherScalar.getUnits())
    );
  }

  /**
   * Divides this DecimalScalar by another DecimalScalar.
   *
   * @param other the DecimalScalar to divide this one by
   * @return a new DecimalScalar that is the quotient of this divided by the other DecimalScalar
   */
  protected EngineValue fulfillDivide(EngineValue other) {
    DecimalScalar otherScalar = (DecimalScalar) other;
    return new DecimalScalar(
      getCaster(),
      getAsDecimal().divide(otherScalar.getAsDecimal()),
      determineDividedUnits(getUnits(), otherScalar.getUnits())
    );
  }

  /**
   * Raises this DecimalScalar to the power of another DecimalScalar.
   *
   * @param other the DecimalScalar to use as the exponent
   * @return a new DecimalScalar that is this value raised to the power of the other value
   */
  protected EngineValue fulfillRaiseToPower(EngineValue other) {
    DecimalScalar otherScalar = (DecimalScalar) other;
    double base = getAsDecimal().doubleValue();
    double exponent = otherScalar.getAsInt();
    if (exponent != otherScalar.getAsDecimal().doubleValue()) {
      throw new UnsupportedOperationException("Non-integer exponents are not supported");
    }
    if (otherScalar.getUnits() != "") {
      throw new IllegalArgumentException("Cannot raise an int to a power with units.");
    }
    return new DecimalScalar(
      getCaster(),
      BigDecimal.valueOf(Math.pow(base, exponent)),
      determineRaisedUnits(getUnits(), otherScalar.getAsInt())
    );
  }
}