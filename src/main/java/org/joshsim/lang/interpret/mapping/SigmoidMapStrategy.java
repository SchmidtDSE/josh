
/**
 * Strategy for performing a sigmoid map between a given domain and range.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.mapping;

import java.math.BigDecimal;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Map strategy which creates a sigmoid mapping between the given domain and range.
 *
 * <p>The strategy maps values using a sigmoid function of the form y = 1 / (1 + e^(scale * x)).
 * The domain controls the x-axis scaling and shifting while the range controls the y-axis scaling.
 * The direction parameter controls whether the mapping increases or decreases with x.</p>
 */
public class SigmoidMapStrategy implements MapStrategy {
  private final EngineValueFactory valueFactory;
  private final MapBounds domain;
  private final MapBounds range;
  private final boolean increasing;
  private final double scale;

  /**
   * Create a new map strategy that performs a sigmoid mapping between a given domain and range.
   *
   * @param valueFactory The value factory to use in constructing returned and supporting values.
   * @param domain The domain from which values provided will be mapped through the sigmoid.
   * @param range The range to which values provided will be mapped through the sigmoid.
   * @param increasing If true, values increase as x increases. If false, values decrease.
   */
  public SigmoidMapStrategy(EngineValueFactory valueFactory, MapBounds domain, MapBounds range,
      boolean increasing) {
    this.valueFactory = valueFactory;
    this.domain = domain;
    this.range = range;
    this.increasing = increasing;

    // Calculate scale based on domain size
    // For domain [-5,5] and range [0,1], scale should be -1 for increasing
    double domainSize = domain.getHigh()
        .subtract(domain.getLow())
        .getAsDouble();

    scale = (increasing ? -1.0 : 1.0) * (10.0 / domainSize);
  }

  @Override
  public EngineValue apply(EngineValue operand) {
    // Normalize input to domain [-1,1]
    EngineValue normalizedX = operand.subtract(domain.getLow())
        .divide(domain.getHigh().subtract(domain.getLow()))
        .multiply(valueFactory.buildForNumber(2, Units.EMPTY))
        .subtract(valueFactory.buildForNumber(1, Units.EMPTY));

    // Calculate sigmoid: 1 / (1 + e^(scale * x))
    double x = normalizedX.getAsDouble() * 5;
    double sigmoid = 1.0 / (1.0 + Math.exp(scale * x));

    // Rescale sigmoid output (0,1) to range
    EngineValue rangeSpan = range.getHigh().subtract(range.getLow());
    return valueFactory.buildForNumber(sigmoid, Units.EMPTY)
        .multiply(rangeSpan)
        .add(range.getLow());
  }
}
