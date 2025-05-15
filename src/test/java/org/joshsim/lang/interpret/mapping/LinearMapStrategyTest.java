package org.joshsim.lang.interpret.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


class LinearMapStrategyTest {

  private EngineValueFactory valueFactory;
  private MapBounds domain;
  private MapBounds range;
  private LinearMapStrategy strategy;

  @BeforeEach
  void setUp() {
    valueFactory = new EngineValueFactory();

    // Set up domain values (1 to 10)
    EngineValue domainLow = valueFactory.build(new BigDecimal("1"), Units.DEGREES);
    EngineValue domainHigh = valueFactory.build(new BigDecimal("10"), Units.DEGREES);
    domain = new MapBounds(domainLow, domainHigh);

    // Set up range values (20 to 200)
    EngineValue rangeLow = valueFactory.build(new BigDecimal("20"), Units.METERS);
    EngineValue rangeHigh = valueFactory.build(new BigDecimal("200"), Units.METERS);
    range = new MapBounds(rangeLow, rangeHigh);

    strategy = new LinearMapStrategy(valueFactory, domain, range);
  }

  @Test
  @DisplayName("Should map value below domain (0) correctly")
  void testMapValueBelowDomain() {
    EngineValue input = valueFactory.build(new BigDecimal("0"), Units.DEGREES);
    EngineValue result = strategy.apply(input);
    assertEquals(0, result.getAsDecimal().doubleValue(), 0.00001);
    assertEquals(result.getUnits().toString(), "meters");
  }

  @Test
  @DisplayName("Should map value within domain (5) correctly")
  void testMapValueWithinDomain() {
    EngineValue input = valueFactory.build(new BigDecimal("5"), Units.DEGREES);
    EngineValue result = strategy.apply(input);
    assertEquals(100, result.getAsDecimal().doubleValue(), 0.00001);
    assertEquals(result.getUnits().toString(), "meters");
  }

  @Test
  @DisplayName("Should map value above domain (11) correctly")
  void testMapValueAboveDomain() {
    EngineValue input = valueFactory.build(new BigDecimal("11"), Units.DEGREES);
    EngineValue result = strategy.apply(input);
    assertEquals(220, result.getAsDecimal().doubleValue(), 0.00001);
    assertEquals(result.getUnits().toString(), "meters");
  }
}
