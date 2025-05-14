
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
  private SigmoidMapStrategy increasingStrategy;
  private SigmoidMapStrategy decreasingStrategy;

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

    increasingStrategy = new SigmoidMapStrategy(valueFactory, domain, range, true);
    decreasingStrategy = new SigmoidMapStrategy(valueFactory, domain, range, false);
  }

  @Test
  @DisplayName("Increasing: Should map domain start (0) to range start (10)")
  void testIncreasingMapDomainStart() {
    EngineValue input = valueFactory.build(new BigDecimal("0"), Units.EMPTY);
    assertEquals(10.0, increasingStrategy.apply(input).getAsDecimal().doubleValue(), 0.1);
  }

  @Test
  @DisplayName("Increasing: Should map domain middle (5) to range middle (15)")
  void testIncreasingMapDomainMiddle() {
    EngineValue input = valueFactory.build(new BigDecimal("5"), Units.EMPTY);
    assertEquals(15.0, increasingStrategy.apply(input).getAsDecimal().doubleValue(), 0.1);
  }

  @Test
  @DisplayName("Increasing: Should map domain end (10) to range end (20)")
  void testIncreasingMapDomainEnd() {
    EngineValue input = valueFactory.build(new BigDecimal("10"), Units.EMPTY);
    assertEquals(20.0, increasingStrategy.apply(input).getAsDecimal().doubleValue(), 0.1);
  }

  @Test
  @DisplayName("Decreasing: Should map domain start (0) to range end (20)")
  void testDecreasingMapDomainStart() {
    EngineValue input = valueFactory.build(new BigDecimal("0"), Units.EMPTY);
    assertEquals(20.0, decreasingStrategy.apply(input).getAsDecimal().doubleValue(), 0.1);
  }

  @Test
  @DisplayName("Decreasing: Should map domain middle (5) to range middle (15)")
  void testDecreasingMapDomainMiddle() {
    EngineValue input = valueFactory.build(new BigDecimal("5"), Units.EMPTY);
    assertEquals(15.0, decreasingStrategy.apply(input).getAsDecimal().doubleValue(), 0.1);
  }

  @Test
  @DisplayName("Decreasing: Should map domain end (10) to range start (10)")
  void testDecreasingMapDomainEnd() {
    EngineValue input = valueFactory.build(new BigDecimal("10"), Units.EMPTY);
    assertEquals(10.0, decreasingStrategy.apply(input).getAsDecimal().doubleValue(), 0.1);
  }
}
