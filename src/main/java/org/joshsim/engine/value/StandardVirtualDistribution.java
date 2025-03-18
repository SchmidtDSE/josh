/**
 * Structures describing a normal distribution.
 *
 * @license BSD-3-Clause
 */
package org.joshsim.engine.value;

/**
 * A normal distribution.
 *
 * <p>A standard or normal distribution as described by a mean and standard deviation but which does
 * not have a specific range.
 */
public abstract class StandardVirtualDistribution extends VirtualDistribution {
  /**
   * Create a new distribution, declaring the units of the distribution.
   *
   * @param units
   */
  public StandardVirtualDistribution(String units) {
    super(units);
  }
}
