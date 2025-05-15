
package org.joshsim.lang.interpret.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QuadraticMapStrategyTest {
  private EngineValueFactory valueFactory;
  private MapBounds domain;
  private MapBounds range;
  private QuadraticMapStrategy maxStrategy;
  private QuadraticMapStrategy minStrategy;

  @BeforeEach
  void setUp() {
    valueFactory = new EngineValueFactory();

    // Set up domain values (5 to 15)
    EngineValue domainLow = valueFactory.build(new BigDecimal("5"), Units.METERS);
    EngineValue domainHigh = valueFactory.build(new BigDecimal("15"), Units.METERS);
    domain = new MapBounds(domainLow, domainHigh);

    // Set up range values (0 to 100)
    EngineValue rangeLow = valueFactory.build(new BigDecimal("0"), Units.DEGREES);
    EngineValue rangeHigh = valueFactory.build(new BigDecimal("100"), Units.DEGREES);
    range = new MapBounds(rangeLow, rangeHigh);

    maxStrategy = new QuadraticMapStrategy(valueFactory, domain, range, true);
    minStrategy = new QuadraticMapStrategy(valueFactory, domain, range, false);
  }

  @Test
  @DisplayName("When center is maximum, domain endpoints should map to range minimum")
  void testMaxStrategyEndpoints() {
    EngineValue input1 = valueFactory.build(new BigDecimal("5"), Units.METERS);
    EngineValue input2 = valueFactory.build(new BigDecimal("15"), Units.METERS);

    assertEquals(0, maxStrategy.apply(input1).getAsDecimal().doubleValue(), 0.00001);
    assertEquals(0, maxStrategy.apply(input2).getAsDecimal().doubleValue(), 0.00001);
  }

  @Test
  @DisplayName("When center is maximum, domain midpoint should map to range maximum")
  void testMaxStrategyMidpoint() {
    EngineValue input = valueFactory.build(new BigDecimal("10"), Units.METERS);
    assertEquals(100, maxStrategy.apply(input).getAsDecimal().doubleValue(), 0.00001);
  }

  @Test
  @DisplayName("When center is minimum, domain endpoints should map to range maximum")
  void testMinStrategyEndpoints() {
    EngineValue input1 = valueFactory.build(new BigDecimal("5"), Units.METERS);
    EngineValue input2 = valueFactory.build(new BigDecimal("15"), Units.METERS);

    assertEquals(100, minStrategy.apply(input1).getAsDecimal().doubleValue(), 0.00001);
    assertEquals(100, minStrategy.apply(input2).getAsDecimal().doubleValue(), 0.00001);
  }

  @Test
  @DisplayName("When center is minimum, domain midpoint should map to range minimum")
  void testMinStrategyMidpoint() {
    EngineValue input = valueFactory.build(new BigDecimal("10"), Units.METERS);
    assertEquals(0, minStrategy.apply(input).getAsDecimal().doubleValue(), 0.00001);
  }
}
