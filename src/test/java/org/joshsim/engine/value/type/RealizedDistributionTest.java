/**
 * Tests for RealizedDistribution.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;
import org.joshsim.engine.value.engine.EngineValueWideningCaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Tests for a distribution which has a discrete number of elements.
 */
class RealizedDistributionTest {

  private EngineValueCaster caster;
  private ArrayList<EngineValue> values;
  private ArrayList<EngineValue> nakedValues;
  private RealizedDistribution distribution;
  private RealizedDistribution nakedDistribution;

  @BeforeEach
  void setUp() {
    caster = new EngineValueWideningCaster();
    values = new ArrayList<>();
    nakedValues = new ArrayList<>();

    // Add some test values (integers 1-5)
    for (int i = 1; i <= 5; i++) {
      values.add(new IntScalar(caster, (long) i, new Units("m")));
      nakedValues.add(new IntScalar(caster, (long) i, Units.EMPTY));
    }

    distribution = new RealizedDistribution(caster, values, new Units("m"));
    nakedDistribution = new RealizedDistribution(caster, values, Units.EMPTY);
  }

  @Test
  void testConstructorAndGetters() {
    assertEquals(new LanguageType("RealizedDistribution"), distribution.getLanguageType());
    assertEquals(new Units("m"), distribution.getUnits());
    assertSame(values, distribution.getInnerValue());
    assertEquals(Optional.of(5), distribution.getSize());
  }

  @Test
  void testAdd() {
    IntScalar addend = new IntScalar(caster, 10L, new Units("m"));
    RealizedDistribution result = (RealizedDistribution) distribution.add(addend);

    // Fix unchecked cast warning by using a safer approach
    Object innerValue = result.getInnerValue();
    assertTrue(innerValue instanceof ArrayList<?>);
    ArrayList<?> resultValues = (ArrayList<?>) innerValue;
    assertEquals(5, resultValues.size());

    // Check each value has been incremented by 10
    for (int i = 0; i < 5; i++) {
      Object value = resultValues.get(i);
      assertTrue(value instanceof IntScalar);
      IntScalar scalar = (IntScalar) value;
      assertEquals(i + 11, scalar.getAsInt());
    }

    assertEquals(new Units("m"), result.getUnits());
  }

  @Test
  void testAddReverse() {
    IntScalar addend = new IntScalar(caster, 10L, new Units("m"));
    RealizedDistribution result = (RealizedDistribution) addend.add(distribution);

    // Fix unchecked cast warning by using a safer approach
    Object innerValue = result.getInnerValue();
    assertTrue(innerValue instanceof ArrayList<?>);
    ArrayList<?> resultValues = (ArrayList<?>) innerValue;
    assertEquals(5, resultValues.size());

    // Check each value has been incremented by 10
    for (int i = 0; i < 5; i++) {
      Object value = resultValues.get(i);
      assertTrue(value instanceof IntScalar);
      IntScalar scalar = (IntScalar) value;
      assertEquals(i + 11, scalar.getAsInt());
    }

    assertEquals(new Units("m"), result.getUnits());
  }

  @Test
  void testSubtract() {
    IntScalar subtrahend = new IntScalar(caster, 1L, new Units("m"));
    RealizedDistribution result = (RealizedDistribution) distribution.subtract(subtrahend);

    Object innerValue = result.getInnerValue();
    assertTrue(innerValue instanceof ArrayList<?>);
    ArrayList<?> resultValues = (ArrayList<?>) innerValue;
    assertEquals(5, resultValues.size());

    // Check each value has been decremented by 1
    for (int i = 0; i < 5; i++) {
      Object value = resultValues.get(i);
      assertTrue(value instanceof IntScalar);
      IntScalar scalar = (IntScalar) value;
      assertEquals(i, scalar.getAsInt());
    }

    assertEquals(new Units("m"), result.getUnits());
  }

  @Test
  void testSubtractReverse() {
    IntScalar subtrahend = new IntScalar(caster, 1L, new Units("m"));
    RealizedDistribution result = (RealizedDistribution) subtrahend.subtract(distribution);

    Object innerValue = result.getInnerValue();
    assertTrue(innerValue instanceof ArrayList<?>);
    ArrayList<?> resultValues = (ArrayList<?>) innerValue;
    assertEquals(5, resultValues.size());

    // Check each value has been decremented by 1
    for (int i = 0; i < 5; i++) {
      Object value = resultValues.get(i);
      assertTrue(value instanceof IntScalar);
      IntScalar scalar = (IntScalar) value;
      assertEquals(i, scalar.getAsInt());
    }

    assertEquals(new Units("m"), result.getUnits());
  }

