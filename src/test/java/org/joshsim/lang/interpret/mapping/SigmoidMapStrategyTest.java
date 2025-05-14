
package org.joshsim.lang.interpret.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class SigmoidMapStrategyTest {
  private EngineValueFactory valueFactory;
  private MapBounds domain;
  private MapBounds range;
  private SigmoidMapStrategy strategy;

  @BeforeEach
  void setUp() {
    valueFactory = new EngineValueFactory();

    // Set up domain values (0 to 10)
    EngineValue domainLow = valueFactory.build(new BigDecimal("0"), Units.EMPTY);
    EngineValue domainHigh = valueFactory.build(new BigDecimal("10"), Units.EMPTY);
    domain = new MapBounds(domainLow, domainHigh);

    // Set up range values (10 to 20)
    EngineValue rangeLow = valueFactory.build(new BigDecimal("10"), Units.EMPTY);
    EngineValue rangeHigh = valueFactory.build(new BigDecimal("20"), Units.EMPTY);
    range = new MapBounds(rangeLow, rangeHigh);

    // Create strategy with scale -2
    strategy = new SigmoidMapStrategy(valueFactory, domain, range, -2.0);
  }

  @Test
  @DisplayName("Should map domain start (0) close to range start (10)")
  void testMapDomainStart() {
    EngineValue input = valueFactory.build(new BigDecimal("0"), Units.EMPTY);
    assertEquals(10.0, strategy.apply(input).getAsDecimal().doubleValue(), 0.1);
  }

  @Test
  @DisplayName("Should map domain middle (5) close to range middle (15)")
  void testMapDomainMiddle() {
    EngineValue input = valueFactory.build(new BigDecimal("5"), Units.EMPTY);
    assertEquals(15.0, strategy.apply(input).getAsDecimal().doubleValue(), 0.1);
  }

  @Test
  @DisplayName("Should map domain end (10) close to range end (20)")
  void testMapDomainEnd() {
    EngineValue input = valueFactory.build(new BigDecimal("10"), Units.EMPTY);
    assertEquals(20.0, strategy.apply(input).getAsDecimal().doubleValue(), 0.1);
  }
}
