/**
 * Strategy for performing a sigmoid map between a given domain and range.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.mapping;

import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Map strategy which creates a sigmoid (S-curve) mapping between the given domain and range.
 *
 * <p>The strategy maps values using a sigmoid function that creates an S-shaped curve.
 * Values near the domain boundaries approach (but don't quite reach) the range boundaries,
 * while the middle of the domain maps to the middle of the range. The curve shape provides
 * smooth transitions with gradual changes near the boundaries.</p>
 */
public class SigmoidMapStrategy implements MapStrategy {
  private final EngineValueFactory valueFactory;
  private final MapBounds domain;
  private final MapBounds range;
  private final boolean increasing;

  /**
   * The steepness factor for the sigmoid curve.
   * Higher values make the transition sharper, lower values make it more gradual.
   * Value of 6 means input normalized to [-6, 6] which gives ~99.75% coverage of sigmoid range.
   */
  private static final double STEEPNESS = 6.0;

  /**
   * Create a new map strategy that performs a sigmoid mapping between a given domain and range.
   *
   * @param valueFactory The value factory to use in constructing returned and supporting values.
   * @param domain The domain from which values provided will be mapped through the sigmoid.
   * @param range The range to which values provided will be mapped through the sigmoid.
   * @param increasing If true, values increase as input increases (standard S-curve).
   *     If false, values decrease as input increases (inverted S-curve).
   */
  public SigmoidMapStrategy(EngineValueFactory valueFactory, MapBounds domain, MapBounds range,
      boolean increasing) {
    this.valueFactory = valueFactory;
    this.domain = domain;
    this.range = range;
    this.increasing = increasing;
  }

  @Override
  public EngineValue apply(EngineValue operand) {
    // Get domain and range bounds as doubles for calculation
    double domainLow = domain.getLow().getAsDouble();
    double domainHigh = domain.getHigh().getAsDouble();
    double rangeLow = range.getLow().getAsDouble();
    double rangeHigh = range.getHigh().getAsDouble();

    double input = operand.getAsDouble();

    // Normalize input to [-STEEPNESS, +STEEPNESS] for sigmoid
    // This ensures the sigmoid covers most of its range (0 to 1)
    double domainSpan = domainHigh - domainLow;
    double t = (input - domainLow) / domainSpan; // [0, 1]
    double x = (t * 2.0 - 1.0) * STEEPNESS; // [-STEEPNESS, +STEEPNESS]

    // Apply sigmoid: 1 / (1 + e^(-x))
    double sigmoidValue = 1.0 / (1.0 + Math.exp(-x));

    // Handle increasing vs decreasing
    if (!increasing) {
      sigmoidValue = 1.0 - sigmoidValue;
    }

    // Scale to output range
    double rangeSpan = rangeHigh - rangeLow;
    double output = rangeLow + sigmoidValue * rangeSpan;

    return valueFactory.buildForNumber(output, range.getLow().getUnits());
  }
}
