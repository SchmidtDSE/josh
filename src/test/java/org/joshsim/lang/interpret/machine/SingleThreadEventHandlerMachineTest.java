
/**
 * Tests for SingleThreadEventHandlerMachine.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.simulation.Query;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.DecimalScalar;
import org.joshsim.engine.value.type.Distribution;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.engine.value.type.RealizedDistribution;
import org.joshsim.lang.interpret.ValueResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


/**
 * Tests for the SingleThreadEventHandlerMachine implementation.
 */
@ExtendWith(MockitoExtension.class)
public class SingleThreadEventHandlerMachineTest {

  @Mock(lenient = true) private Scope mockScope;
  @Mock(lenient = true) private EngineValue mockValue;
  @Mock(lenient = true) private Geometry mockGeometry;
  @Mock(lenient = true) private Entity mockEntity;
  @Mock(lenient = true) private Query mockQuery;
  @Mock(lenient = true) private Distribution mockDistribution;

  private SingleThreadEventHandlerMachine machine;

  /**
   * Setup test environment before each test.
   */
  @BeforeEach
  void setUp() {
    machine = new SingleThreadEventHandlerMachine(null, mockScope);
  }

  @Test
  void push_shouldPushValueOntoStack() {
    // When
    machine.push(mockValue);

    // Then
    machine.end();
    assertEquals(mockValue, machine.getResult());
  }

  @Test
  void end_shouldMarkMachineAsEnded() {
    // When
    machine.end();

    // Then
    assertTrue(machine.isEnded());
  }

  @Test
  void isEnded_shouldReturnFalseInitially() {
    // Then
    assertTrue(!machine.isEnded());
  }

  @Test
  void getResult_shouldReturnPushedValue() {
    // Given
    machine.push(mockValue);

    // Then
    machine.end();
    assertEquals(mockValue, machine.getResult());
  }

  @Test
  void add_shouldAddTwoValues() {
    // Given
    EngineValue value1 = makeIntScalar(5);
    EngineValue value2 = makeIntScalar(3);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.add();

    // Then
    machine.end();
    assertEquals(makeIntScalar(8), machine.getResult());
  }

  @Test
  void subtract_shouldSubtractTwoValues() {
    // Given
    EngineValue value1 = makeIntScalar(10);
    EngineValue value2 = makeIntScalar(4);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.subtract();

    // Then
    machine.end();
    assertEquals(makeIntScalar(6), machine.getResult());
  }

  @Test
  void multiply_shouldMultiplyTwoValues() {
    // Given
    EngineValue value1 = makeIntScalar(6);
    EngineValue value2 = makeIntScalar(7);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.multiply();

    // Then
    machine.end();
    assertEquals(makeIntScalar(42), machine.getResult());
  }

  @Test
  void divide_shouldDivideTwoValues() {
    // Given
    EngineValue value1 = makeIntScalar(15);
    EngineValue value2 = makeIntScalar(3);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.divide();

    // Then
    machine.end();
    assertEquals(makeIntScalar(5), machine.getResult());
  }

  @Test
  void pow_shouldRaiseToPower() {
    // Given
    EngineValue value1 = makeIntScalar(2);
    EngineValue value2 = makeIntScalar(3);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.pow();

    // Then
    machine.end();
    assertAlmostEquals(makeIntScalar(8), machine.getResult());
  }

  @Test
  void applyMap_shouldApplyMapOperation() {
    // Given
    EngineValue operand = makeIntScalar(5);      // Value to map
    EngineValue fromLow = makeIntScalar(0);      // Original range start
    EngineValue fromHigh = makeIntScalar(10);    // Original range end
    EngineValue toLow = makeIntScalar(0);        // Target range start
    EngineValue toHigh = makeIntScalar(100);     // Target range end

    // When - stack order: operand, fromLow, fromHigh, toLow, toHigh
    machine.push(operand);
    machine.push(fromLow);
    machine.push(fromHigh);
    machine.push(toLow);
    machine.push(toHigh);
    machine.applyMap("linear");

    // Then - 5 is 50% of way from 0 to 10, so result should be 50 (50% of way from 0 to 100)
    machine.end();
    assertAlmostEquals(makeIntScalar(50), machine.getResult());
  }

