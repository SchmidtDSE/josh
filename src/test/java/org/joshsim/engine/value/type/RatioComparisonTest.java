/**
 * Tests for ratio unit comparison with literal numbers.
 *
 * <p>This test is designed to investigate a bug where external data with
 * "ratio" units compared against literal numbers always evaluates to
 * the same result regardless of the actual data values.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueCaster;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.engine.EngineValueWideningCaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for comparing values with ratio units against values with count/empty units.
 */
class RatioComparisonTest {

  private EngineValueCaster caster;
  private EngineValueFactory factory;

  @BeforeEach
  void setUp() {
    factory = new EngineValueFactory();
    caster = new EngineValueWideningCaster(factory);
  }

  @Test
  void testRatioUnitsAreCreated() {
    Units ratioUnits = Units.of("ratio");
    Units countUnits = Units.of("count");

    assertEquals("ratio", ratioUnits.toString());
    assertTrue(countUnits.toString().isBlank(), "count units should be blank");
  }

  @Test
  void testRatioVsCountComparisonGreaterThan() {
    // Simulate external data with ratio units
    EngineValue ratioValue = new DoubleScalar(caster, 0.6, Units.of("ratio"));

    // Simulate literal number with count/empty units (how Josh parses "0.5")
    EngineValue countValue = new DoubleScalar(caster, 0.5, Units.of("count"));

    // 0.6 > 0.5 should be true
    EngineValue result = ratioValue.greaterThan(countValue);
    assertTrue(result.getAsBoolean(), "0.6 ratio > 0.5 count should be true");
  }

  @Test
  void testRatioVsCountComparisonGreaterThanFalse() {
    // Simulate external data with ratio units
    EngineValue ratioValue = new DoubleScalar(caster, 0.3, Units.of("ratio"));

    // Simulate literal number with count/empty units
    EngineValue countValue = new DoubleScalar(caster, 0.5, Units.of("count"));

    // 0.3 > 0.5 should be false
    EngineValue result = ratioValue.greaterThan(countValue);
    assertFalse(result.getAsBoolean(), "0.3 ratio > 0.5 count should be false");
  }

  @Test
  void testRatioVsEmptyComparisonGreaterThan() {
    // Simulate external data with ratio units
    EngineValue ratioValue = new DoubleScalar(caster, 0.6, Units.of("ratio"));

    // Simulate literal number with empty units
    EngineValue emptyValue = new DoubleScalar(caster, 0.5, Units.EMPTY);

    // 0.6 > 0.5 should be true
    EngineValue result = ratioValue.greaterThan(emptyValue);
    assertTrue(result.getAsBoolean(), "0.6 ratio > 0.5 empty should be true");
  }

  @Test
  void testRatioVsEmptyComparisonGreaterThanFalse() {
    // Simulate external data with ratio units
    EngineValue ratioValue = new DoubleScalar(caster, 0.3, Units.of("ratio"));

    // Simulate literal number with empty units
    EngineValue emptyValue = new DoubleScalar(caster, 0.5, Units.EMPTY);

    // 0.3 > 0.5 should be false
    EngineValue result = ratioValue.greaterThan(emptyValue);
    assertFalse(result.getAsBoolean(), "0.3 ratio > 0.5 empty should be false");
  }

  @Test
  void testCountVsCountComparisonGreaterThan() {
    // Both with count units (should work as per bug report)
    EngineValue count1 = new DoubleScalar(caster, 2.0, Units.of("count"));
    EngineValue count2 = new DoubleScalar(caster, 1.0, Units.of("count"));

    // 2 > 1 should be true
    EngineValue result = count1.greaterThan(count2);
    assertTrue(result.getAsBoolean(), "2 count > 1 count should be true");
  }

  @Test
  void testRatioVsRatioComparisonGreaterThan() {
    // Both with ratio units
    EngineValue ratio1 = new DoubleScalar(caster, 0.6, Units.of("ratio"));
    EngineValue ratio2 = new DoubleScalar(caster, 0.5, Units.of("ratio"));

    // 0.6 > 0.5 should be true
    EngineValue result = ratio1.greaterThan(ratio2);
    assertTrue(result.getAsBoolean(), "0.6 ratio > 0.5 ratio should be true");
  }

  @Test
  void testRatioVsRatioComparisonGreaterThanFalse() {
    // Both with ratio units
    EngineValue ratio1 = new DoubleScalar(caster, 0.3, Units.of("ratio"));
    EngineValue ratio2 = new DoubleScalar(caster, 0.5, Units.of("ratio"));

    // 0.3 > 0.5 should be false
    EngineValue result = ratio1.greaterThan(ratio2);
    assertFalse(result.getAsBoolean(), "0.3 ratio > 0.5 ratio should be false");
  }

  @Test
  void testMultipleRatioComparisons() {
    // Simulate a range of external data values
    double[] ratioValues = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
    double threshold = 0.5;
    EngineValue thresholdValue = new DoubleScalar(caster, threshold, Units.of("count"));

    int trueCount = 0;
    int falseCount = 0;

    for (double v : ratioValues) {
      EngineValue ratioValue = new DoubleScalar(caster, v, Units.of("ratio"));
      EngineValue result = ratioValue.greaterThan(thresholdValue);
      boolean actual = result.getAsBoolean();
      boolean expected = v > threshold;

      assertEquals(expected, actual,
          String.format("Comparison %f ratio > %f count should be %s", v, threshold, expected));

      if (actual) {
        trueCount++;
      } else {
        falseCount++;
      }
    }

    // Verify we got different results for different values
    assertTrue(trueCount > 0, "Should have some true results");
    assertTrue(falseCount > 0, "Should have some false results");
  }

  @Test
  void testRatioVsCountLessThan() {
    EngineValue ratioValue = new DoubleScalar(caster, 0.3, Units.of("ratio"));
    EngineValue countValue = new DoubleScalar(caster, 0.5, Units.of("count"));

    // 0.3 < 0.5 should be true
    EngineValue result = ratioValue.lessThan(countValue);
    assertTrue(result.getAsBoolean(), "0.3 ratio < 0.5 count should be true");
  }

  @Test
  void testRatioVsCountEqual() {
    EngineValue ratioValue = new DoubleScalar(caster, 0.5, Units.of("ratio"));
    EngineValue countValue = new DoubleScalar(caster, 0.5, Units.of("count"));

    // 0.5 == 0.5 should be true
    EngineValue result = ratioValue.equalTo(countValue);
    assertTrue(result.getAsBoolean(), "0.5 ratio == 0.5 count should be true");
  }
}
