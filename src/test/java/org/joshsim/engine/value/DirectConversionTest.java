
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
import org.joshsim.engine.value.converter.DirectConversion;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test for a conversion which has a single callable.
 */
@ExtendWith(MockitoExtension.class)
public class DirectConversionTest {

  private Units sourceUnits;
  private Units destUnits;

  @Mock private CompiledCallable mockCallable;
  @Mock private Scope mockScope;
  @Mock private EngineValue mockEngineValue;

  private DirectConversion conversion;

  /**
   * Establish common values before each test.
   */
  @BeforeEach
  void setUp() {
    sourceUnits = new Units("m");
    destUnits = new Units("cm");
    conversion = new DirectConversion(sourceUnits, destUnits, mockCallable);
  }

  @Test
  void testGetSourceUnits() {
    assertEquals(sourceUnits, conversion.getSourceUnits());
  }

  @Test
  void testGetDestinationUnits() {
    assertEquals(destUnits, conversion.getDestinationUnits());
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
