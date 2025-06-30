package org.joshsim.lang.interpret.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
  @DisplayName("Should throw exception for value below domain when bounded")
  void testMapValueBelowDomainThrowsException() {
    EngineValue input = valueFactory.build(new BigDecimal("0"), Units.DEGREES);
    MapDomainException exception = assertThrows(MapDomainException.class, () -> {
      strategy.apply(input);
    });
    assertEquals(input, exception.getOperand());
    assertEquals(domain.getLow(), exception.getDomainLow());
    assertEquals(domain.getHigh(), exception.getDomainHigh());
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
  @DisplayName("Should throw exception for value above domain when bounded")
  void testMapValueAboveDomainThrowsException() {
    EngineValue input = valueFactory.build(new BigDecimal("11"), Units.DEGREES);
    MapDomainException exception = assertThrows(MapDomainException.class, () -> {
      strategy.apply(input);
    });
    assertEquals(input, exception.getOperand());
    assertEquals(domain.getLow(), exception.getDomainLow());
    assertEquals(domain.getHigh(), exception.getDomainHigh());
  }

  @Test
  @DisplayName("Should map value below domain (0) correctly when unbounded")
  void testMapValueBelowDomainUnbounded() {
    LinearMapStrategy unboundedStrategy = new LinearMapStrategy(valueFactory, domain, range, true);
    EngineValue input = valueFactory.build(new BigDecimal("0"), Units.DEGREES);
    EngineValue result = unboundedStrategy.apply(input);
    assertEquals(0, result.getAsDecimal().doubleValue(), 0.00001);
    assertEquals(result.getUnits().toString(), "meters");
  }

  @Test
  @DisplayName("Should map value above domain (11) correctly when unbounded")
  void testMapValueAboveDomainUnbounded() {
    LinearMapStrategy unboundedStrategy = new LinearMapStrategy(valueFactory, domain, range, true);
    EngineValue input = valueFactory.build(new BigDecimal("11"), Units.DEGREES);
    EngineValue result = unboundedStrategy.apply(input);
    assertEquals(220, result.getAsDecimal().doubleValue(), 0.00001);
    assertEquals(result.getUnits().toString(), "meters");
  }

  @Test
  @DisplayName("Should map boundary values correctly")
  void testMapBoundaryValues() {
    // Test lower boundary
    EngineValue lowerBoundary = valueFactory.build(new BigDecimal("1"), Units.DEGREES);
    EngineValue lowerResult = strategy.apply(lowerBoundary);
    assertEquals(20, lowerResult.getAsDecimal().doubleValue(), 0.00001);

    // Test upper boundary
    EngineValue upperBoundary = valueFactory.build(new BigDecimal("10"), Units.DEGREES);
    EngineValue upperResult = strategy.apply(upperBoundary);
    assertEquals(200, upperResult.getAsDecimal().doubleValue(), 0.00001);
  }

  @Test
  @DisplayName("Should provide helpful error message")
  void testErrorMessageContent() {
    EngineValue input = valueFactory.build(new BigDecimal("-5"), Units.DEGREES);
    MapDomainException exception = assertThrows(MapDomainException.class, () -> {
      strategy.apply(input);
    });
    
    String message = exception.getMessage();
    assert (message.contains("Map domain violation"));
    assert (message.contains("-5"));
    assert (message.contains("degrees"));
    assert (message.contains("unbounded"));
    assert (message.contains("modeling error"));
  }
}
