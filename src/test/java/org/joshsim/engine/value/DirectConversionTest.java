
/**
 * Tests for DirectConversion.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.Scope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DirectConversionTest {

  @Mock private Units mockSourceUnits;
  @Mock private Units mockDestUnits;
  @Mock private CompiledCallable mockCallable;
  @Mock private Scope mockScope;
  @Mock private EngineValue mockEngineValue;

  private DirectConversion conversion;

  @BeforeEach
  void setUp() {
    conversion = new DirectConversion(mockSourceUnits, mockDestUnits, mockCallable);
  }

  @Test
  void testGetSourceUnits() {
    assertEquals(mockSourceUnits, conversion.getSourceUnits());
  }

  @Test
  void testGetDestinationUnits() {
    assertEquals(mockDestUnits, conversion.getDestinationUnits());
  }

  @Test
  void testGetConversionCallable() {
    assertEquals(mockCallable, conversion.getConversionCallable());
  }

  @Test
  void testConversionCallableExecution() {
    when(mockCallable.evaluate(mockScope)).thenReturn(mockEngineValue);
    
    EngineValue result = conversion.getConversionCallable().evaluate(mockScope);
    assertEquals(mockEngineValue, result);
  }
}
