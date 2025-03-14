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
public interface Scalar extends EngineValue, Comparable<Scalar> {

  /**
   * Gets the value as a BigDecimal.
   *
   * @return the scalar value as a BigDecimal.
   */
  BigDecimal getAsDecimal();
  
  /**
   * Gets the value as a boolean.
   *
   * @return the scalar value as a boolean.
   */
  boolean getAsBoolean();
  
  /**
   * Gets the value as a String.
   *
   * @return the scalar value as a String.
   */
  String getAsString();
  
  /**
   * Gets the value as an integer.
   *
   * @return the scalar value as an int.
   */
  int getAsInt();
  
  /**
   * Gets the type of this scalar value.
   *
   * @return the type as a String.
   */
  String getType();

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
  int compareTo(Scalar other);

  /**
   * Compare this EngineValue to the specified object for equality.
   *
   * <p>Compare two EngineValues for equality where two EngineValue objects are considered equal if
   * they have the same numeric value.</p>
   *
   * @param obj the object to compare with
   * @return true if the objects are equal, false otherwise
   */
  boolean equals(EngineValue obj);
  
}
