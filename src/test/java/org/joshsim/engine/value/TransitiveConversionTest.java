
/**
 * Tests for TransitiveConversion.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.func.Scope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TransitiveConversionTest {

  @Mock private Conversion mockFirstConversion;
  @Mock private Conversion mockSecondConversion;
  @Mock private CompiledCallable mockFirstCallable;
  @Mock private CompiledCallable mockSecondCallable;
  @Mock private Scope mockScope;
  @Mock private EngineValue mockFirstResult;
  @Mock private EngineValue mockSecondResult;

  private Units sourceUnits;
  private Units intermediateUnits;
  private Units destUnits;
  private TransitiveConversion conversion;

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
