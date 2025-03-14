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
public abstract class EngineValue implements Comparable<EngineValue> {
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

  /**
   * Subtract another value from this value.
   *
   * @param other the other value
   * @return the result of the subtraction
   * @throws IllegalArgumentException if units are incompatible
   */
  public abstract EngineValue subtract(EngineValue other);

  /**
   * Multiply this value by another value.
   *
   * @param other the other value
   * @return the result of the multiplication
   * @throws IllegalArgumentException if units are incompatible
   */
  public abstract EngineValue multiply(EngineValue other);

  /**
   * Divide this value by another value.
   *
   * @param other the other value
   * @return the result of the division
   * @throws IllegalArgumentException if units are incompatible
   * @throws ArithmeticException if division by zero is attempted
   */
  public abstract EngineValue divide(EngineValue other);

  /**
   * Raise this value to the power of another value.
   *
   * @param other the other value
   * @return the result of the exponentiation
   * @throws IllegalArgumentException if units are incompatible
   * @throws ArithmeticException if division by zero is attempted
   */
  public abstract EngineValue raiseToPower(EngineValue other);

  /**
   * Get the units of this value.
   *
   * @return the units of this value
   */
  public abstract boolean equals(EngineValue obj);

  /**
   * Compare this EngineValue to the specified object.
   *
   * <p>Compare two EngineValues for ordinal ranking where two EngineValue objects are considered
   * equal if they have the same numeric value.</p>
   *
   * @param other the object to compare with.
   * @return A number less than 0 if this is less than other, 0 if the two are the same, and a
   *     number larger than 1 if this is more than the other.
   */
  public abstract int compareTo(EngineValue other);

}