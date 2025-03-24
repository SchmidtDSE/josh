/**
 * Tests for Converter.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test for a converter which supports conversion lookup.
 */
@ExtendWith(MockitoExtension.class)
public class ConverterTest {

  @Mock(lenient = true) private Conversion mockConversion;
  private Units sourceUnits;
  private Units destUnits;
  private Map<EngineValueTuple.UnitsTuple, Conversion> conversions;
  private Converter converter;

  /**
   * Setup common objects across tests with an example conversion.
   */
  @BeforeEach
  void setUp() {
    sourceUnits = new Units("m");
    destUnits = new Units("cm");
    conversions = new HashMap<>();
    
    EngineValueTuple.UnitsTuple tuple = new EngineValueTuple.UnitsTuple(sourceUnits, destUnits);
    when(mockConversion.getSourceUnits()).thenReturn(sourceUnits);
    when(mockConversion.getDestinationUnits()).thenReturn(destUnits);
    
    conversions.put(tuple, mockConversion);
    converter = new Converter(conversions);
  }

  @Test
  void testGetConversion() {
    Conversion result = converter.getConversion(sourceUnits, destUnits);
    assertEquals(mockConversion, result);
  }

  @Test
  void testGetConversionThrowsOnMissingConversion() {
    Units unknownUnits = new Units("unknown");
    assertThrows(IllegalArgumentException.class,
        () -> converter.getConversion(sourceUnits, unknownUnits));
  }

  @Test
  void testGetConversionWithSameUnits() {
    Conversion result = converter.getConversion(sourceUnits, sourceUnits);
    assertTrue(result instanceof NoopConversion);
  }
}