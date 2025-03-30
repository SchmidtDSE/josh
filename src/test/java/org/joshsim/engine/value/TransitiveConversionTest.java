
/**
 * Tests for TransitiveConversion.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.converter.Conversion;
import org.joshsim.engine.value.converter.TransitiveConversion;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test chaining conversions for transitive behavior.
 */
@ExtendWith(MockitoExtension.class)
public class TransitiveConversionTest {

  @Mock(lenient = true) private Conversion mockFirstConversion;
  @Mock(lenient = true) private Conversion mockSecondConversion;
  @Mock(lenient = true) private CompiledCallable mockFirstCallable;
  @Mock(lenient = true) private CompiledCallable mockSecondCallable;
  @Mock(lenient = true) private Scope mockScope;
  @Mock(lenient = true) private EngineValue mockFirstResult;
  @Mock(lenient = true) private EngineValue mockSecondResult;

  private Units sourceUnits;
  private Units intermediateUnits;
  private Units destUnits;
  private TransitiveConversion conversion;

  /**
   * Crate common values for tests.
   */
  @BeforeEach
  void setUp() {
    sourceUnits = new Units("m");
    intermediateUnits = new Units("cm");
    destUnits = new Units("mm");

    when(mockFirstConversion.getSourceUnits()).thenReturn(sourceUnits);
    when(mockFirstConversion.getDestinationUnits()).thenReturn(intermediateUnits);
    when(mockFirstConversion.getConversionCallable()).thenReturn(mockFirstCallable);

    when(mockSecondConversion.getSourceUnits()).thenReturn(intermediateUnits);
    when(mockSecondConversion.getDestinationUnits()).thenReturn(destUnits);
    when(mockSecondConversion.getConversionCallable()).thenReturn(mockSecondCallable);

    conversion = new TransitiveConversion(mockFirstConversion, mockSecondConversion);
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
    when(mockFirstCallable.evaluate(mockScope)).thenReturn(mockFirstResult);
    when(mockSecondCallable.evaluate(any(Scope.class))).thenReturn(mockSecondResult);

    EngineValue result = conversion.getConversionCallable().evaluate(mockScope);
    assertEquals(mockSecondResult, result);
  }

  @Test
  void testConstructorThrowsOnUnitsMismatch() {
    Units differentUnits = new Units("kg");
    when(mockSecondConversion.getSourceUnits()).thenReturn(differentUnits);

    assertThrows(IllegalArgumentException.class,
        () -> new TransitiveConversion(mockFirstConversion, mockSecondConversion));
  }
}
