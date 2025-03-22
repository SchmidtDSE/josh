/**
 * Structures to help support casts.
 *
 * @license BSD-3-Clause
 */
package org.joshsim.engine.value;

/**
 * Strategy which casts from one Scalar to another.
 *
 * <p>Strategy which casts from one Scalar type to another. This operates on the data type itself
 * like int, String, etc and not on the units like meters and centimeters.</p>
 */
public interface Cast {

  /**
   * Perform this cast operation.
   *
   * @param target the Scalar to cast.
   * @returns the Scalar after the cast operation.
   */
  EngineValue cast(Scalar target);
}
