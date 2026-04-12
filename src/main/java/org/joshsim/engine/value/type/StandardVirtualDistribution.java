/**
 * Structures describing a normal distribution.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.type;

import java.math.BigDecimal;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;
import org.joshsim.util.SharedRandom;

/**
 * A normal distribution.
 *
 * <p>A standard or normal distribution as described by a mean and standard deviation but which
 * does not have a specific range.</p>
 */
public class StandardVirtualDistribution extends VirtualDistribution {
  private final BigDecimal mu;
  private final BigDecimal sigma;


  /**
   * Create a new standard virtual distribution.
   *
   * @param caster The value caster to use.
   * @param units The units of the distribution.
   */
  public StandardVirtualDistribution(EngineValueCaster caster, Units units, BigDecimal mu,
      BigDecimal sigma) {
    super(caster, units);
    this.mu = mu;
    this.sigma = sigma;
  }

  /**
   * Generate one sample from the distribution, according to mu and sigma.
   *
   * @return A sample from the distribution.
   */
  @Override
  public EngineValue sample() {
    double baseValue = SharedRandom.nextGaussian();
    double value = baseValue * sigma.doubleValue() + mu.doubleValue();
    return new DecimalScalar(caster, BigDecimal.valueOf(value), units);
  }
}
