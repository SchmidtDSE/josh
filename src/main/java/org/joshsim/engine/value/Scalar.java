/**
 * Structures describing an individual engine value.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.util.List;


/**
 * Engine value which only has a single discrete value.
 */
public abstract class Scalar extends EngineValue implements Comparable<Scalar> {

  /**
   * Create a new scalar with the given units.
   *
   * @param caster The engine value caster to use in operations involving this scalar.
   * @param units for dimensional analysis.
   */
  public Scalar(EngineValueCaster caster, Units units) {
    super(caster, units);
  }

  /**
   * Converts this Scalar into a Distribution.
   *
   * <p>Creates a realized distribution containing this scalar value.</p>
   *
   * @return a Distribution representation of this Scalar.
   */
  @Override
  public Distribution getAsDistribution() {
    EngineValueFactory factory = new EngineValueFactory(getCaster());
    List<EngineValue> values = List.of(this);
    return factory.buildRealizedDistribution(values, getUnits());
  }

  /**
   * Get the decorated value.
   *
   * @returns value inside this Scalar decorator.
   */
  @Override
  public abstract Comparable getInnerValue();

  /**
   * Compare this Scalar to the specified object.
   *
   * <p>Compare two Scalars for ordinal ranking where two Scalar objects are considered equal if
   
   * they have the same numeric value.</p>
   *
   * @param other the object to compare with.
   * @return A number less than 0 if this is less than other, 0 if the two are the same, and a
   *     number larger than 1 if this is more than other.
   */
  @Override
  public int compareTo(Scalar other) {
    EngineValueTuple unsafeTuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, true);
    Scalar firstScalar = (Scalar) safeTuple.getFirst();
    Scalar secondScalar = (Scalar) safeTuple.getSecond();
    return firstScalar.getInnerValue().compareTo(secondScalar.getInnerValue());
  }

  /**
   * Compare this EngineValue to the specified object for equality.
   *
   * <p>Compare two EngineValues for equality where two EngineValue objects are considered equal if
   * they have the same numeric value.</p>
   *
   * @param other the object to compare with
   * @return true if the objects are equal, false otherwise
   */
  public boolean equals(EngineValue other) {
    boolean sameUnits = getUnits().equals(other.getUnits());
    boolean sameValue = getInnerValue().equals(other.getInnerValue());
    return sameUnits && sameValue;
  }

  @Override
  public boolean equals(Object other) {
    return equals((EngineValue) other);
  }

  @Override
  public EngineValue cast(Cast strategy) {
    return strategy.cast(this);
  }

  @Override
  public Scalar getAsScalar() {
    return this;
  }

  /**
   * Indicate that add is not supported for this type unless overloaded.
   *
   * @param other the other operand.
   */
  @Override
  protected EngineValue unsafeAdd(EngineValue other) {
    raiseUnsupported("Cannot add %s.");
    return null;
  }

  /**
   * Indicate that subtract is not supported for this type unless overloaded.
   *
   * @param other the other operand.
   */
  @Override
  protected EngineValue unsafeSubtract(EngineValue other) {
    raiseUnsupported("Cannot subtract with %s.");
    return null;
  }

  /**
   * Indicate that multiply is not supported for this type unless overloaded.
   *
   * @param other the other operand.
   */
  @Override
  protected EngineValue unsafeMultiply(EngineValue other) {
    raiseUnsupported("Cannot multiply with %s.");
    return null;
  }

  /**
   * Indicate that divide is not supported for this type unless overloaded.
   *
   * @param other the other operand.
   */
  @Override
  protected EngineValue unsafeDivide(EngineValue other) {
    raiseUnsupported("Cannot divide with %s.");
    return null;
  }

  @Override
  protected EngineValue unsafeSubtractFrom(EngineValue other) {
    return other.unsafeSubtract(this);
  }

  @Override
  protected EngineValue unsafeDivideFrom(EngineValue other) {
    return other.unsafeDivide(this);
  }

  @Override
  protected EngineValue unsafeRaiseAllToPower(EngineValue other) {
    return other.unsafeRaiseToPower(this);
  }

  /**
   * Indicate that raise to power is not supported for this type unless overloaded.
   *
   * @param other the other operand.
   */
  @Override
  protected EngineValue unsafeRaiseToPower(EngineValue other) {
    raiseUnsupported("Cannot raise %s to powers.");
    return null;
  }

  
  /**
   * Check if provided EngineValue is compatible with the current Scalar for arithmetic operations.
   *
   * <p>This method ensures that the other EngineValue is not a distribution and that it has the
   * same type as the current Scalar. If these conditions are not met, an 
   * IllegalArgumentException is thrown.</p>
   *
   * @param other the EngineValue to check compatibility with.
   * @throws IllegalArgumentException if the other EngineValue is a distribution or has a different
   *     type.
   */
  protected void assertScalarCompatible(EngineValue other) {
    if (other.getLanguageType().isDistribution()) {
      throw new IllegalArgumentException("Unexpected distribution.");
    }

    if (!getLanguageType().equals(other.getLanguageType())) {
      throw new IllegalArgumentException("Unsafe scalar arithmetic operation with incompatible types.");
    }
  }

  /**
   * Provides a human-readable representation of the Scalar.
   * 
   * <p>This implementation returns a string representation of the inner value
   * contained by this Scalar, which can be useful for debugging and logging purposes.</p>
   * 
   * @return a string representation of this Scalar.
   */
  @Override
  public String toString() {
    return "Scalar [value=" + getInnerValue() + ", units=" + getUnits() + "]";
  }
  private void raiseUnsupported(String messageTemplate) {
    String message = String.format(messageTemplate, getLanguageType());
    throw new UnsupportedOperationException(message);
  }

}
