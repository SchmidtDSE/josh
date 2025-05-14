
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
 * The scale parameter controls how sharp the sigmoid curve is, with negative values creating an
 * increasing curve and positive values creating a decreasing curve.</p>
 */
public class SigmoidMapStrategy implements MapStrategy {
  private final EngineValueFactory valueFactory;
  private final MapBounds domain;
  private final MapBounds range;
  private final double scale;

  /**
   * Create a new map strategy that performs a sigmoid mapping between a given domain and range.
   *
   * @param valueFactory The value factory to use in constructing returned and supporting values.
   * @param domain The domain from which values provided will be mapped through the sigmoid.
   * @param range The range to which values provided will be mapped through the sigmoid.
   * @param scale Controls the steepness and direction of the sigmoid curve. Negative values create
   *     an increasing curve while positive values create a decreasing curve.
   */
  public SigmoidMapStrategy(EngineValueFactory valueFactory, MapBounds domain, MapBounds range,
      double scale) {
    this.valueFactory = valueFactory;
    this.domain = domain;
    this.range = range;
    this.scale = scale;
  }

  @Override
  public EngineValue apply(EngineValue operand) {
    // Normalize input to domain
    EngineValue normalizedX = operand.subtract(domain.getLow())
        .divide(domain.getHigh().subtract(domain.getLow()))
        .multiply(valueFactory.build(new BigDecimal("2"), Units.EMPTY))
        .subtract(valueFactory.build(new BigDecimal("1"), Units.EMPTY));

    // Calculate sigmoid: 1 / (1 + e^(scale * x))
    double x = normalizedX.getAsDecimal().doubleValue();
    double sigmoid = 1.0 / (1.0 + Math.exp(scale * x));

    // Rescale sigmoid output (0,1) to range
    EngineValue rangeSpan = range.getHigh().subtract(range.getLow());
    return valueFactory.build(new BigDecimal(sigmoid), Units.EMPTY)
        .multiply(rangeSpan)
        .add(range.getLow());
  }
}
