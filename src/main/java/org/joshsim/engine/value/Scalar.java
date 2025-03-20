/**
 * Structures describing an individual engine value.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.math.BigDecimal;
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
   * Gets the value as a BigDecimal.
   *
   * @return the scalar value as a BigDecimal.
   */
  public abstract BigDecimal getAsDecimal();
  
  /**
   * Gets the value as a boolean.
   *
   * @return the scalar value as a boolean.
   */
  public abstract boolean getAsBoolean();
  
  /**
   * Gets the value as a String.
   *
   * @return the scalar value as a String.
   */
  public abstract String getAsString();
  
  /**
   * Gets the value as an integer.
   *
   * @return the scalar value as an int.
   */
  public abstract long getAsInt();

  /**
   * Converts this Scalar into a Distribution.
   *
   * <p>Creates a realized distribution containing this scalar value.</p>
   *
   * @return a Distribution representation of this Scalar.
   */
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
    
    // TODO: Seems smelly to have to convert to BigDecimal to compare Scalars
    return safeTuple.getFirst().getAsScalar().getAsDecimal()
            .compareTo(safeTuple.getSecond().getAsScalar().getAsDecimal());
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
  public EngineValue add(EngineValue other) {
    EngineValueTuple unsafeTuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, true);
    return safeTuple.getFirst().add(safeTuple.getSecond());
  }

  @Override
  public EngineValue subtract(EngineValue other) {
    EngineValueTuple unsafeTuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, true);
    return safeTuple.getFirst().subtract(safeTuple.getSecond());
  }

  @Override
  public EngineValue multiply(EngineValue other) {
    EngineValueTuple unsafeTuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, false);
    return safeTuple.getFirst().multiply(safeTuple.getSecond());
  }

  @Override
  public EngineValue divide(EngineValue other) {
    EngineValueTuple unsafeTuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, false);
    return safeTuple.getFirst().divide(safeTuple.getSecond());
  }

  @Override
  public EngineValue raiseToPower(EngineValue other) {
    EngineValueTuple unsafeTuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple, false);

    if (!other.getUnits().equals("count")) {
      throw new IllegalArgumentException("Can only raise to a count.");
    }

    return safeTuple.getFirst().raiseToPower(safeTuple.getSecond());
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

}
