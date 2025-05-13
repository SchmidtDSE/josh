/**
 * Tests for NoopConversion.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoopConversionTest {

  @Mock private Scope mockScope;
  @Mock private EngineValue mockValue;
  private Units units;
  private NoopConversion conversion;

  @BeforeEach
  void setUp() {
    units = Units.of("m");
    conversion = new NoopConversion(units);
  }

  @Test
  void testGetSourceUnits() {
    assertEquals(units, conversion.getSourceUnits());
  }

  @Test
  void testGetDestinationUnits() {
    assertEquals(units, conversion.getDestinationUnits());
  }

  @Test
  void testConversionCallableExecution() {
    when(mockScope.get("current")).thenReturn(mockValue);

    EngineValue result = conversion.getConversionCallable().evaluate(mockScope);
    assertEquals(mockValue, result);
  }
}
