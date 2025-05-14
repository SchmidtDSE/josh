/**
 * Strategy for performing a quadratic map between a given domain and range.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.mapping;

import java.math.BigDecimal;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Map strategy which creates a quadratic mapping between the given domain and range.
 *
 * <p>The strategy maps values using a quadratic function where the domain endpoints map to either
 * the minimum or maximum of the range (based on centerIsMaximum), and the midpoint of the domain
 * maps to the opposite range value. The resulting curve forms a parabola.</p>
 */
public class QuadraticMapStrategy implements MapStrategy {
  private final EngineValueFactory valueFactory;
  private final MapBounds domain;
  private final MapBounds range;
  private final boolean centerIsMaximum;

  /**
   * Create a new map strategy that performs a quadratic mapping between a given domain and range.
   *
   * @param valueFactory The value factory to use in constructing returned and supporting values.
   * @param domain The domain from which values provided will be mapped quadratically.
   * @param range The range to which values provided will be mapped quadratically.
   * @param centerIsMaximum If true, the center of the domain maps to range maximum, endpoints to
   *     minimum. If false, the center maps to range minimum, endpoints to maximum.
   */
  public QuadraticMapStrategy(EngineValueFactory valueFactory, MapBounds domain, MapBounds range,
      boolean centerIsMaximum) {
    this.valueFactory = valueFactory;
    this.domain = domain;
    this.range = range;
    this.centerIsMaximum = centerIsMaximum;
  }

  @Override
  public EngineValue apply(EngineValue operand) {
    // Calculate domain midpoint
    EngineValue domainMid = domain.getHigh()
        .subtract(domain.getLow())
        .divide(valueFactory.build(new BigDecimal("2"), Units.EMPTY))
        .add(domain.getLow());

    // Scale factor is based on range and domain
    EngineValue rangeSpan = range.getHigh().subtract(range.getLow());
    EngineValue domainSpan = domain.getHigh().subtract(domain.getLow());
    EngineValue scaleFactor = rangeSpan.divide(
        domainSpan.multiply(
            domainSpan.divide(valueFactory.build(new BigDecimal("4"), Units.EMPTY))
        )
    );

    // Calculate quadratic term
    EngineValue x = operand.subtract(domainMid);
    EngineValue quadraticTerm = x.multiply(x).multiply(scaleFactor);

    if (centerIsMaximum) {
      return range.getHigh().subtract(quadraticTerm);
    } else {
      return range.getLow().add(quadraticTerm);
    }
  }
}
