package org.joshsim.engine.value.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.joshsim.engine.func.CompiledCallable;
import org.junit.jupiter.api.Test;


class InverseConversionTest {

  @Test
  void testGetConversionCallable_ShouldReturnInnerCallable() {
    // Arrange
    Conversion mockInner = mock(Conversion.class);
    CompiledCallable mockCallable = mock(CompiledCallable.class);
    when(mockInner.getConversionCallable()).thenReturn(mockCallable);
    InverseConversion inverseConversion = new InverseConversion(mockInner);

    // Act
    CompiledCallable result = inverseConversion.getConversionCallable();

    // Assert
    assertEquals(mockCallable, result, "Expected the callable from the inner Conversion to be returned.");
  }

  @Test
  void testGetSourceUnits_ShouldSwapUnits() {
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
    assertEquals(mockSourceUnits, resultSourceUnits, "Expected source units to be swapped with destination units.");
  }

  @Test
  void testGetDestinationUnits_ShouldSwapUnits() {
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
    assertEquals(mockDestinationUnits, resultDestinationUnits, "Expected destination units to be swapped with source units.");
  }

  @Test
  void testIsCommunicativeSafe_ShouldReturnInnerResult() {
    // Arrange
    Conversion mockInner = mock(Conversion.class);
    when(mockInner.isCommunicativeSafe()).thenReturn(true);
    InverseConversion inverseConversion = new InverseConversion(mockInner);

    // Act
    boolean result = inverseConversion.isCommunicativeSafe();

    // Assert
    assertTrue(result, "Expected the communicative safety to match the inner Conversion's result.");
  }
}