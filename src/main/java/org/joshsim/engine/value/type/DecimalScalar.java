/**
 * Structures describing an individual engine decimal value.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.type;

import java.math.BigDecimal;
import java.math.MathContext;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;


/**
 * Engine value which provides a single decimal value backed by a BigDecimal.
 */
public class DecimalScalar extends Scalar {

  private final BigDecimal innerValue;

  /**
  * Constructs a new DecimalScalar with the specified value.
  *
  * @param caster the caster to use for automatic type conversion.
  * @param innerValue value the value of this scalar.
  * @param units the units of this scalar.
  */
  public DecimalScalar(EngineValueCaster caster, BigDecimal innerValue, Units units) {
    super(caster, units);
    this.innerValue = innerValue;
  }

  @Override
  public BigDecimal getAsDecimal() {
    return innerValue;
  }

  @Override
  public double getAsDouble() {
    return innerValue.doubleValue();
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
    return new DecimalScalar(getCaster(), getAsDecimal(), newUnits);
  }

  @Override
  protected EngineValue unsafeAdd(EngineValue other) {
    assertScalarCompatible(other);
    return new DecimalScalar(getCaster(), getAsDecimal().add(other.getAsDecimal()), getUnits());
  }

  @Override
  protected EngineValue unsafeSubtract(EngineValue other) {
    assertScalarCompatible(other);
    return new DecimalScalar(
        getCaster(),
        getAsDecimal().subtract(other.getAsDecimal()),
        getUnits()
    );
  }

  @Override
  protected EngineValue unsafeMultiply(EngineValue other) {
    assertScalarCompatible(other);
    return new DecimalScalar(
      getCaster(),
      getAsDecimal().multiply(other.getAsDecimal()),
      determineMultipliedUnits(getUnits(), other.getUnits())
    );
  }

  @Override
  protected EngineValue unsafeDivide(EngineValue other) {
    assertScalarCompatible(other);
    return new DecimalScalar(
      getCaster(),
      getAsDecimal().divide(other.getAsDecimal(), MathContext.DECIMAL128),
      determineDividedUnits(getUnits(), other.getUnits())
    );
  }

  @Override
  protected EngineValue unsafeRaiseToPower(EngineValue other) {
    assertScalarCompatible(other);

    double base = getAsDouble();
    double exponent = other.getAsDouble();
    double remainder = Math.abs(other.getAsInt() - other.getAsDouble());
    boolean otherIsInteger = remainder < 1e-7;
    if (!otherIsInteger && !canBePower()) {
      throw new UnsupportedOperationException("Non-integer exponents with units are not supported");
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

  /**
   * Epsilon value for floating-point equality comparison.
   *
   * <p>This value is used to determine if two decimal numbers are "equal"
   * within acceptable precision limits. Some operations involve conversion
   * through doubles which can introduce small rounding errors.</p>
   */
  private static final double EPSILON = 1e-10;

  /**
   * Compares this DecimalScalar to another EngineValue for equality using epsilon tolerance.
   *
   * <p>While BigDecimal provides exact decimal arithmetic, some operations may involve
   * floating-point conversions. This method uses epsilon tolerance for robustness.</p>
   *
   * @param other the other EngineValue to compare with.
   * @return a BooleanScalar indicating whether the values are equal within tolerance.
   */
  @Override
  protected EngineValue unsafeEqualTo(EngineValue other) {
    double thisVal = getAsDouble();
    double otherVal = other.getAsDouble();
    double diff = Math.abs(thisVal - otherVal);

    // Use relative epsilon for large values, absolute for small values
    double scale = Math.max(1.0, Math.max(Math.abs(thisVal), Math.abs(otherVal)));
    boolean result = diff < EPSILON * scale;

    return new BooleanScalar(getCaster(), result, Units.EMPTY);
  }

  /**
   * Compares this DecimalScalar to another EngineValue for inequality using epsilon tolerance.
   *
   * <p>This is the logical negation of {@link #unsafeEqualTo(EngineValue)}.</p>
   *
   * @param other the other EngineValue to compare with.
   * @return a BooleanScalar indicating whether the values are not equal within tolerance.
   */
  @Override
  protected EngineValue unsafeNotEqualTo(EngineValue other) {
    double thisVal = getAsDouble();
    double otherVal = other.getAsDouble();
    double diff = Math.abs(thisVal - otherVal);

    // Use relative epsilon for large values, absolute for small values
    double scale = Math.max(1.0, Math.max(Math.abs(thisVal), Math.abs(otherVal)));
    boolean result = diff >= EPSILON * scale;

    return new BooleanScalar(getCaster(), result, Units.EMPTY);
  }
}
