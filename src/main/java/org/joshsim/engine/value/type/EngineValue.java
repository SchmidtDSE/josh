/**
 * Structures describing either an individual value or distribution of values.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.type;

import java.math.BigDecimal;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;
import org.joshsim.engine.value.engine.EngineValueTuple;


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
   * Get the size of this EngineValue if known. Scalars will always have a size of 1,
   * while RealizedDistribution will have a size according to the elements within.
   *
   * @return the number of elements in the distribution, or empty if virtualized
   */
  public abstract Optional<Integer> getSize();

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
   * Gets the value as a double or samples randomly if a distribution.
   *
   * @return the scalar value as a double or distribution sampled.
   */
  public abstract double getAsDouble();

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
   * Gets the value as an Entity or samples randomly if a distribution.
   *
   * @return the single value as an Entity or distribution sampled.
   */
  public abstract Entity getAsEntity();

  /**
   * Gets the value as an MutableEntity or samples randomly if a distribution.
   *
   * @return the single value as an MutableEntity or distribution sampled.
   */
  public abstract MutableEntity getAsMutableEntity();

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
   * Create an immutable version of this EngineValue.
   *
   * @return An immutable version of this EngineValue that may still participate in operations but
   *      where its inner decorated value cannot change such that references held will resolve to
   *      the same value.
   */
  public EngineValue freeze() {
    return this;
  }

  /**
   * Determine the new units string having multipled two units together.
   *
   * @param left units from the left side operand.
   * @param right units from the right side operand.
   * @returns new units description string.
   */
  public Units determineMultipliedUnits(Units left, Units right) {
    return left.multiply(right);
  }

  /**
   * Determine the new units string having divided two units.
   *
   * @param left units from the left side operand.
   * @param right units from the right side operand.
   * @returns new units description string.
   */
  public Units determineDividedUnits(Units left, Units right) {
    return left.divide(right);
  }

  /**
   * Determine the new units string having raised a unit to a power.
   *
   * @param base units from the base operand.
   * @param exponent units from the exponent operand.
   * @returns new units description string.
   */
  public Units determineRaisedUnits(Units base, long exponent) {
    return base.raiseToPower(exponent);
  }

  /**
   * Get the EngineValueCaster for this value.
   *
   * @return the EngineValueCaster for this value
   */
  public EngineValueCaster getCaster() {
    return caster;
  }

  /**
   * Get a copy of this EngineValue which has the same data but different units label.
   *
   * <p>Create a new copy of this EngineValue that has the same inner value as this EngineValue but
   * with different units.</p>
   *
   * @param newUnits The new units to specify in the returned EngineValue. This will not change this
   *     original EngineValue.
   * @return Newly created independent EngineValue with the specified units.
   */
  public abstract EngineValue replaceUnits(Units newUnits);

  /**
   * Add another value to this value.
   *
   * <p>Performs addition after ensuring type and unit compatibility through casting.
   * If either value is a Distribution, the result will be a Distribution.</p>
   *
   * @param other the value to add to this value.
   * @return the result of the addition.
   * @throws IllegalArgumentException if the values are incompatible and cannot be cast.
   */
  public EngineValue add(EngineValue other) {
    EngineValueTuple unsafeTuple = EngineValueTuple.of(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, true);

    if (safeTuple.getSecond().getLanguageType().isDistribution()) {
      return safeTuple.getSecond().unsafeAdd(safeTuple.getFirst());
    } else {
      return safeTuple.getFirst().unsafeAdd(safeTuple.getSecond());
    }
  }

  /**
   * Subtract another value from this value.
   *
   * <p>Performs subtraction after ensuring type and unit compatibility through casting.
   * If either value is a Distribution, the result will be a Distribution.</p>
   *
   * @param other the value to subtract from this value.
   * @return the result of the subtraction.
   * @throws IllegalArgumentException if the values are incompatible and cannot be cast.
   */
  public EngineValue subtract(EngineValue other) {
    EngineValueTuple unsafeTuple = EngineValueTuple.of(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, true);

    if (safeTuple.getSecond().getLanguageType().isDistribution()) {
      return safeTuple.getFirst().unsafeSubtractFrom(safeTuple.getSecond());
    } else {
      return safeTuple.getFirst().unsafeSubtract(safeTuple.getSecond());
    }
  }

  /**
   * Multiply this value by another value.
   *
   * <p>Performs multiplication after ensuring type compatibility through casting. Units are
   * combined according to multiplication rules. If either value is a Distribution, the result will
   * be a Distribution.</p>
   *
   * @param other the value to multiply by
   * @return the result of the multiplication
   * @throws IllegalArgumentException if the values are incompatible and cannot be cast
   */
  public EngineValue multiply(EngineValue other) {
    EngineValueTuple unsafeTuple = EngineValueTuple.of(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, false);

    if (safeTuple.getSecond().getLanguageType().isDistribution()) {
      return safeTuple.getSecond().unsafeMultiply(safeTuple.getFirst());
    } else {
      return safeTuple.getFirst().unsafeMultiply(safeTuple.getSecond());
    }
  }

  /**
   * Divide this value by another value.
   *
   * <p>Performs division after ensuring type compatibility through casting. Units are combined
   * according to division rules. If either value is a Distribution, the result will be a
   * Distribution.</p>
   *
   * @param other the value to divide by.
   * @return the result of the division.
   * @throws IllegalArgumentException if the values are incompatible and cannot be cast.
   */
  public EngineValue divide(EngineValue other) {
    EngineValueTuple unsafeTuple = EngineValueTuple.of(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, false);

    if (safeTuple.getSecond().getLanguageType().isDistribution()) {
      return safeTuple.getSecond().unsafeDivideFrom(safeTuple.getFirst());
    } else {
      return safeTuple.getFirst().unsafeDivide(safeTuple.getSecond());
    }
  }

  /**
   * Raise this value to the power of another value.
   *
   * <p>Performs exponentiation after ensuring type compatibility through casting.The exponent must
   * be a count or unitless value. Units are combined according to power rules. If either value is
   * a Distribution, the result will be a Distribution.</p>
   *
   * @param other the exponent value to raise this value to
   * @return the result of raising this value to the given power
   * @throws IllegalArgumentException if the exponent has units or values are incompatible
   */
  public EngineValue raiseToPower(EngineValue other) {
    EngineValueTuple unsafeTuple = EngineValueTuple.of(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, false);

    if (!other.canBePower()) {
      String otherUnitsStr = other.getUnits().toString();
      throw new IllegalArgumentException("Can only raise to a count. Got: " + otherUnitsStr);
    }

    if (safeTuple.getSecond().getLanguageType().isDistribution()) {
      return safeTuple.getFirst().unsafeRaiseAllToPower(safeTuple.getSecond());
    } else {
      return safeTuple.getFirst().unsafeRaiseToPower(safeTuple.getSecond());
    }
  }

  /**
   * Determine if this is engine value is greater than another engine value.
   *
   * @param other The other engine value to compare.
   * @return EngineValue with a single boolean if comparing two scalars or a distribution of boolean
   *     values otherwise.
   */
  public EngineValue greaterThan(EngineValue other) {
    EngineValueTuple unsafeTuple = EngineValueTuple.of(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, false);

    if (safeTuple.getSecond().getLanguageType().isDistribution()) {
      return safeTuple.getSecond().unsafeLessThan(safeTuple.getFirst());
    } else {
      return safeTuple.getFirst().unsafeGreaterThan(safeTuple.getSecond());
    }
  }

  /**
   * Determine if this is engine value is greater than or equal to another engine value.
   *
   * @param other The other engine value to compare.
   * @return EngineValue with a single boolean if comparing two scalars or a distribution of boolean
   *     values otherwise.
   */
  public EngineValue greaterThanOrEqualTo(EngineValue other) {
    EngineValueTuple unsafeTuple = EngineValueTuple.of(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, false);

    if (safeTuple.getSecond().getLanguageType().isDistribution()) {
      return safeTuple.getSecond().unsafeLessThanOrEqualTo(safeTuple.getFirst());
    } else {
      return safeTuple.getFirst().unsafeGreaterThanOrEqualTo(safeTuple.getSecond());
    }
  }

  /**
   * Determine if this is engine value is less than another engine value.
   *
   * @param other The other engine value to compare.
   * @return EngineValue with a single boolean if comparing two scalars or a distribution of boolean
   *     values otherwise.
   */
  public EngineValue lessThan(EngineValue other) {
    EngineValueTuple unsafeTuple = EngineValueTuple.of(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, false);

    if (safeTuple.getSecond().getLanguageType().isDistribution()) {
      return safeTuple.getSecond().unsafeGreaterThan(safeTuple.getFirst());
    } else {
      return safeTuple.getFirst().unsafeLessThan(safeTuple.getSecond());
    }
  }

  /**
   * Determine if this is engine value is less than or equal to another engine value.
   *
   * @param other The other engine value to compare.
   * @return EngineValue with a single boolean if comparing two scalars or a distribution of boolean
   *     values otherwise.
   */
  public EngineValue lessThanOrEqualTo(EngineValue other) {
    EngineValueTuple unsafeTuple = EngineValueTuple.of(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, false);

    if (safeTuple.getSecond().getLanguageType().isDistribution()) {
      return safeTuple.getSecond().unsafeGreaterThanOrEqualTo(safeTuple.getFirst());
    } else {
      return safeTuple.getFirst().unsafeLessThanOrEqualTo(safeTuple.getSecond());
    }
  }

  /**
   * Determine if this is engine value is equal to another engine value.
   *
   * @param other The other engine value to compare.
   * @return EngineValue with a single boolean if comparing two scalars or a distribution of boolean
   *     values otherwise.
   */
  public EngineValue equalTo(EngineValue other) {
    EngineValueTuple unsafeTuple = EngineValueTuple.of(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, false);

    if (safeTuple.getSecond().getLanguageType().isDistribution()) {
      return safeTuple.getSecond().unsafeEqualTo(safeTuple.getFirst());
    } else {
      return safeTuple.getFirst().unsafeEqualTo(safeTuple.getSecond());
    }
  }

  /**
   * Determine if this is engine value is not equal to another engine value.
   *
   * @param other The other engine value to compare.
   * @return EngineValue with a single boolean if comparing two scalars or a distribution of boolean
   *     values otherwise.
   */
  public EngineValue notEqualTo(EngineValue other) {
    EngineValueTuple unsafeTuple = EngineValueTuple.of(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, false);

    if (safeTuple.getSecond().getLanguageType().isDistribution()) {
      return safeTuple.getSecond().unsafeNotEqualTo(safeTuple.getFirst());
    } else {
      return safeTuple.getFirst().unsafeNotEqualTo(safeTuple.getSecond());
    }
  }

  /**
   * Perform logical AND with another value.
   *
   * <p>Performs logical AND after type compatibility. If either value is a Distribution, the result
   * will be a Distribution of boolean values with element-wise AND operation.</p>
   *
   * @param other the value to AND with this value.
   * @return the result of the logical AND operation.
   */
  public EngineValue and(EngineValue other) {
    if (other.getLanguageType().isDistribution()) {
      return other.unsafeAnd(this);
    } else {
      return this.unsafeAnd(other);
    }
  }

  /**
   * Perform logical OR with another value.
   *
   * <p>Performs logical OR after type compatibility. If either value is a Distribution, the result
   * will be a Distribution of boolean values with element-wise OR operation.</p>
   *
   * @param other the value to OR with this value.
   * @return the result of the logical OR operation.
   */
  public EngineValue or(EngineValue other) {
    if (other.getLanguageType().isDistribution()) {
      return other.unsafeOr(this);
    } else {
      return this.unsafeOr(other);
    }
  }

  /**
   * Perform logical XOR with another value.
   *
   * <p>Performs logical XOR after type compatibility. If either value is a Distribution, the result
   * will be a Distribution of boolean values with element-wise XOR operation.</p>
   *
   * @param other the value to XOR with this value.
   * @return the result of the logical XOR operation.
   */
  public EngineValue xor(EngineValue other) {
    if (other.getLanguageType().isDistribution()) {
      return other.unsafeXor(this);
    } else {
      return this.unsafeXor(other);
    }
  }

  /**
   * Add this value to another value assuming that the units and type are compatible.
   *
   * @param other the other value.
   * @return the result of the addition.
   * @throws RuntimeException if the operation is not supported for this data type.
   * @throws IllegalArgumentException if units are incompatible.
   */
  protected abstract EngineValue unsafeAdd(EngineValue other);

  /**
   * Subtract another value from this value assuming units and type are compatible.
   *
   * @param other the other value.
   * @return the result of the subtraction.
   * @throws RuntimeException if the operation is not supported for this data type.
   * @throws IllegalArgumentException if units are incompatible.
   */
  protected abstract EngineValue unsafeSubtract(EngineValue other);

  /**
   * Multiply this value by another value assuming units and type are compatible.
   *
   * @param other the other value.
   * @return the result of the multiplication.
   * @throws RuntimeException if the operation is not supported for this data type.
   * @throws IllegalArgumentException if units are incompatible.
   */
  protected abstract EngineValue unsafeMultiply(EngineValue other);

  /**
   * Divide this value by another value assuming units and type are compatible.
   *
   * @param other the other value.
   * @return the result of the division.
   * @throws RuntimeException if the operation is not supported for this data type.
   * @throws IllegalArgumentException if units are incompatible.
   */
  protected abstract EngineValue unsafeDivide(EngineValue other);

  /**
   * Raise this value to the power of another value assuming units / type are compatible.
   *
   * @param other the other value.
   * @return the result of raising to power.
   * @throws RuntimeException if the operation is not supported for this data type.
   * @throws IllegalArgumentException if units are incompatible or exponent has non-count units.
   */
  protected abstract EngineValue unsafeRaiseToPower(EngineValue other);

  /**
   * Subtract this value from another value assuming units and type are compatible.
   *
   * <p>Subtract this value from another value assuming units and type are compatible. This is the
   * reverse of operands of unsafeSubtract as subtraction is non-commutative, unlike addition.</p>
   *
   * @param other the value to subtract this value from
   * @return the result of the subtraction
   * @throws RuntimeException if the operation is not supported for this data type
   * @throws IllegalArgumentException if units are incompatible
   */
  protected abstract EngineValue unsafeSubtractFrom(EngineValue other);

  /**
   * Divide another value by this value assuming units and type are compatible.
   *
   * <p>Divide another value by this value assuming units and type are compatible. This is the
   * reverse of operands of unsafeDivide as division is non-commutative, unlike multiplication.</p>
   *
   * @param other the value to be divided by this value.
   * @return the result of the division.
   * @throws RuntimeException if the operation is not supported for this data type.
   * @throws IllegalArgumentException if units are incompatible.
   */
  protected abstract EngineValue unsafeDivideFrom(EngineValue other);

  /**
   * Raise a value to all values in a distribution via broadcast.
   *
   * @param other the distribution of exponents.
   * @return the result of raising this value to all powers in the distribution.
   * @throws RuntimeException if the operation is not supported for this data type.
   * @throws IllegalArgumentException if exponent has non-count units.
   */
  protected abstract EngineValue unsafeRaiseAllToPower(EngineValue other);

  /**
   * Compare this value with another for greater-than assuming that units and type are compatible.
   *
   * @param other the other value.
   * @return the result of the comparison.
   * @throws NotImplementedException  if the operation is not supported for this data type.
   * @throws IllegalArgumentException if units are incompatible.
   */
  protected abstract EngineValue unsafeGreaterThan(EngineValue other);

  /**
   * Compare this value with another for greater-than-or-equal-to assuming compatible units / type.
   *
   * @param other the other value.
   * @return the result of the comparison.
   * @throws NotImplementedException  if the operation is not supported for this data type.
   * @throws IllegalArgumentException if units are incompatible.
   */
  protected abstract EngineValue unsafeGreaterThanOrEqualTo(EngineValue other);

  /**
   * Compare this value with another for less-than assuming that the units and type are compatible.
   *
   * @param other the other value.
   * @return the result of the comparison.
   * @throws NotImplementedException  if the operation is not supported for this data type.
   * @throws IllegalArgumentException if units are incompatible.
   */
  protected abstract EngineValue unsafeLessThan(EngineValue other);

  /**
   * Compare this value with another for less-than-or-equal-to assuming compatible units / type.
   *
   * @param other the other value.
   * @return the result of the comparison.
   * @throws NotImplementedException  if the operation is not supported for this data type.
   * @throws IllegalArgumentException if units are incompatible.
   */
  protected abstract EngineValue unsafeLessThanOrEqualTo(EngineValue other);

  /**
   * Compare this value with another for equal-to assuming compatible units / type.
   *
   * @param other the other value.
   * @return the result of the comparison.
   * @throws NotImplementedException  if the operation is not supported for this data type.
   * @throws IllegalArgumentException if units are incompatible.
   */
  protected abstract EngineValue unsafeEqualTo(EngineValue other);

  /**
   * Compare this value with another for equal-to assuming compatible units / type.
   *
   * @param other the other value.
   * @return the result of the comparison.
   * @throws NotImplementedException  if the operation is not supported for this data type.
   * @throws IllegalArgumentException if units are incompatible.
   */
  protected abstract EngineValue unsafeNotEqualTo(EngineValue other);

  /**
   * Perform logical AND with another value assuming type compatibility.
   *
   * @param other the other value.
   * @return the result of the logical AND.
   * @throws RuntimeException if the operation is not supported for this data type.
   */
  protected abstract EngineValue unsafeAnd(EngineValue other);

  /**
   * Perform logical OR with another value assuming type compatibility.
   *
   * @param other the other value.
   * @return the result of the logical OR.
   * @throws RuntimeException if the operation is not supported for this data type.
   */
  protected abstract EngineValue unsafeOr(EngineValue other);

  /**
   * Perform logical XOR with another value assuming type compatibility.
   *
   * @param other the other value.
   * @return the result of the logical XOR.
   * @throws RuntimeException if the operation is not supported for this data type.
   */
  protected abstract EngineValue unsafeXor(EngineValue other);

  /**
   * Determine if this value can be used to raise another value to a power.
   *
   * @return true if this can be a power and false otherwise.
   */
  protected boolean canBePower() {
    String unitsStr = getUnits().toString();
    return unitsStr.isEmpty() || unitsStr.equals("count");
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }

    if (other == this) {
      return true;
    }

    if (!(other instanceof EngineValue)) {
      return false;
    }

    EngineValue otherValue = (EngineValue) other;

    if (!getUnits().equals(otherValue.getUnits())) {
      return false;
    }

    return getInnerValue().equals(otherValue.getInnerValue());
  }
}
