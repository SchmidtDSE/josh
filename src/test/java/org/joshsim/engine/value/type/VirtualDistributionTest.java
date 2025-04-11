package org.joshsim.engine.value.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VirtualDistributionTest {

  @Mock
  private EngineValueCaster mockCaster;

  @Mock
  private Units mockUnits;

  @Mock
  private EngineValue mockOtherValue;

  private TestVirtualDistribution virtualDistribution;
  private Scalar testScalar;

  /**
   * Concrete implementation for testing the abstract VirtualDistribution class.
   */
  private static class TestVirtualDistribution extends VirtualDistribution {
    private final Scalar sampleValue;

    public TestVirtualDistribution(EngineValueCaster caster, Units units, Scalar sampleValue) {
      super(caster, units);
      this.sampleValue = sampleValue;
    }

    @Override
    public EngineValue sample() {
      return sampleValue;
    }
  }

  @BeforeEach
  void setUp() {
    testScalar = mock(Scalar.class);
    // when(testScalar.getAsDecimal()).thenReturn(BigDecimal.TEN);
    virtualDistribution = new TestVirtualDistribution(mockCaster, mockUnits, testScalar);
  }

  @Test
  void testGetContents() {
    // When
    List<EngineValue> contents = virtualDistribution.getContents(3, true);

    // Then
    assertEquals(3, contents.size());
    contents.forEach(value -> assertEquals(testScalar, value));
  }

  @Test
  void testGetInnerValue() {
    assertThrows(UnsupportedOperationException.class,
        () -> virtualDistribution.getInnerValue());
  }

  @Test
  void testSampleMultiple() {
    // When
    Distribution result = virtualDistribution.sampleMultiple(5, true);

    // Then
    assertNotNull(result);
    assertTrue(result instanceof RealizedDistribution);

    // Instead of verifying a non-existent method call,
    // verify the distribution contains the expected values
    Iterable<EngineValue> values = result.getContents(5, false);
    int count = 0;
    for (EngineValue value : values) {
      assertEquals(testScalar, value);
      count++;
    }
    assertEquals(5, count);
    values.forEach(value -> assertEquals(testScalar, value));
  }

  @Test
  void testRealize() {
    // When
    RealizedDistribution realized = virtualDistribution.realize(10, false);

    // Then
    assertNotNull(realized);

    // Instead of verifying a method call to the caster,
    // verify the distribution contains the expected samples
    Iterable<EngineValue> values = realized.getContents(10, false);

    // Verify the correct number of samples were generated
    int count = 0;
    for (EngineValue value : values) {
      assertEquals(testScalar, value);
      count++;
    }
    assertEquals(10, count);
    values.forEach(value -> assertEquals(testScalar, value));
  }

  @Test
  void testRealizeToMatchOther() {
    // Given
    when(mockOtherValue.getSize()).thenReturn(Optional.of(5));

    // When
    RealizedDistribution realized = virtualDistribution.realizeToMatchOther(mockOtherValue);

    // Then
    assertNotNull(realized);

    // Verify the correct number of samples were generated
    Iterable<EngineValue> values = realized.getContents(5, false);
    int count = 0;
    for (EngineValue value : values) {
      assertEquals(testScalar, value);
      count++;
    }

    assertEquals(5, count);
    values.forEach(value -> assertEquals(testScalar, value));
  }

  @Test
  void testStatisticalMethodsThrowUnsupported() {
    // These methods should all throw UnsupportedOperationException
    assertThrows(UnsupportedOperationException.class, () -> virtualDistribution.getMean());
    assertThrows(UnsupportedOperationException.class, () -> virtualDistribution.getStd());
    assertThrows(UnsupportedOperationException.class, () -> virtualDistribution.getMin());
    assertThrows(UnsupportedOperationException.class, () -> virtualDistribution.getMax());
    assertThrows(UnsupportedOperationException.class, () -> virtualDistribution.getSum());
    assertThrows(UnsupportedOperationException.class, () ->
        virtualDistribution.replaceUnits(mockUnits));
  }

  @Test
  void testGetAsScalarReturnsSample() {
    // When
    when(testScalar.getAsScalar()).thenReturn(testScalar);

    Scalar result = virtualDistribution.getAsScalar();

    // Then
    assertSame(testScalar, result);
  }

  @Test
  void testUnsafeOperationMethods() {
    // Setup
    RealizedDistribution mockRealized = mock(RealizedDistribution.class);
    when(mockOtherValue.getSize()).thenReturn(Optional.of(1));

    // For any operation, we should realize and then delegate
    // Testing just a couple operations as an example
    virtualDistribution.unsafeAdd(mockOtherValue);
    virtualDistribution.unsafeMultiply(mockOtherValue);

    // Verify the distribution is realized for each operation
    verify(mockOtherValue, times(2)).getSize();
  }
}
