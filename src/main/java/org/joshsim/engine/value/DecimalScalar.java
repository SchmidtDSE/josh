/**
 * Structures describing an individual engine value.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.math.BigDecimal;


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
    assertScalarCompatible(other);
    return new DecimalScalar(getCaster(), getAsDecimal().add(other.getAsDecimal()), getUnits());
  }

  /**
   * Subtracts another DecimalScalar from this DecimalScalar.
   *
   * @param other the DecimalScalar to subtract from this one
   * @return a new DecimalScalar that is the difference between this and the other DecimalScalar
   */
  protected EngineValue fulfillSubtract(EngineValue other) {
    assertScalarCompatible(other);
    return new DecimalScalar(
        getCaster(),
        getAsDecimal().subtract(other.getAsDecimal()),
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
    assertScalarCompatible(other);
    return new DecimalScalar(
      getCaster(),
      getAsDecimal().multiply(other.getAsDecimal()),
      determineMultipliedUnits(getUnits(), other.getUnits())
    );
  }

  /**
   * Divides this DecimalScalar by another DecimalScalar.
   *
   * @param other the DecimalScalar to divide this one by
   * @return a new DecimalScalar that is the quotient of this divided by the other DecimalScalar
   */
  protected EngineValue fulfillDivide(EngineValue other) {
    assertScalarCompatible(other);
    return new DecimalScalar(
      getCaster(),
      getAsDecimal().divide(other.getAsDecimal()),
      determineDividedUnits(getUnits(), other.getUnits())
    );
  }

  /**
   * Raises this DecimalScalar to the power of another DecimalScalar.
   *
   * @param other the DecimalScalar to use as the exponent
   * @return a new DecimalScalar that is this value raised to the power of the other value
   */
  protected EngineValue fulfillRaiseToPower(EngineValue other) {
    assertScalarType(other, getLanguageType());
  
    double base = getAsDecimal().doubleValue();
    double exponent = other.getAsInt();
    if (exponent != other.getAsDecimal().doubleValue()) {
      throw new UnsupportedOperationException("Non-integer exponents are not supported");
    }
    if (!other.canBePower()) {
      throw new IllegalArgumentException("Cannot raise an int to a power with non-count units.");
    }

    return new DecimalScalar(
      getCaster(),
      BigDecimal.valueOf(Math.pow(base, exponent)),
      determineRaisedUnits(getUnits(), other.getAsInt())
    );
  }
}