  @Test
  void testMultiply() {
    IntScalar multiplier = new IntScalar(caster, 2L, new Units("s"));
    RealizedDistribution result = (RealizedDistribution) distribution.multiply(multiplier);

    Object innerValue = result.getInnerValue();
    assertTrue(innerValue instanceof ArrayList<?>);
    ArrayList<?> resultValues = (ArrayList<?>) innerValue;
    assertEquals(5, resultValues.size());

    // Check each value has been multiplied by 2
    for (int i = 0; i < 5; i++) {
      Object value = resultValues.get(i);
      assertTrue(value instanceof IntScalar);
      IntScalar scalar = (IntScalar) value;
      assertEquals((i + 1) * 2, scalar.getAsInt());
    }

    assertEquals(new Units("m*s"), result.getUnits());
  }

  void testMultiplyReverse() {
    IntScalar multiplier = new IntScalar(caster, 2L, new Units("s"));
    RealizedDistribution result = (RealizedDistribution) multiplier.multiply(distribution);

    Object innerValue = result.getInnerValue();
    assertTrue(innerValue instanceof ArrayList<?>);
    ArrayList<?> resultValues = (ArrayList<?>) innerValue;
    assertEquals(5, resultValues.size());

    // Check each value has been multiplied by 2
    for (int i = 0; i < 5; i++) {
      Object value = resultValues.get(i);
      assertTrue(value instanceof IntScalar);
      IntScalar scalar = (IntScalar) value;
      assertEquals((i + 1) * 2, scalar.getAsInt());
    }

    assertEquals(new Units("m*s"), result.getUnits());
  }

  @Test
  void testDivide() {
    IntScalar divisor = new IntScalar(caster, 2L, new Units("s"));
    RealizedDistribution result = (RealizedDistribution) distribution.divide(divisor);

    Object innerValue = result.getInnerValue();
    assertTrue(innerValue instanceof ArrayList<?>);
    ArrayList<?> resultValues = (ArrayList<?>) innerValue;
    assertEquals(5, resultValues.size());

    // Check each value has been divided by 2
    for (int i = 0; i < 5; i++) {
      Object value = resultValues.get(i);
      assertTrue(value instanceof IntScalar);
      IntScalar scalar = (IntScalar) value;
      assertEquals((i + 1) / 2, scalar.getAsInt());
    }

    assertEquals(new Units("m / s"), result.getUnits());
  }

  @Test
  void testDivideReverse() {
    IntScalar divisor = new IntScalar(caster, 2L, new Units("s"));
    RealizedDistribution result = (RealizedDistribution) divisor.divide(distribution);

    Object innerValue = result.getInnerValue();
    assertTrue(innerValue instanceof ArrayList<?>);
    ArrayList<?> resultValues = (ArrayList<?>) innerValue;
    assertEquals(5, resultValues.size());

    // Check each value has been divided by 2
    for (int i = 0; i < 5; i++) {
      Object value = resultValues.get(i);
      assertTrue(value instanceof IntScalar);
      IntScalar scalar = (IntScalar) value;
      assertEquals((int) (2 / (i + 1)), scalar.getAsInt());
    }

    assertEquals(new Units("s / m"), result.getUnits());
  }

  @Test
  void testRaiseToPower() {
    IntScalar exponent = new IntScalar(caster, 2L, Units.EMPTY);
    RealizedDistribution result = (RealizedDistribution) distribution.raiseToPower(exponent);

    Object innerValue = result.getInnerValue();
    assertTrue(innerValue instanceof ArrayList<?>);
    ArrayList<?> resultValues = (ArrayList<?>) innerValue;
    assertEquals(5, resultValues.size());

    // Check each value has been squared
    for (int i = 0; i < 5; i++) {
      Object value = resultValues.get(i);
      assertTrue(value instanceof DecimalScalar);
      DecimalScalar scalar = (DecimalScalar) value;
      assertEquals(new BigDecimal((i + 1) * (i + 1)), scalar.getAsDecimal());
    }

    assertEquals(new Units("m * m"), result.getUnits());
  }

  @Test
  void testRaiseToPowerReverse() {
    IntScalar exponent = new IntScalar(caster, 2L, Units.EMPTY);
    RealizedDistribution result = (RealizedDistribution) exponent.raiseToPower(nakedDistribution);

    Object innerValue = result.getInnerValue();
    assertTrue(innerValue instanceof ArrayList<?>);
    ArrayList<?> resultValues = (ArrayList<?>) innerValue;
    assertEquals(5, resultValues.size());

    // Check each value has been squared
    for (int i = 0; i < 5; i++) {
      Object value = resultValues.get(i);
      assertTrue(value instanceof DecimalScalar);
      DecimalScalar scalar = (DecimalScalar) value;
      assertEquals(new BigDecimal((i + 1) * (i + 1)), scalar.getAsDecimal());
    }

    assertEquals(Units.EMPTY, result.getUnits());
  }

  @Test
  void testGetAsScalar() {
    assertThrows(UnsupportedOperationException.class, () -> distribution.getAsScalar());
  }

  @Test
  void testGetAsDistribution() {
    assertSame(distribution, distribution.getAsDistribution());
  }

