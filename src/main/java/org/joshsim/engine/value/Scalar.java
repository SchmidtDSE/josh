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
public abstract class Scalar extends EngineValue {

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
   * Adds another scalar to this scalar.
   *
   * @param other the scalar to add.
   * @return the result of the addition.
   */
  public abstract Scalar add(Scalar other);

  /**
   * Subtracts another scalar from this scalar.
   *
   * @param other the scalar to subtract.
   * @return the result of the subtraction.
   */
  public abstract Scalar subtract(Scalar other);

  /**
   * Multiplies this scalar by another scalar.
   *
   * @param other the scalar to subtract.
   * @return
   */
  public abstract Scalar multiply(Scalar other);

  /**
   * Divides this scalar by another scalar.
   * @param other
   * @return
   */

  public abstract Scalar divide(Scalar other);
}
