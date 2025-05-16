/**
 * Tests for RealizedDistribution.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;
import org.joshsim.engine.value.engine.EngineValueFactory;
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
    EngineValueFactory valueFactory = CompatibilityLayerKeeper.get().getEngineValueFactory();
    caster = new EngineValueWideningCaster(valueFactory);
    values = new ArrayList<>();
    nakedValues = new ArrayList<>();

    // Add some test values (integers 1-5)
    for (int i = 1; i <= 5; i++) {
      values.add(new IntScalar(caster, (long) i, Units.of("m")));
      nakedValues.add(new IntScalar(caster, (long) i, Units.EMPTY));
    }

    distribution = new RealizedDistribution(caster, values, Units.of("m"));
    nakedDistribution = new RealizedDistribution(caster, values, Units.EMPTY);
  }

  @Test
  void testConstructorAndGetters() {
    assertEquals(new LanguageType("RealizedDistribution"), distribution.getLanguageType());
    assertEquals(Units.of("m"), distribution.getUnits());
    assertSame(values, distribution.getInnerValue());
    assertEquals(Optional.of(5), distribution.getSize());
  }

  @Test
  void testAdd() {
    IntScalar addend = new IntScalar(caster, 10L, Units.of("m"));
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

    assertEquals(Units.of("m"), result.getUnits());
  }

  @Test
  void testAddReverse() {
    IntScalar addend = new IntScalar(caster, 10L, Units.of("m"));
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

    assertEquals(Units.of("m"), result.getUnits());
  }

  @Test
  void testSubtract() {
    IntScalar subtrahend = new IntScalar(caster, 1L, Units.of("m"));
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

    assertEquals(Units.of("m"), result.getUnits());
  }

  @Test
  void testSubtractReverse() {
    IntScalar subtrahend = new IntScalar(caster, 1L, Units.of("m"));
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

    assertEquals(Units.of("m"), result.getUnits());
  }

  @Test
  void testMultiply() {
    IntScalar multiplier = new IntScalar(caster, 2L, Units.of("s"));
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

    assertEquals(Units.of("m * s"), result.getUnits());
  }

  void testMultiplyReverse() {
    IntScalar multiplier = new IntScalar(caster, 2L, Units.of("s"));
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

    assertEquals(Units.of("m*s"), result.getUnits());
  }

  @Test
  void testDivide() {
    IntScalar divisor = new IntScalar(caster, 2L, Units.of("s"));
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

    assertEquals(Units.of("m / s"), result.getUnits());
  }

  @Test
  void testDivideReverse() {
    IntScalar divisor = new IntScalar(caster, 2L, Units.of("s"));
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

    assertEquals(Units.of("s / m"), result.getUnits());
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

    assertEquals(Units.of("m * m"), result.getUnits());
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
    assertThrows(
        IllegalArgumentException.class,
        () -> distribution.getContents(10, false)
    );
  }

  @Test
  void testGetMean() {
    Optional<Scalar> mean = distribution.getMean();

    assertTrue(mean.isPresent());
    assertTrue(mean.get() instanceof DecimalScalar);

    DecimalScalar meanScalar = (DecimalScalar) mean.get();
    assertEquals(3.0, meanScalar.getAsDecimal().doubleValue(), 0.0001);
    assertEquals(Units.of("m"), meanScalar.getUnits());
  }

  @Test
  void testGetStd() {
    assertEquals(
        1.5811,
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

    RealizedDistribution distribution = new RealizedDistribution(
        caster,
        emptyValues,
        Units.of("m")
    );

    assertThrows(IllegalArgumentException.class, () -> distribution.getMean());
  }

  @Test
  void testSample() {
    // Since sampling is random, we'll verify that multiple samples fall within expected range
    for (int i = 0; i < 100; i++) {
      EngineValue result = distribution.sample();
      assertTrue(result instanceof IntScalar);
      IntScalar scalar = (IntScalar) result;
      // Values should be between 1 and 5 inclusive
      assertTrue(scalar.getAsInt() >= 1 && scalar.getAsInt() <= 5);
      assertEquals(Units.of("m"), scalar.getUnits());
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
      assertEquals(Units.of("m"), scalar.getUnits());
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

  @Test
  void testFreeze() {
    RealizedDistribution distributionWithMutable = new RealizedDistribution(
        caster,
        values,
        Units.EMPTY
    );

    // Freeze the distribution
    EngineValue frozenResult = distributionWithMutable.freeze();

    // Verify the result is a RealizedDistribution
    assertTrue(frozenResult instanceof RealizedDistribution);
    RealizedDistribution frozenDistribution = (RealizedDistribution) frozenResult;

    // Verify the frozen distribution has the same size
    assertEquals(distributionWithMutable.getSize(), frozenDistribution.getSize());

    // Verify the units are preserved
    assertEquals(distributionWithMutable.getUnits(), frozenDistribution.getUnits());

    // Check that changes do not propogate
    values.add(new IntScalar(caster, 0L, Units.of("m")));

    // Verify the frozen distribution has the same size
    assertNotEquals(distributionWithMutable.getSize(), frozenDistribution.getSize());
  }

}
