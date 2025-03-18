/**
 * Structures describing either an individual value or distribution of values.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;


/**
 * Structure representing a value in the engine.
 *
 * <p>Represents a value in the engine which may be an individual value (Scalar) or may be a
 * collection of values (Distribution).</p>
 */
public abstract class EngineValue {
  private final String units;

  protected EngineValue(String units) {
    this.units = units;
  }

  public String getUnits() {
    return units;
  }

  /**
   * Add this value to another value.
   *
   * @param other the other value
   * @return the result of the addition
   * @throws IllegalArgumentException if units are incompatible
   */
  public abstract EngineValue add(EngineValue other);
  public abstract EngineValue addIntScalar(IntScalar other);
  public abstract EngineValue addDecimalScalar(DecimalScalar other);
  public abstract EngineValue addBooleanScalar(BooleanScalar other);
  public abstract EngineValue addStringScalar(StringScalar other);


  /**
   * Subtract another value from this value.
   *
   * @param other the other value
   * @return the result of the subtraction
   * @throws IllegalArgumentException if units are incompatible
   */
  public abstract EngineValue subtract(EngineValue other);
  public abstract EngineValue subtractIntScalar(IntScalar other);
  public abstract EngineValue subtractDecimalScalar(DecimalScalar other);
  public abstract EngineValue subtractBooleanScalar(BooleanScalar other);
  public abstract EngineValue subtractStringScalar(StringScalar other);

  /**
   * Multiply this value by another value.
   *
   * @param other the other value
   * @return the result of the multiplication
   * @throws IllegalArgumentException if units are incompatible
   */
  public abstract EngineValue multiply(EngineValue other);
  public abstract EngineValue multiplyIntScalar(IntScalar other);
  public abstract EngineValue multiplyDecimalScalar(DecimalScalar other);
  public abstract EngineValue multiplyBooleanScalar(BooleanScalar other);
  public abstract EngineValue multiplyStringScalar(StringScalar other);

  /**
   * Divide this value by another value.
   *
   * @param other the other value
   * @return the result of the division
   * @throws IllegalArgumentException if units are incompatible
   * @throws ArithmeticException if division by zero is attempted
   */
  public abstract EngineValue divide(EngineValue other);
  public abstract EngineValue divideIntScalar(IntScalar other);
  public abstract EngineValue divideDecimalScalar(DecimalScalar other);
  public abstract EngineValue divideBooleanScalar(BooleanScalar other);
  public abstract EngineValue divideStringScalar(StringScalar other);

  /**
   * Raise this value to the power of another value.
   *
   * @param other the other value
   * @return the result of the exponentiation
   * @throws IllegalArgumentException if units are incompatible
   * @throws ArithmeticException if division by zero is attempted
   */
  public abstract EngineValue power(EngineValue other);
  public abstract EngineValue powerIntScalar(IntScalar other);
  public abstract EngineValue powerDecimalScalar(DecimalScalar other);
  public abstract EngineValue powerBooleanScalar(BooleanScalar other);
  public abstract EngineValue powerStringScalar(StringScalar other);

  /**
   * Convert this EngineValue to a Scalar.
   *
   * <p>Convert this EngineValue to a Scalar such that Scalars return themselves unchanged while
   * Distributions are sampled randomly for a single value with selection probability proportional
   * to the frequency of each value. This can be useful if the user is providing a Distribution
   * where a Scalar is typically provided, allowing any operation to become stochastic.</p>
   *
   * @return This EngineValue either as a Scalar or sampled for a single Scalar.
   */
  public abstract Scalar getAsScalar();

  /**
   * Convert this EngineValue to a Distribution.
   *
   * <p>Convert this EngineValue to a Distribution such that Distributions return themselves
   * unchanged and Scalars are returned as a RealizedDistribution of size 1. This can be useful if
   * the user is trying to use a Scalar value in a place where a Distribution is typically
   * requested, causing effectively a distribution of constant value to be used.</p>
   *
   * @return This EngineValue as a distribution.
   */
  public abstract Distribution getAsDistribution();


  /**
   * Compare for equality.
   *
   * @param other the other valu
   */
  public boolean equals(EngineValue other) {
    return this.equals(other);
  }
}
