/**
 * Structures describing strategies for automatic type conversions.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;


/**
 * Interface for strategies which manage automated type casts.
 */
public interface EngineValueCaster {

  /**
   * Convert two engine values to the same type so that they can participate in mutual operations.
   *
   * @param operands the operands that need to be made compatible.
   * @param requireSameUnits flag indicating if the units must be the same, throwing an exception if
   *    they are different only if this flag is true.
   */
  EngineValueTuple makeCompatible(EngineValueTuple operands, boolean requireSameUnits);
  
}
