
package org.joshsim.lang.interpret.mapping;

import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.engine.value.converter.Units;
import java.math.BigDecimal;

public class QuadraticMapStrategy implements MapStrategy {
  private final EngineValueFactory valueFactory;
  private final MapBounds domain;
  private final MapBounds range;
  private final boolean centerIsMaximum;

  public QuadraticMapStrategy(EngineValueFactory valueFactory, MapBounds domain, MapBounds range, boolean centerIsMaximum) {
    this.valueFactory = valueFactory;
    this.domain = domain;
    this.range = range;
    this.centerIsMaximum = centerIsMaximum;
  }

  @Override
  public EngineValue apply(EngineValue operand) {
    // Calculate domain midpoint
    EngineValue domainMid = domain.getHigh().subtract(domain.getLow()).divide(
        valueFactory.build(new BigDecimal("2"), Units.EMPTY)).add(domain.getLow());
    
    // Scale factor is based on range and domain
    EngineValue rangeSpan = range.getHigh().subtract(range.getLow());
    EngineValue domainSpan = domain.getHigh().subtract(domain.getLow());
    EngineValue scaleFactor = rangeSpan.divide(domainSpan.multiply(domainSpan.divide(
        valueFactory.build(new BigDecimal("4"), Units.EMPTY))));
    
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
