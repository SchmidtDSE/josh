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
  protected EngineValue unsafeAdd(EngineValue other) {
    assertScalarCompatible(other);
    return new IntScalar(getCaster(), getAsInt() + other.getAsInt(), getUnits());
  }


  @Override
  protected EngineValue fulfillSubtract(EngineValue other) {
    assertScalarCompatible(other);
    return new IntScalar(getCaster(), getAsInt() - other.getAsInt(), getUnits());
  }


  @Override
  protected EngineValue fulfillMultiply(EngineValue other) {
    assertScalarCompatible(other);
    return new IntScalar(
        getCaster(),
        getAsInt() * other.getAsInt(),
        determineMultipliedUnits(getUnits(), other.getUnits())
    );
  }

  @Override
  protected EngineValue fulfillDivide(EngineValue other) {
    assertScalarCompatible(other);
    return new IntScalar(
        getCaster(),
        getAsInt() / other.getAsInt(),
        determineDividedUnits(getUnits(), other.getUnits())
    );
  }

  @Override
  protected EngineValue fulfillRaiseToPower(EngineValue other) {
    assertScalarCompatible(other);

    if (!other.canBePower()) {
      throw new IllegalArgumentException("Cannot raise an int to a power with non-count units.");
    }

    return new DecimalScalar(
        getCaster(),
        new BigDecimal(Math.pow(getAsInt(), other.getAsInt())),
        determineRaisedUnits(getUnits(), other.getAsInt())
    );
  }
}