package org.joshsim.engine.value.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.joshsim.engine.func.CompiledCallable;
import org.junit.jupiter.api.Test;


class InverseConversionTest {

  @Test
  void testGetConversionCallableShouldReturnInnerCallable() {
    // Arrange
    Conversion mockInner = mock(Conversion.class);
    CompiledCallable mockCallable = mock(CompiledCallable.class);
    when(mockInner.getConversionCallable()).thenReturn(mockCallable);
    InverseConversion inverseConversion = new InverseConversion(mockInner);

    // Act
    CompiledCallable result = inverseConversion.getConversionCallable();

    // Assert
    assertEquals(mockCallable, result);
  }

  @Test
  void testGetSourceUnitsShouldSwapUnits() {
    // Arrange
    Conversion mockInner = mock(Conversion.class);
    Units mockSourceUnits = mock(Units.class);
    Units mockDestinationUnits = mock(Units.class);
    when(mockInner.getDestinationUnits()).thenReturn(mockSourceUnits);
    when(mockInner.getSourceUnits()).thenReturn(mockDestinationUnits);
    InverseConversion inverseConversion = new InverseConversion(mockInner);

    // Act
    Units resultSourceUnits = inverseConversion.getSourceUnits();

    // Assert
    assertEquals(mockSourceUnits, resultSourceUnits);
  }

  @Test
  void testGetDestinationUnitsShouldSwapUnits() {
    // Arrange
    Conversion mockInner = mock(Conversion.class);
    Units mockSourceUnits = mock(Units.class);
    Units mockDestinationUnits = mock(Units.class);
    when(mockInner.getSourceUnits()).thenReturn(mockDestinationUnits);
    when(mockInner.getDestinationUnits()).thenReturn(mockSourceUnits);
    InverseConversion inverseConversion = new InverseConversion(mockInner);

    // Act
    Units resultDestinationUnits = inverseConversion.getDestinationUnits();

    // Assert
    assertEquals(mockDestinationUnits, resultDestinationUnits);
  }

  @Test
  void testIsCommunicativeSafeShouldReturnInnerResult() {
    // Arrange
    Conversion mockInner = mock(Conversion.class);
    when(mockInner.isCommunicativeSafe()).thenReturn(true);
    InverseConversion inverseConversion = new InverseConversion(mockInner);

    // Act
    boolean result = inverseConversion.isCommunicativeSafe();

    // Assert
    assertTrue(result);
  }
}
