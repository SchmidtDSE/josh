/**
 * Structures describing either an individual value or distribution of values.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;
import java.math.BigDecimal;


/**
 * Structure representing a value in the engine.
 *
 * <p>Represents a value in the engine which may be an individual value (Scalar) or may be a
 * collection of values (Distribution).</p>
 */
public abstract class EngineValue {

  protected final EngineValueCaster caster;
  protected final Units units;

  /**
   * Create a new EngineValue.
   *
   * @param caster the EngineValueCaster to use for casting
   * @param units the units of the value
   */
  public EngineValue(EngineValueCaster caster, Units units) {
    this.caster = caster;
    this.units = units;
  }

  /**
   * Get the units of this value.
   *
   * @return the units of this value
   */
  public Units getUnits() {
    return units;
  }

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
   * Gets the value as a BigDecimal or samples randomly if a distribution.
   *
   * @return the scalar value as a BigDecimal or distribution sampled.
   */
  public abstract BigDecimal getAsDecimal();

  /**
   * Gets the value as a boolean or samples randomly if a distribution.
   *
   * @return the scalar value as a boolean or distribution sampled.
   */
  public abstract boolean getAsBoolean();

  /**
   * Gets the value as a String or samples randomly if a distribution.
   *
   * @return the scalar value as a String or distribution sampled.
   */
  public abstract String getAsString();

  /**
   * Gets the value as an integer or samples randomly if a distribution.
   *
   * @return the scalar value as an int or distribution sampled.
   */
  public abstract long getAsInt();

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
   * Get a string description of the type that this engine value would be in Josh sources.
   *
   * <p>Get a string description of the type that this engine value would be in Josh sources, giving
   * subclasses control over when they no longer should follow the type conversion rules of their
   * parents.</p>
   *
   * @returns String description of this type as it would appear to Josh source code.
   */
  public abstract LanguageType getLanguageType();

  /**
   * Change the type of this EngineValue.
   *
   * <p>Change the data type of this EngineValue, leaving the units unchanged. For example this may
   * change from int to string but not meters to centimeters.</p>
   *
   * @param strategy Cast strategy to apply.
   * @returns Engine value after cast.
   */
  public abstract EngineValue cast(Cast strategy);

  /**
   * Get the underlying Java object decorated by this EngineValue.
   *
   * @returns inner Java object decorated by this EngineValue.
   */
  public abstract Object getInnerValue();

  /**
   * Determine the new units string having multipled two units together.
   *
   * @param left units from the left side operand.
   * @param right units from the right side operand.
   * @returns new units description string.
   */
  public abstract String determineMultipliedUnits(String left, String right);

  /**
   * Determine the new units string having divided two units.
   *
   * @param left units from the left side operand.
   * @param right units from the right side operand.
   * @returns new units description string.
   */
  public abstract String determineDividedUnits(String left, String right);

  /**
   * Determine the new units string having raised a unit to a power.
   *
   * @param base units from the base operand.
   * @param exponent units from the exponent operand.
   * @returns new units description string.
   */
  public abstract String determineRaisedUnits(String base, Long exponent);

  /**
   * Get the EngineValueCaster for this value.
   *
   * @return the EngineValueCaster for this value
   */
  public EngineValueCaster getCaster() {
    return caster;
  }

  public EngineValue add(EngineValue other) {
    EngineValueTuple unsafeTuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, true);
    return safeTuple.getFirst().unsafeAdd(safeTuple.getSecond());
  }

  public EngineValue subtract(EngineValue other) {
    EngineValueTuple unsafeTuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, true);
    return safeTuple.getFirst().unsafeSubtract(safeTuple.getSecond());
  }

  public EngineValue multiply(EngineValue other) {
    EngineValueTuple unsafeTuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, false);
    return safeTuple.getFirst().unsafeMultiply(safeTuple.getSecond());
  }

  public EngineValue divide(EngineValue other) {
    EngineValueTuple unsafeTuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, false);
    return safeTuple.getFirst().unsafeDivide(safeTuple.getSecond());
  }

  public EngineValue raiseToPower(EngineValue other) {
    EngineValueTuple unsafeTuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, false);

    if (!other.canBePower()) {
      throw new IllegalArgumentException("Can only raise to a count.");
    }

    return safeTuple.getFirst().unsafeRaiseToPower(safeTuple.getSecond());
  }

  /**
   * Add this value to another value assuming that the units and type are compatible.
   *
   * @param other the other value.
   * @return the result of the addition.
   * @throws NotImplementedException if the operation is not supported for this data type.
   * @throws IllegalArgumentException if units are incompatible.
   */
  protected abstract EngineValue unsafeAdd(EngineValue other);

  /**
   * Subtract another value from this value assuming units and type are compatible.
   *
   * @param other the other value.
   * @return the result of the subtraction.
   * @throws NotImplementedException if the operation is not supported for this data type.
   * @throws IllegalArgumentException if units are incompatible.
   */
  protected abstract EngineValue unsafeSubtract(EngineValue other);

  /**
   * Multiply this value by another value assuming units and type are compatible.
   *
   * @param other the other value.
   * @return the result of the multiplication.
   * @throws NotImplementedException if the operation is not supported for this data type.
   * @throws IllegalArgumentException if units are incompatible.
   */
  protected abstract EngineValue unsafeMultiply(EngineValue other);

  /**
   * Divide this value by another value assuming units and type are compatible.
   *
   * @param other the other value.
   * @return the result of the division.
   * @throws NotImplementedException if the operation is not supported for this data type.
   * @throws IllegalArgumentException if units are incompatible.
   */
  protected abstract EngineValue unsafeDivide(EngineValue other);

  /**
   * Raise this value to the power of another value assuming units / type are compatible.
   *
   * @param other the other value.
   * @return the result of raising to power.
   * @throws NotImplementedException if the operation is not supported for this data type.
   * @throws IllegalArgumentException if units are incompatible or exponent has non-count units.
   */
  protected abstract EngineValue unsafeRaiseToPower(EngineValue other);

  /**
   * Determine if this value can be used to raise another value to a power.
   *
   * @return true if this can be a power and false otherwise.
   */
  protected boolean canBePower() {
    return getUnits().equals("") || getUnits().equals("count");
  }
}
