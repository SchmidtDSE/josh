/**
 * Structures describing an individual engine value.
 *
 * @license BSD-3-Clause
 */
package org.joshsim.engine.value;

import java.math.BigDecimal;

/**
 * Engine value which only has a single discrete value.
 */
public class IntScalar extends Scalar {

  private final Long innerValue;

  /**
   * Constructs an IntScalar instance with specified caster, value, and units.
   *
   * @param caster the EngineValueCaster used for casting
   * @param innerValue the initial integer value of this IntScalar
   * @param units the units associated with this IntScalar
   */
  public IntScalar(EngineValueCaster caster, long innerValue, Units units) {
    super(caster, units);
    this.innerValue = innerValue;
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
  public LanguageType getLanguageType() {
    return new LanguageType("int");
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
  protected EngineValue unsafeSubtract(EngineValue other) {
    assertScalarCompatible(other);
    return new IntScalar(getCaster(), getAsInt() - other.getAsInt(), getUnits());
  }

  @Override
  protected EngineValue unsafeMultiply(EngineValue other) {
    assertScalarCompatible(other);
    return new IntScalar(
        getCaster(),
        getAsInt() * other.getAsInt(),
        determineMultipliedUnits(getUnits(), other.getUnits()));
  }

  @Override
  protected EngineValue unsafeDivide(EngineValue other) {
    assertScalarCompatible(other);
    return new IntScalar(
        getCaster(),
        getAsInt() / other.getAsInt(),
        determineDividedUnits(getUnits(), other.getUnits()));
  }

  @Override
  protected EngineValue unsafeRaiseToPower(EngineValue other) {
    assertScalarCompatible(other);

    if (!other.canBePower()) {
      throw new IllegalArgumentException("Cannot raise an int to a power with non-count units.");
    }

    return new DecimalScalar(
        getCaster(),
        new BigDecimal(Math.pow(getAsInt(), other.getAsInt())),
        determineRaisedUnits(getUnits(), other.getAsInt()));
  }
}
