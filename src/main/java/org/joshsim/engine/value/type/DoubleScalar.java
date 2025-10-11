/**
 * Structures describing an individual engine double value.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.type;

import java.math.BigDecimal;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;


/**
 * Engine value which provides a single decimal value backed by a double (64bit) value.
 */
public class DoubleScalar extends Scalar {

  private final Double innerValue;

  /**
   * Constructs a new DoubleScalar with the specified value.
   *
   * @param caster the caster to use for automatic type conversion.
   * @param innerValue value the value of this scalar.
   * @param units the units of this scalar.
   */
  public DoubleScalar(EngineValueCaster caster, double innerValue, Units units) {
    super(caster, units);
    this.innerValue = innerValue;
  }

  @Override
  public BigDecimal getAsDecimal() {
    return BigDecimal.valueOf(innerValue);
  }

  @Override
  public double getAsDouble() {
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
  public LanguageType getLanguageType() {
    return LanguageType.of("decimal");
  }

  @Override
  public Comparable getInnerValue() {
    return innerValue;
  }

  @Override
  public EngineValue replaceUnits(Units newUnits) {
    return new DoubleScalar(getCaster(), getAsDouble(), newUnits);
  }

  @Override
  protected EngineValue unsafeAdd(EngineValue other) {
    assertScalarCompatible(other);
    return new DoubleScalar(getCaster(), getAsDouble() + other.getAsDouble(), getUnits());
  }

  @Override
  protected EngineValue unsafeSubtract(EngineValue other) {
    assertScalarCompatible(other);
    return new DoubleScalar(
        getCaster(),
        getAsDouble() - other.getAsDouble(),
        getUnits()
    );
  }

  @Override
  protected EngineValue unsafeMultiply(EngineValue other) {
    assertScalarCompatible(other);
    return new DoubleScalar(
        getCaster(),
        getAsDouble() * other.getAsDouble(),
        determineMultipliedUnits(getUnits(), other.getUnits())
    );
  }

  @Override
  protected EngineValue unsafeDivide(EngineValue other) {
    assertScalarCompatible(other);
    return new DoubleScalar(
        getCaster(),
        getAsDouble() / other.getAsDouble(),
        determineDividedUnits(getUnits(), other.getUnits())
    );
  }

  @Override
  protected EngineValue unsafeRaiseToPower(EngineValue other) {
    assertScalarCompatible(other);

    double base = getAsDouble();
    double exponent = other.getAsDouble();
    double remainder = other.getAsInt() - other.getAsDouble();
    boolean otherIsInteger = remainder < 1e-7;
    if (!otherIsInteger && !canBePower()) {
      throw new UnsupportedOperationException("Non-integer exponents with units are not supported");
    }
    if (!other.canBePower()) {
      throw new IllegalArgumentException("Cannot raise an int to a power with non-count units.");
    }

    return new DoubleScalar(
        getCaster(),
        Math.pow(base, exponent),
        determineRaisedUnits(getUnits(), other.getAsInt())
    );
  }
}
