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
   * @param newUnits String describing the units which may be a user-defined unit.
   */
  public Scalar(EngineValueCaster newCaster, String newUnits) {
    super(newCaster, newUnits);
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
  public abstract Comparable<?> getInnerValue();

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
    return safeTuple.getFirst().getInnerValue().equals(safeTuple.getSecond().getInnerValue());
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
  public EngineValue cast(Cast strategy) {
    return strategy.cast(this);
  }

  @Override
  public String getUnits() {
    return units;    
  }

  @Override
  public Scalar getAsScalar() {
    return this;
  }

  @Override
  public String determineMultipliedUnits(String left, String right) {
    Units leftUnits = new Units(left);
    Units rightUnits = new Units(right);
    return leftUnits.multiply(rightUnits).simplify().toString();
  }

  @Override
  public String determineDividedUnits(String left, String right) {
    Units leftUnits = new Units(left);
    Units rightUnits = new Units(right);
    return leftUnits.divide(rightUnits).simplify().toString();
  }

  @Override
  public String determineRaisedUnits(String base, Long exponent) {
    Units baseUnits = new Units(base);
    return baseUnits.raiseToPower(exponent).simplify().toString();
  }


  /**
   * Indicate that add is not supported for this type unless overloaded.
   *
   * @param other the other operand.
   */
  @Override
  protected EngineValue fulfillAdd(EngineValue other) {
    raiseUnsupported("Cannot add %s.");
  }

  /**
   * Indicate that subtract is not supported for this type unless overloaded.
   *
   * @param other the other operand.
   */
  @Override
  protected EngineValue fulfillSubtract(EngineValue other) {
    raiseUnsupported("Cannot subtract with %s.");
  }

  /**
   * Indicate that multiply is not supported for this type unless overloaded.
   *
   * @param other the other operand.
   */
  @Override
  protected EngineValue fulfillMultiply(EngineValue other) {
    raiseUnsupported("Cannot multiply with %s.");
  }

  /**
   * Indicate that divide is not supported for this type unless overloaded.
   *
   * @param other the other operand.
   */
  @Override
  protected EngineValue fulfillDivide(EngineValue other) {
    raiseUnsupported("Cannot divide with %s.");
  }

  /**
   * Indicate that raise to power is not supported for this type unless overloaded.
   *
   * @param other the other operand.
   */
  @Override
  protected EngineValue fulfillRaiseToPower(EngineValue other) {
    raiseUnsupported("Cannot raise %s to powers.");
  }

  protected void assertScalarCompatible(EngineValue other) {
    if (other.getLanguageType().contains("Distribution")) {
      throw IllegalArgumentException("Unexpected distribution.");
    }

    if (!getLanguageType().equals(other.getLanguageType())) {
      throw IllegalArgumentException("Unsafe scalar arithmetic operation with incompatible types.");
    }
  }

  private void raiseUnsupported(String messageTemplate) {
    String message = String.format(messageTemplate, getLanguageType());
    throw NotImplementedException(message);
  }

}
