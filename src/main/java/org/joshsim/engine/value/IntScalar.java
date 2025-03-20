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


  @Override
  protected EngineValue fulfillAdd(Scalar other) {
    validateCommonUnits(other);
    return new IntScalar(getCaster(), getAsInt() + ((IntScalar)other).getAsInt(), getUnits());
  }

  @Override
  protected EngineValue fulfillSubtract(Scalar other) {
    validateCommonUnits(other);
    return new IntScalar(getCaster(), getAsInt() - ((IntScalar)other).getAsInt(), getUnits());
  }

  @Override
  protected EngineValue fulfillMultiply(Scalar other) {
    return new IntScalar(
        getCaster(),
        getAsInt() * ((IntScalar)other).getAsInt(),
        determineMultipliedUnits(getUnits(), other.getUnits())
    );
  }

  @Override
  protected EngineValue fulfillDivide(Scalar other) {
    if (((IntScalar)other).getAsInt() == 0) {
      throw new ArithmeticException("Division by zero");
    }
    return new IntScalar(
        getCaster(),
        getAsInt() / ((IntScalar)other).getAsInt(),
        determineDividedUnits(getUnits(), other.getUnits())
    );
  }

  @Override
  protected EngineValue fulfillRaiseToPower(Scalar other) {
    if (other.getUnits() != "") {
      throw new IllegalArgumentException("Cannot raise an int to a power with units.");
    }
    return new DecimalScalar(
        getCaster(),
        new BigDecimal(Math.pow(getAsInt(), ((IntScalar)other).getAsInt())),
        determineRaisedUnits(getUnits(), ((IntScalar)other).getAsInt())
    );
  }
}