  @Test
  void and_shouldPerformLogicalAnd() {
    // Given
    EngineValue value1 = makeBoolScalar(true);
    EngineValue value2 = makeBoolScalar(false);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.and();

    // Then
    machine.end();
    assertEquals(makeBoolScalar(false), machine.getResult());
  }

  @Test
  void or_shouldPerformLogicalOr() {
    // Given
    EngineValue value1 = makeBoolScalar(true);
    EngineValue value2 = makeBoolScalar(false);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.or();

    // Then
    machine.end();
    assertEquals(makeBoolScalar(true), machine.getResult());
  }

  @Test
  void xor_shouldPerformLogicalXor() {
    // Given
    EngineValue value1 = makeBoolScalar(true);
    EngineValue value2 = makeBoolScalar(true);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.xor();

    // Then
    machine.end();
    assertEquals(makeBoolScalar(false), machine.getResult());
  }

  @Test
  void eq_shouldTestEquality() {
    // Given
    EngineValue value1 = makeIntScalar(5);
    EngineValue value2 = makeIntScalar(5);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.eq();

    // Then
    machine.end();
    assertEquals(makeBoolScalar(true), machine.getResult());
  }

  @Test
  void neq_shouldTestInequality() {
    // Given
    EngineValue value1 = makeIntScalar(5);
    EngineValue value2 = makeIntScalar(3);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.neq();

    // Then
    machine.end();
    assertEquals(makeBoolScalar(true), machine.getResult());
  }

  @Test
  void gt_shouldTestGreaterThan() {
    // Given
    EngineValue value1 = makeIntScalar(5);
    EngineValue value2 = makeIntScalar(3);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.gt();

    // Then
    machine.end();
    assertEquals(makeBoolScalar(true), machine.getResult());
  }

  @Test
  void gteq_shouldTestGreaterThanOrEqual() {
    // Given
    EngineValue value1 = makeIntScalar(5);
    EngineValue value2 = makeIntScalar(5);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.gteq();

    // Then
    machine.end();
    assertEquals(makeBoolScalar(true), machine.getResult());
  }

  @Test
  void lt_shouldTestLessThan() {
    // Given
    EngineValue value1 = makeIntScalar(3);
    EngineValue value2 = makeIntScalar(5);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.lt();

    // Then
    machine.end();
    assertEquals(makeBoolScalar(true), machine.getResult());
  }

  @Test
  void lteq_shouldTestLessThanOrEqual() {
    // Given
    EngineValue value1 = makeIntScalar(3);
    EngineValue value2 = makeIntScalar(3);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.lteq();

    // Then
    machine.end();
    assertEquals(makeBoolScalar(true), machine.getResult());
  }

  @Test
  void abs_shouldCalculateAbsoluteValue() {
    // Given
    EngineValue value = makeDecimalScalar(new BigDecimal("-5.5"));

    // When
    machine.push(value);
    machine.abs();

    // Then
    machine.end();
    assertAlmostEquals(makeDecimalScalar(new BigDecimal("5.5")), machine.getResult());
  }

  @Test
  void ceil_shouldRoundUpToNearestInteger() {
    // Given
    EngineValue value = makeDecimalScalar(new BigDecimal("5.3"));

    // When
    machine.push(value);
    machine.ceil();

    // Then
    machine.end();
    assertAlmostEquals(makeDecimalScalar(new BigDecimal("6")), machine.getResult());
  }

  @Test
  void floor_shouldRoundDownToNearestInteger() {
    // Given
    EngineValue value = makeDecimalScalar(new BigDecimal("5.7"));

    // When
    machine.push(value);
    machine.floor();

    // Then
    machine.end();
    assertAlmostEquals(makeDecimalScalar(new BigDecimal("5")), machine.getResult());
  }

  @Test
  void round_shouldRoundToNearestInteger() {
    // Given
    EngineValue value = makeDecimalScalar(new BigDecimal("5.7"));

    // When
    machine.push(value);
    machine.round();

    // Then
    machine.end();
    assertAlmostEquals(makeDecimalScalar(new BigDecimal("6")), machine.getResult());
  }

