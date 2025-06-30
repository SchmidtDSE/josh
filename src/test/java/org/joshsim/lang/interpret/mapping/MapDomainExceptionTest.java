package org.joshsim.lang.interpret.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MapDomainExceptionTest {

  private EngineValueFactory valueFactory;
  private EngineValue operand;
  private EngineValue domainLow;
  private EngineValue domainHigh;

  @BeforeEach
  void setUp() {
    valueFactory = new EngineValueFactory();
    operand = valueFactory.build(new BigDecimal("15"), Units.DEGREES);
    domainLow = valueFactory.build(new BigDecimal("0"), Units.DEGREES);
    domainHigh = valueFactory.build(new BigDecimal("10"), Units.DEGREES);
  }

  @Test
  @DisplayName("Should store operand and domain bounds correctly")
  void testConstructorAndGetters() {
    MapDomainException exception = new MapDomainException(operand, domainLow, domainHigh);
    
    assertEquals(operand, exception.getOperand());
    assertEquals(domainLow, exception.getDomainLow());
    assertEquals(domainHigh, exception.getDomainHigh());
  }

  @Test
  @DisplayName("Should generate informative error message")
  void testErrorMessage() {
    MapDomainException exception = new MapDomainException(operand, domainLow, domainHigh);
    
    String message = exception.getMessage();
    
    // Check that the message contains all expected elements
    assertTrue(message.contains("Map domain violation"));
    assertTrue(message.contains("15"));
    assertTrue(message.contains("degrees"));
    assertTrue(message.contains("0"));
    assertTrue(message.contains("10"));
    assertTrue(message.contains("modeling error"));
    assertTrue(message.contains("unbounded"));
    assertTrue(message.contains("domain bounds"));
    assertTrue(message.contains("calculations"));
  }

  @Test
  @DisplayName("Should handle negative values in error message")
  void testErrorMessageWithNegativeValue() {
    EngineValue negativeOperand = valueFactory.build(new BigDecimal("-5"), Units.of("%"));
    MapDomainException exception = new MapDomainException(negativeOperand, domainLow, domainHigh);
    
    String message = exception.getMessage();
    assertTrue(message.contains("-5"));
    assertTrue(message.contains("%"));
  }

  @Test
  @DisplayName("Should handle large values in error message")
  void testErrorMessageWithLargeValue() {
    EngineValue largeOperand = valueFactory.build(new BigDecimal("1000000"), Units.METERS);
    MapDomainException exception = new MapDomainException(largeOperand, domainLow, domainHigh);
    
    String message = exception.getMessage();
    assertTrue(message.contains("1000000"));
    assertTrue(message.contains("meters"));
  }
}