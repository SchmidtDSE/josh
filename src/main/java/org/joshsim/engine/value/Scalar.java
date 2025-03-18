/**
 * Structures describing an individual engine value.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import java.math.BigDecimal;


/**
 * Engine value which only has a single discrete value.
 */
public abstract class Scalar extends EngineValue, Comparable<Scalar> {

  private final String units;

  /**
   * Create a new scalar with the given units.
   *
   * @param newUnits String describing the units which may be a user-defined unit.
   */
  public Scalar(String newUnits) {
    units = newUnits;
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
  public abstract int getAsInt();
  
  /**
   * Gets the type of this scalar value.
   *
   * @return the type as a String.
   */
  public abstract String getType();

  /**
   *
   */
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
  public int compareTo(Scalar other) {
    return getInnerValue().compareTo(other.getInnerValue());
  }

  /**
   * Compare this EngineValue to the specified object for equality.
   *
   * <p>Compare two EngineValues for equality where two EngineValue objects are considered equal if
   * they have the same numeric value.</p>
   *
   * @param obj the object to compare with
   * @return true if the objects are equal, false otherwise
   */
  public boolean equals(EngineValue other) {
    boolean sameUnits = getUnits().equals(obj.getUnits());
    boolean sameValue = getInnerValue().equals(other.getInnerValue());
    return sameUnits && sameValue;
  }

  public EngineValue cast(Cast strategy) {
    return strategy.cast(this);
  }

  @Overrides
  public EngineValue add(EngineValue other) {
    EngineValueTuple tuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple);
    return safeTuple.getFirst().add(safeTuple.getSecond());
  }

  @Overrides
  public EngineValue subtract(EngineValue other) {
    EngineValueTuple tuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple);
    return safeTuple.getFirst().subtract(safeTuple.getSecond());
  }

  @Overrides
  public EngineValue multiply(EngineValue other) {
    EngineValueTuple tuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple);
    return safeTuple.getFirst().multiply(safeTuple.getSecond());
  }

  @Overrides
  public EngineValue divide(EngineValue other) {
    EngineValueTuple tuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple);
    return safeTuple.getFirst().divide(safeTuple.getSecond());
  }

  @Overrides
  public EngineValue raiseToPower(EngineValue other) {
    EngineValueTuple tuple = new EngineValueTuple(this, other);
    EngineValueTuple safeTuple = caster.makeCompatible(unsafeTuple);
    return safeTuple.getFirst().raiseToPower(safeTuple.getSecond());
  }

  @Overrides
  public String getUnits() {
    return units;    
  }

  @Overrides
  public Scalar getAsScalar() {
    return this;
  }
  
}
