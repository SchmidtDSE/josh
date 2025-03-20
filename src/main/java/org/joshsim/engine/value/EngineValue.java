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

  protected final EngineValueCaster caster;
  protected final String units;

  /**
   * Create a new EngineValue.
   *
   * @param caster the EngineValueCaster to use for casting
   * @param units the units of the value
   */
  public EngineValue(EngineValueCaster caster, String units) {
    this.caster = caster;
    this.units = units;
  }

  /**
   * Get the units of this value.
   *
   * @return the units of this value
   */
  public String getUnits() {
    return units;
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
   * Checks if the units match or handles the case based on requirements.
   *
   *
   * @param other The other scalar to check units against
   * @param requireSameUnits Whether matching units are required
   * @return true if the operation can proceed, false otherwise
   * @throws IllegalArgumentException if units don't match and matching is required
   */
  protected void validateCommonUnits(EngineValue other) {
    if (!units.equals(other.getUnits())) {
      throw new IllegalArgumentException("Units do not match");
    }
  }

  /**
   * Add this value to another value.
   *
   * @param other the other value
   * @return the result of the addition
   * @throws IllegalArgumentException if units are incompatible
   */
  public EngineValue add(EngineValue other) {
    EngineValueTuple unsafeTuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, true);
    return safeTuple.getFirst().add(safeTuple.getSecond());
  }

  /**
   * Subtract another value from this value.
   *
   * @param other the other value
   * @return the result of the subtraction
   * @throws IllegalArgumentException if units are incompatible
   */
  public EngineValue subtract(EngineValue other) {
    EngineValueTuple unsafeTuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, true);
    return safeTuple.getFirst().subtract(safeTuple.getSecond());
  }

  /**
   * Multiply this value by another value.
   *
   * @param other the other value
   * @return the result of the multiplication
   * @throws IllegalArgumentException if units are incompatible
   */
  public EngineValue multiply(EngineValue other) {
    EngineValueTuple unsafeTuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, false);
    return safeTuple.getFirst().multiply(safeTuple.getSecond());
  }

  /**
   * Divide this value by another value.
   *
   * @param other the other value
   * @return the result of the division
   * @throws IllegalArgumentException if units are incompatible
   * @throws ArithmeticException if division by zero is attempted
   */
  public EngineValue divide(EngineValue other) {
    EngineValueTuple unsafeTuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, false);
    return safeTuple.getFirst().divide(safeTuple.getSecond());
  }

  /**
   * Raise this value to the power of another value.
   *
   * @param other the other value
   * @return the result of the exponentiation
   * @throws IllegalArgumentException if units are incompatible
   * @throws ArithmeticException if division by zero is attempted
   */
  public EngineValue raiseToPower(EngineValue other) {
    EngineValueTuple unsafeTuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, false);

    if (!other.getUnits().equals("count")) {
      throw new IllegalArgumentException("Can only raise to a count.");
    }

    return safeTuple.getFirst().raiseToPower(safeTuple.getSecond());
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
  abstract Scalar getAsScalar();

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
  abstract Distribution getAsDistribution();

  /**
   * Get a string description of the type that this engine value would be in Josh sources.
   *
   * <p>Get a string description of the type that this engine value would be in Josh sources, giving
   * subclasses control over when they no longer should follow the type conversion rules of their
   * parents.</p>
   *
   * @returns String description of this type as it would appear to Josh source code.
   */
  abstract String getLanguageType();

  /**
   * Change the type of this EngineValue.
   *
   * <p>Change the data type of this EngineValue, leaving the units unchanged. For example this may
   * change from int to string but not meters to centimeters.</p>
   *
   * @param strategy Cast strategy to apply.
   * @returns Engine value after cast.
   */
  abstract EngineValue cast(Cast strategy);

  /**
   * Get the underlying Java object decorated by this EngineValue.
   *
   * @returns inner Java object decorated by this EngineValue.
   */
  abstract Object getInnerValue();

  /**
   * Determine the new units string having multipled two units together.
   *
   * @param left units from the left side operand.
   * @param right units from the right side operand.
   * @returns new units description string.
   */
  abstract String determineMultipliedUnits(String left, String right);

  /**
   * Determine the new units string having divided two units.
   *
   * @param left units from the left side operand.
   * @param right units from the right side operand.
   * @returns new units description string.
   */
  abstract String determineDividedUnits(String left, String right);

  /**
   * Determine the new units string having raised a unit to a power.
   *
   * @param base units from the base operand.
   * @param exponent units from the exponent operand.
   * @returns new units description string.
   */
  abstract String determineRaisedUnits(String base, Long exponent);
}
