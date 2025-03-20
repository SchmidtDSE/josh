/**
 * Structures describing an individual engine value.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.lang.UnsupportedOperationException;
import java.math.BigDecimal;


/**
 * Engine value which only has a single discrete value.
 */
public class IntScalar extends Scalar {

  private final Long innerValue;
  
  /**
   * Constructs an IntScalar instance with specified caster, value, and units.
   *
   * @param newCaster the EngineValueCaster used for casting
   * @param newInnerValue the initial integer value of this IntScalar
   * @param newUnits the units associated with this IntScalar
   */
  public IntScalar(EngineValueCaster newCaster, long newInnerValue, String newUnits) {
    super(newCaster, newUnits);
    innerValue = newInnerValue;
  }

  @Override
  public BigDecimal getAsDecimal() {
    return new BigDecimal(innerValue);
  }

  @Override
  public boolean getAsBoolean() {
    if (innerValue == 0) {
      return false;
    } else if (innerValue == 1) {
      return true;
    }
    throw new UnsupportedOperationException("Cannot convert an int to boolean.");
  }

  @Override
  public String getAsString() {
    return innerValue.toString();
  }

  @Override
  public long getAsInt() {
    return innerValue;
  }

  @Override
  public String getLanguageType() {
    return "int";
  }

  @Override
  public Comparable<Long> getInnerValue() {
    return innerValue;
  }

  
  /**
   * Compares this IntScalar instance with another for equality.
   *
   * @param other the object to compare to
   * @return true if the specified object is equal to this IntScalar; false otherwise.
   */
  public EngineValue add(IntScalar other) {
    validateCommonUnits(other);
    return new IntScalar(getCaster(), getAsInt() + other.getAsInt(), getUnits());
  }

  
  /**
   * Checks equality of this IntScalar instance with another object.
   *
   * @param other the object to compare to
   * @return true if the specified object is equal to this IntScalar; false otherwise.
   */
  public EngineValue subtract(IntScalar other) {
    validateCommonUnits(other);
    return new IntScalar(getCaster(), getAsInt() - other.getAsInt(), getUnits());
  }

  
  /**
   * Multiplies this IntScalar instance with another IntScalar.
   *
   * @param other the IntScalar to multiply with
   * @return a new IntScalar that is the product of this and the other IntScalar
   */
  public EngineValue multiply(IntScalar other) {
    return new IntScalar(
        getCaster(),
        getAsInt() * other.getAsInt(),
        determineMultipliedUnits(getUnits(), other.getUnits())
    );
  }
  
  /**
   * Divides this IntScalar by another IntScalar.
   *
   * @param other the IntScalar to divide this one by
   * @return a new IntScalar that is the quotient of this divided by the other IntScalar
   * @throws ArithmeticException if division by zero is attempted
   */
  public EngineValue divide(IntScalar other) {
    return new IntScalar(
        getCaster(),
        getAsInt() / other.getAsInt(),
        determineDividedUnits(getUnits(), other.getUnits())
    );
  }
  
  /**
   * Raises this IntScalar to the power of another IntScalar.
   *
   * @param other the IntScalar to use as the exponent
   * @return a new DecimalScalar that is this value raised to the power of the other value
   */
  public EngineValue raiseToPower(IntScalar other) {
    if (other.getUnits() != "") {
      throw new IllegalArgumentException("Cannot raise an int to a power with units.");
    }
    return new DecimalScalar(
        getCaster(),
        new BigDecimal(Math.pow(getAsInt(), other.getAsInt())),
        determineRaisedUnits(getUnits(), other.getAsInt())
    );
  }
}
