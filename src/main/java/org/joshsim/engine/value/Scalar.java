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
public interface Scalar extends EngineValue {

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
  
}
