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
public abstract class Scalar extends EngineValue implements Comparable<Scalar> {

  /**
   * Constructor.
   */
  public Scalar(String units) {
      super(units);
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
   * Compares this scalar to another scalar.
   *
   * @param other the scalar to add.
   * @return -1 if the scalar is less than the other scalar, 0 if they are equal, 1 if the scalar is greater than the other scalar.
   */
  public int compareTo(Scalar other) {
    throw new Error("`compareTo` is not implemented for abstract type Scalar");
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
  public boolean equals(EngineValue obj) {
    throw new Error("`equals` is not implemented for abstract type Scalar");
  }
}
