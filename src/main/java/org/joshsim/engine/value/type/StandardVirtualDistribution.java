/**
 * Structures describing a normal distribution.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.type;

import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;

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
   * @param caster The value caster to use.
   * @param units The units of the distribution.
   */
  public StandardVirtualDistribution(EngineValueCaster caster, Units units) {
    super(caster, units);
  }
}
