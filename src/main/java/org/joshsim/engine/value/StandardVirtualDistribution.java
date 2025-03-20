/**
 * Structures describing a normal distribution.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;


/**
 * A normal distribution.
 *
 * <p>A standard or normal distribution as described by a mean and standard deviation but which
 * does not have a specific range.</p>
 */
public abstract class StandardVirtualDistribution extends VirtualDistribution {

  /**
   * Create a new standard virtual distribution.
   *
   * @param newCaster The value caster to use.
   * @param newUnits The units of the distribution.
   */
  public StandardVirtualDistribution(EngineValueCaster newCaster, String newUnits) {
    super(newCaster, newUnits);
  }
}
