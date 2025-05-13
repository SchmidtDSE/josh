/**
 * Tests for Converter.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.joshsim.engine.value.engine.EngineValueTuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test for a converter which supports conversion lookup.
 */
@ExtendWith(MockitoExtension.class)
public class MapConverterTest {

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
    sourceUnits = Units.of("m");
    destUnits = Units.of("cm");
    conversions = new HashMap<>();

    EngineValueTuple.UnitsTuple tuple = new EngineValueTuple.UnitsTuple(sourceUnits, destUnits);
    when(mockConversion.getSourceUnits()).thenReturn(sourceUnits);
    when(mockConversion.getDestinationUnits()).thenReturn(destUnits);

    conversions.put(tuple, mockConversion);
    converter = new MapConverter(conversions);
  }

  @Test
  void testGetConversion() {
    Conversion result = converter.getConversion(sourceUnits, destUnits);
    assertEquals(mockConversion, result);
  }

  @Test
  void testGetConversionThrowsOnMissingConversion() {
    Units unknownUnits = Units.of("unknown");
    assertThrows(IllegalArgumentException.class,
        () -> converter.getConversion(sourceUnits, unknownUnits));
  }

  @Test
  void testGetConversionWithSameUnits() {
    Conversion result = converter.getConversion(sourceUnits, sourceUnits);
    assertTrue(result instanceof NoopConversion);
  }
}
