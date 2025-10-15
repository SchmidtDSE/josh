/**
 * Structures describing an individual engine value.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.type;

import java.util.List;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;
import org.joshsim.engine.value.engine.EngineValueTuple;


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

  public Optional<Integer> getSize() {
    return Optional.of(1);
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
    List<EngineValue> values = List.of(this);
    return new RealizedDistribution(caster, values, getUnits());
  }

  /**
   * Indicate that non-entity conversion to entity for single values is not defined.
   *
   * @throws UnsupportedOperationException on all calls.
   */
  @Override
  public Entity getAsEntity() {
    throw new UnsupportedOperationException("Non-entity scalar conversion to entity not defined");
  }

  /**
   * Indicate that non-entity conversion to entity for single values is not defined.
   *
   * @throws UnsupportedOperationException on all calls.
   */
  @Override
  public MutableEntity getAsMutableEntity() {
    throw new UnsupportedOperationException("Non-entity scalar conversion to entity not defined");
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
    EngineValueTuple unsafeTuple = EngineValueTuple.of(this, other);
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

  @Override
  protected EngineValue unsafeGreaterThan(EngineValue other) {
    boolean result = getInnerValue().compareTo(other.getInnerValue()) > 0;
    return new BooleanScalar(caster, result, Units.EMPTY);
  }

  @Override
  protected EngineValue unsafeGreaterThanOrEqualTo(EngineValue other) {
    boolean result = getInnerValue().compareTo(other.getInnerValue()) >= 0;
    return new BooleanScalar(caster, result, Units.EMPTY);
  }

  @Override
  protected EngineValue unsafeLessThan(EngineValue other) {
    boolean result = getInnerValue().compareTo(other.getInnerValue()) < 0;
    return new BooleanScalar(caster, result, Units.EMPTY);
  }

  @Override
  protected EngineValue unsafeLessThanOrEqualTo(EngineValue other) {
    boolean result = getInnerValue().compareTo(other.getInnerValue()) <= 0;
    return new BooleanScalar(caster, result, Units.EMPTY);
  }

  @Override
  protected EngineValue unsafeEqualTo(EngineValue other) {
    boolean result = getInnerValue().compareTo(other.getInnerValue()) == 0;
    return new BooleanScalar(caster, result, Units.EMPTY);
  }

  @Override
  protected EngineValue unsafeNotEqualTo(EngineValue other) {
    boolean result = getInnerValue().compareTo(other.getInnerValue()) != 0;
    return new BooleanScalar(caster, result, Units.EMPTY);
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
      throw new IllegalArgumentException("Unsafe scalar operation: incompatible types.");
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

  /**
   * Log a message and raise an UnsupportedOperationException with the given message template.
   *
   * <p>This method utilizes the current Scalar's language type to format the message
   * before throwing the exception.</p>
   *
   * @param messageTemplate the template for the exception message.
   * @throws UnsupportedOperationException always thrown with a formatted message.
   */
  private void raiseUnsupported(String messageTemplate) {
    String message = String.format(messageTemplate, getLanguageType());
    throw new UnsupportedOperationException(message);
  }

}