  @Test
  void log10_shouldCalculateBase10Logarithm() {
    // Given
    EngineValue value = makeDecimalScalar(new BigDecimal("100"));
    // When
    machine.push(value);
    machine.log10();

    // Then
    machine.end();
    assertAlmostEquals(makeDecimalScalar(new BigDecimal("2")), machine.getResult());
  }

  @Test
  void ln_shouldCalculateNaturalLogarithm() {
    // Given
    EngineValue value = makeDecimalScalar(new BigDecimal("1"));

    // When
    machine.push(value);
    machine.ln();

    // Then
    machine.end();
    assertAlmostEquals(makeDecimalScalar(new BigDecimal("0")), machine.getResult());
  }

  @Test
  void count_shouldReturnDistributionSize() {
    // Given
    List<EngineValue> values = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      values.add(makeIntScalar(i));
    }
    EngineValue distribution = new RealizedDistribution(null, values, Units.EMPTY);

    // When
    machine.push(distribution);
    machine.count();

    // Then
    machine.end();
    assertEquals(makeIntScalar(5L), machine.getResult());
  }

  @Test
  void max_shouldReturnMaximumValue() {
    // Given
    List<EngineValue> values = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      values.add(makeIntScalar(i));
    }
    EngineValue distribution = new RealizedDistribution(null, values, Units.EMPTY);

    // When
    machine.push(distribution);
    machine.max();

    // Then
    machine.end();
    assertAlmostEquals(makeIntScalar(5L), machine.getResult());
  }

  @Test
  void mean_shouldReturnAverageValue() {
    // Given
    ArrayList<EngineValue> values = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      values.add(makeIntScalar(i));
    }
    EngineValue distribution = new RealizedDistribution(null, values, Units.EMPTY);

    // When
    machine.push(distribution);
    machine.mean();

    // Then
    machine.end();
    assertAlmostEquals(makeDecimalScalar(new BigDecimal("3.0")), machine.getResult());
  }

  @Test
  void min_shouldReturnMinimumValue() {
    // Given
    ArrayList<EngineValue> values = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      values.add(makeIntScalar(1));
    }
    EngineValue distribution = new RealizedDistribution(null, values, Units.EMPTY);

    // When
    machine.push(distribution);
    machine.min();

    // Then
    machine.end();
    assertAlmostEquals(makeIntScalar(1L), machine.getResult());
  }

  @Test
  void std_shouldReturnStandardDeviation() {
    // Given
    ArrayList<EngineValue> values = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      values.add(makeIntScalar(i));
    }
    EngineValue distribution = new RealizedDistribution(null, values, Units.EMPTY);

    // When
    machine.push(distribution);
    machine.std();

    // Then
    machine.end();
    DecimalScalar result = (DecimalScalar) machine.getResult();
    assertTrue(Math.abs(result.getAsDecimal().doubleValue() - 1.4142) < 0.0001);
  }

  @Test
  void sum_shouldReturnTotalValue() {
    // Given
    ArrayList<EngineValue> values = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      values.add(makeIntScalar(i));
    }
    EngineValue distribution = new RealizedDistribution(null, values, Units.EMPTY);

    // When
    machine.push(distribution);
    machine.sum();

    // Then
    machine.end();
    assertAlmostEquals(makeIntScalar(15L), machine.getResult());
  }

  @Test
  void bound_shouldConstrainValueWithinRange() {
    // Given
    EngineValue value = makeIntScalar(15L);
    EngineValue min = makeIntScalar(0L);
    EngineValue max = makeIntScalar(10L);

    // When
    machine.push(value);
    machine.push(min);
    machine.push(max);
    machine.bound(true, true);

    // Then
    machine.end();
    assertEquals(makeIntScalar(10L), machine.getResult());
  }

  @Test
  void bound_shouldNotConstrainValueWithinRange() {
    // Given
    EngineValue value = makeIntScalar(5L);
    EngineValue min = makeIntScalar(0L);
    EngineValue max = makeIntScalar(10L);

    // When
    machine.push(value);
    machine.push(min);
    machine.push(max);
    machine.bound(true, true);

    // Then
    machine.end();
    assertEquals(makeIntScalar(5L), machine.getResult());
  }

  @Test
  void bound_shouldConstrainValueWithLowerBoundOnly() {
    // Given
    EngineValue value = makeIntScalar(-5L);
    EngineValue min = makeIntScalar(0L);

    // When
    machine.push(value);
    machine.push(min);
    machine.bound(true, false);

    // Then
    machine.end();
    assertEquals(makeIntScalar(0L), machine.getResult());
  }

  @Test
  void randUniform_shouldGenerateNumberWithinRange() {
    // Given
    EngineValue low = makeDecimalScalar(new BigDecimal("0.0"));
    EngineValue high = makeDecimalScalar(new BigDecimal("10.0"));

    // When
    machine.push(low);
    machine.push(high);
    machine.randUniform();

    // Then
    machine.end();
    DecimalScalar result = (DecimalScalar) machine.getResult();
    BigDecimal value = result.getAsDecimal();
    assertTrue(value.compareTo(new BigDecimal("0.0")) >= 0);
    assertTrue(value.compareTo(new BigDecimal("10.0")) <= 0);
  }

  @Test
  void randNorm_shouldGenerateNumberFromNormalDistribution() {
    // Given
    EngineValue mean = makeDecimalScalar(new BigDecimal("5.0"));
    EngineValue stdDev = makeDecimalScalar(new BigDecimal("1.0"));



  @Test
  void condition_shouldExecuteActionWhenConditionIsTrue() {
    // Given
    EngineValue condition = makeBoolScalar(true);
    EventHandlerAction positiveAction = machine -> {
      machine.push(makeIntScalar(42));
      return machine;
    };

    // When
    machine.push(condition);
    machine.condition(positiveAction);

    // Then
    machine.end();
    assertEquals(makeIntScalar(42), machine.getResult());
  }

  @Test
  void condition_shouldNotExecuteActionWhenConditionIsFalse() {
    // Given
    EngineValue condition = makeBoolScalar(false);
    EventHandlerAction positiveAction = machine -> {
      machine.push(makeIntScalar(42));
      return machine;
    };

    // When
    machine.push(condition);
    machine.condition(positiveAction);

    // Then
    machine.end();
    assertTrue(machine.isEnded());
  }


    // When
    machine.push(mean);
    machine.push(stdDev);
    machine.randNorm();

    // Then
    machine.end();
    DecimalScalar result = (DecimalScalar) machine.getResult();
    BigDecimal value = result.getAsDecimal();

    // Most values in a normal distribution fall within 3 standard deviations
    assertTrue(value.compareTo(new BigDecimal("2.0")) >= 0); // mean - 3*stdDev
    assertTrue(value.compareTo(new BigDecimal("8.0")) <= 0); // mean + 3*stdDev
  }

  @Test
  void saveLocalVariable_makesVisible() {
    // Given
    EngineValue value = makeIntScalar(5L);

    // When
    machine.push(value);
    machine.saveLocalVariable("localConstant");
    machine.push(new ValueResolver("localConstant"));

    // Then
    machine.end();
    EngineValue result = machine.getResult();
    long valueReturned = result.getAsInt();

    // Check value returned.
    assertEquals(valueReturned, 5L);
  }

  private EngineValue makeIntScalar(long value) {
    EngineValueFactory factory = new EngineValueFactory();
    return factory.build(value, Units.EMPTY);
  }

  private EngineValue makeBoolScalar(boolean value) {
    EngineValueFactory factory = new EngineValueFactory();
    return factory.build(value, Units.EMPTY);
  }

  private EngineValue makeDecimalScalar(BigDecimal value) {
    EngineValueFactory factory = new EngineValueFactory();
    return factory.build(value, Units.EMPTY);
  }

  private void assertAlmostEquals(EngineValue engineValue, EngineValue result) {
    BigDecimal expected = engineValue.getAsDecimal();
    BigDecimal actual = result.getAsDecimal();
    BigDecimal tolerance = new BigDecimal("0.0001");
    assertTrue(expected.subtract(actual).abs().compareTo(tolerance) <= 0,
        "Expected " + expected + " but got " + actual);
  }
}
