
/**
 * Tests for ConverterBuilder.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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

  @Mock(lenient = true) private Conversion kmToMeterConversion;
  @Mock(lenient = true) private Conversion mToCmConversion;
  
  private Units kmUnits;
  private Units mUnits;
  private Units cmUnits;
  private ConverterBuilder builder;

  /**
   * Setup example converter builder with km -> m and m -> cm.
   */
  @BeforeEach
  void setUp() {
    kmUnits = new Units("km");
    mUnits = new Units("m");
    cmUnits = new Units("cm");
    
    when(kmToMConversion.getSourceUnits()).thenReturn(kmUnits);
    when(kmToMConversion.getDestinationUnits()).thenReturn(mUnits);
    
    when(mToCmConversion.getSourceUnits()).thenReturn(mUnits);
    when(mToCmConversion.getDestinationUnits()).thenReturn(cmUnits);
    
    builder = new ConverterBuilder();
    builder.addConversion(kmToMConversion);
    builder.addConversion(mToCmConversion);
  }

  @Test
  void testKilometersToMetersConversion() {
    Converter converter = builder.build();
    Conversion result = converter.getConversion(kmUnits, mUnits);
    assertEquals(kmToMConversion, result);
  }

  @Test
  void testKilometersToCentimetersTransitiveConversion() {
    Converter converter = builder.build();
    Conversion result = converter.getConversion(kmUnits, cmUnits);
    // The result should be a transitive conversion since it requires two steps
    assertTrue(result instanceof TransitiveConversion);
  }
}