  @Test
  void testGetContentsWithReplacement() {
    Iterable<EngineValue> result = distribution.getContents(10, true);
    ArrayList<EngineValue> resultList = new ArrayList<>();
    result.forEach(resultList::add);

    // Should return 10 items with replacement (cycling through the 5 values)
    assertEquals(10, resultList.size());

    for (int i = 0; i < 10; i++) {
      Object value = resultList.get(i);
      assertTrue(value instanceof IntScalar);
      IntScalar scalar = (IntScalar) value;
      assertEquals((i % 5) + 1, scalar.getAsInt());
    }
  }

  @Test
  void testGetContentsWithoutReplacement() {
    Iterable<EngineValue> result = distribution.getContents(3, false);
    ArrayList<EngineValue> resultList = new ArrayList<>();
    result.forEach(resultList::add);

    // Should return only the first 3 items
    assertEquals(3, resultList.size());

    for (int i = 0; i < 3; i++) {
      Object value = resultList.get(i);
      assertTrue(value instanceof IntScalar);
      IntScalar scalar = (IntScalar) value;
      assertEquals(i + 1, scalar.getAsInt());
    }
  }

  @Test
  void testGetContentsLimitedWithoutReplacement() {
    // Request more items than available without replacement
    Iterable<EngineValue> result = distribution.getContents(10, false);
    ArrayList<EngineValue> resultList = new ArrayList<>();
    result.forEach(resultList::add);

    // Should only return the 5 available items
    assertEquals(5, resultList.size());

    for (int i = 0; i < 5; i++) {
      Object value = resultList.get(i);
      assertTrue(value instanceof IntScalar);
      IntScalar scalar = (IntScalar) value;
      assertEquals(i + 1, scalar.getAsInt());
    }
  }

  @Test
  void testGetMean() {
    Optional<Scalar> mean = distribution.getMean();

    assertTrue(mean.isPresent());
    assertTrue(mean.get() instanceof DecimalScalar);

    DecimalScalar meanScalar = (DecimalScalar) mean.get();
    assertEquals(3.0, meanScalar.getAsDecimal().doubleValue(), 0.0001);
    assertEquals(new Units("m"), meanScalar.getUnits());
  }

  @Test
  void testGetStd() {
    assertEquals(
        1.4142,
        distribution.getStd().orElseThrow().getAsDecimal().doubleValue(),
        0.0001
    );
  }

  @Test
  void testGetMin() {
    assertEquals(1, distribution.getMin().orElseThrow().getAsInt());
  }

  @Test
  void testGetMax() {
    assertEquals(5, distribution.getMax().orElseThrow().getAsInt());
  }

  @Test
  void testGetSum() {
    assertEquals(1 + 2 + 3 + 4 + 5, distribution.getSum().orElseThrow().getAsInt());
  }

  @Test
  void testEmptyDistribution() {
    ArrayList<EngineValue> emptyValues = new ArrayList<>();

    assertThrows(IllegalArgumentException.class, () -> new RealizedDistribution(
        caster,
        emptyValues,
        new Units("m")
    ));
  }

  @Test
  void testSample() {
    // Since sampling is random, we'll verify that multiple samples fall within expected range
    for (int i = 0; i < 100; i++) {
      Scalar result = distribution.sample();
      assertTrue(result instanceof IntScalar);
      IntScalar scalar = (IntScalar) result;
      // Values should be between 1 and 5 inclusive
      assertTrue(scalar.getAsInt() >= 1 && scalar.getAsInt() <= 5);
      assertEquals(new Units("m"), scalar.getUnits());
    }
  }

  @Test
  void testSampleMultipleWithReplacement() {
    long sampleCount = 10;
    Distribution result = distribution.sampleMultiple(sampleCount, true);
    assertTrue(result instanceof RealizedDistribution);

    // Verify size matches requested count
    assertEquals(Optional.of((int) sampleCount), result.getSize());

    // Verify all sampled values are within expected range
    Object innerValue = result.getInnerValue();
    assertTrue(innerValue instanceof ArrayList<?>);
    ArrayList<?> resultValues = (ArrayList<?>) innerValue;

    for (Object value : resultValues) {
      assertTrue(value instanceof IntScalar);
      IntScalar scalar = (IntScalar) value;
      assertTrue(scalar.getAsInt() >= 1 && scalar.getAsInt() <= 5);
      assertEquals(new Units("m"), scalar.getUnits());
    }
  }

  @Test
  void testSampleMultipleWithoutReplacement() {
    long sampleCount = 3;
    Distribution result = distribution.sampleMultiple(sampleCount, false);
    assertTrue(result instanceof RealizedDistribution);

    // Verify size matches requested count
    assertEquals(Optional.of((int) sampleCount), result.getSize());
  }

  @Test
  void testSampleMultipleWithoutReplacementExceedingSize() {
    // Attempting to sample more elements than available without replacement should throw exception
    assertThrows(IllegalArgumentException.class,
        () -> distribution.sampleMultiple(10, false));
  }

}
