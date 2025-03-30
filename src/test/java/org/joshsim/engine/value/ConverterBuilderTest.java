
/**
 * Tests for ConverterBuilder.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.joshsim.engine.value.converter.Conversion;
import org.joshsim.engine.value.converter.Converter;
import org.joshsim.engine.value.converter.ConverterBuilder;
import org.joshsim.engine.value.converter.TransitiveConversion;
import org.joshsim.engine.value.converter.Units;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test transitive conversion construction.
 */
@ExtendWith(MockitoExtension.class)
public class ConverterBuilderTest {

  @Mock(lenient = true) private Conversion kilometersToMetersConversion;
  @Mock(lenient = true) private Conversion metersToCentimetersConversion;

  private Units kilometersUnits;
  private Units metersUnits;
  private Units centimetersUnits;
  private ConverterBuilder builder;

  /**
   * Setup example converter builder with kilometers -> meters and meters -> centimeters.
   */
  @BeforeEach
  void setUp() {
    kilometersUnits = new Units("km");
    metersUnits = new Units("m");
    centimetersUnits = new Units("cm");

    when(kilometersToMetersConversion.getSourceUnits()).thenReturn(kilometersUnits);
    when(kilometersToMetersConversion.getDestinationUnits()).thenReturn(metersUnits);

    when(metersToCentimetersConversion.getSourceUnits()).thenReturn(metersUnits);
    when(metersToCentimetersConversion.getDestinationUnits()).thenReturn(centimetersUnits);

    builder = new ConverterBuilder();
    builder.addConversion(kilometersToMetersConversion);
    builder.addConversion(metersToCentimetersConversion);
  }

  @Test
  void testKilometersToMetersConversion() {
    Converter converter = builder.build();
    Conversion result = converter.getConversion(kilometersUnits, metersUnits);
    assertEquals(kilometersToMetersConversion, result);
  }

  @Test
  void testKilometersToCentimetersTransitiveConversion() {
    Converter converter = builder.build();
    Conversion result = converter.getConversion(kilometersUnits, centimetersUnits);
    // The result should be a transitive conversion since it requires two steps
    assertTrue(result instanceof TransitiveConversion);
  }
}
