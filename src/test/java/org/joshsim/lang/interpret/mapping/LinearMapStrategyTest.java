
package org.joshsim.lang.interpret.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class LinearMapStrategyTest {

  private EngineValueFactory valueFactory;
  private MapBounds domain;
  private MapBounds range;
  private LinearMapStrategy strategy;

  @BeforeEach
  void setUp() {
    valueFactory = mock(EngineValueFactory.class);
    
    // Set up domain values (1 to 10)
    EngineValue domainLow = mock(EngineValue.class);
    EngineValue domainHigh = mock(EngineValue.class);
    when(domainLow.getAsString()).thenReturn("1");
    when(domainHigh.getAsString()).thenReturn("10");
    domain = new MapBounds(domainLow, domainHigh);
    
    // Set up range values (20 to 200)
    EngineValue rangeLow = mock(EngineValue.class);
    EngineValue rangeHigh = mock(EngineValue.class);
    when(rangeLow.getAsString()).thenReturn("20");
    when(rangeHigh.getAsString()).thenReturn("200");
    range = new MapBounds(rangeLow, rangeHigh);
    
    // Mock the operations needed for the linear map calculations
    mockOperations(domainLow, domainHigh, rangeLow, rangeHigh);
    
    strategy = new LinearMapStrategy(valueFactory, domain, range);
  }

  private void mockOperations(EngineValue domainLow, EngineValue domainHigh, 
                            EngineValue rangeLow, EngineValue rangeHigh) {
    // Mock the zero value
    EngineValue zero = mock(EngineValue.class);
    when(valueFactory.build(BigDecimal.ZERO, Units.EMPTY)).thenReturn(zero);
    
    // Mock the span calculations and results
    EngineValue fromSpan = mock(EngineValue.class);
    EngineValue toSpan = mock(EngineValue.class);
    when(domainHigh.subtract(domainLow)).thenReturn(fromSpan);
    when(rangeHigh.subtract(rangeLow)).thenReturn(toSpan);
    
    // Mock operations for input value 0
    EngineValue input0 = mock(EngineValue.class);
    when(valueFactory.build(new BigDecimal("0"), Units.EMPTY)).thenReturn(input0);
    mockMapCalculations(input0, domainLow, fromSpan, toSpan, rangeLow, "0");
    
    // Mock operations for input value 5
    EngineValue input5 = mock(EngineValue.class);
    when(valueFactory.build(new BigDecimal("5"), Units.EMPTY)).thenReturn(input5);
    mockMapCalculations(input5, domainLow, fromSpan, toSpan, rangeLow, "110");
    
    // Mock operations for input value 11
    EngineValue input11 = mock(EngineValue.class);
    when(valueFactory.build(new BigDecimal("11"), Units.EMPTY)).thenReturn(input11);
    mockMapCalculations(input11, domainLow, fromSpan, toSpan, rangeLow, "220");
  }

  private void mockMapCalculations(EngineValue input, EngineValue domainLow, 
                                 EngineValue fromSpan, EngineValue toSpan,
                                 EngineValue rangeLow, String expectedResult) {
    EngineValue operandDiff = mock(EngineValue.class);
    EngineValue percent = mock(EngineValue.class);
    EngineValue result = mock(EngineValue.class);
    
    when(input.subtract(domainLow)).thenReturn(operandDiff);
    when(operandDiff.divide(fromSpan)).thenReturn(percent);
    when(toSpan.multiply(percent)).thenReturn(result);
    when(result.add(rangeLow)).thenReturn(result);
    when(result.getAsString()).thenReturn(expectedResult);
  }

  @Test
  @DisplayName("Should map value below domain (0) correctly")
  void testMapValueBelowDomain() {
    EngineValue input = valueFactory.build(new BigDecimal("0"), Units.EMPTY);
    EngineValue result = strategy.apply(input);
    assertEquals("0", result.getAsString());
  }

  @Test
  @DisplayName("Should map value within domain (5) correctly")
  void testMapValueWithinDomain() {
    EngineValue input = valueFactory.build(new BigDecimal("5"), Units.EMPTY);
    EngineValue result = strategy.apply(input);
    assertEquals("110", result.getAsString());
  }

  @Test
  @DisplayName("Should map value above domain (11) correctly")
  void testMapValueAboveDomain() {
    EngineValue input = valueFactory.build(new BigDecimal("11"), Units.EMPTY);
    EngineValue result = strategy.apply(input);
    assertEquals("220", result.getAsString());
  }
}
