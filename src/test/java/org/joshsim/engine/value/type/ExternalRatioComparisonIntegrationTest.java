/**
 * Integration tests for external ratio data comparison with literal numbers.
 *
 * This test investigates the reported bug where external data with "ratio" units
 * compared against literal numbers always evaluates to the same result for all patches.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.value.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.lang.bridge.EngineBridge;
import org.joshsim.lang.interpret.machine.SingleThreadEventHandlerMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Integration tests that simulate the bug scenario: comparing external ratio data with literals.
 */
@ExtendWith(MockitoExtension.class)
class ExternalRatioComparisonIntegrationTest {

  @Mock private EngineBridge mockBridge;
  @Mock private Scope mockScope;

  private EngineValueFactory factory;

  @BeforeEach
  void setUp() {
    factory = new EngineValueFactory();
    lenient().when(mockBridge.getEngineValueFactory()).thenReturn(factory);
    // Mock the convert method to return the value with new units (NoopConversion behavior)
    lenient().when(mockBridge.convert(any(EngineValue.class), any(Units.class)))
        .thenAnswer(invocation -> {
          EngineValue value = invocation.getArgument(0);
          Units newUnits = invocation.getArgument(1);
          return value.replaceUnits(newUnits);
        });
  }

  /**
   * Test that simulates comparing external ratio data with a literal number.
   * This mimics the bug report scenario: "testValue > 0.5" where testValue has ratio units.
   */
  @Test
  void testRatioExternalVsLiteralComparison_ValueAboveThreshold() {
    // Setup: External data returns 0.7 ratio (above threshold)
    EngineValue externalValue = factory.buildForNumber(0.7, Units.of("ratio"));

    // Literal value 0.5 with count/empty units
    EngineValue literalValue = factory.buildForNumber(0.5, Units.of("count"));

    // Create machine and simulate the comparison
    SingleThreadEventHandlerMachine machine = new SingleThreadEventHandlerMachine(
        mockBridge, mockScope
    );

    // Simulate: push external value, push literal, call gt()
    machine.push(externalValue);
    machine.push(literalValue);
    machine.gt();
    machine.end();

    // Verify: 0.7 > 0.5 should be true
    EngineValue result = machine.getResult();
    assertNotNull(result, "Comparison result should not be null");
    assertTrue(result.getAsBoolean(), "0.7 ratio > 0.5 should be true");
  }

  @Test
  void testRatioExternalVsLiteralComparison_ValueBelowThreshold() {
    // Setup: External data returns 0.3 ratio (below threshold)
    EngineValue externalValue = factory.buildForNumber(0.3, Units.of("ratio"));

    // Literal value 0.5 with count/empty units
    EngineValue literalValue = factory.buildForNumber(0.5, Units.of("count"));

    // Create machine and simulate the comparison
    SingleThreadEventHandlerMachine machine = new SingleThreadEventHandlerMachine(
        mockBridge, mockScope
    );

    // Simulate: push external value, push literal, call gt()
    machine.push(externalValue);
    machine.push(literalValue);
    machine.gt();
    machine.end();

    // Verify: 0.3 > 0.5 should be false
    EngineValue result = machine.getResult();
    assertNotNull(result, "Comparison result should not be null");
    assertFalse(result.getAsBoolean(), "0.3 ratio > 0.5 should be false");
  }

  @Test
  void testMultiplePatchesWithDifferentRatioValues() {
    // Simulate multiple patches with different external data values
    double[] externalValues = {0.1, 0.3, 0.5, 0.7, 0.9};
    double threshold = 0.5;

    int trueCount = 0;
    int falseCount = 0;

    for (double value : externalValues) {
      // Setup external data for this "patch"
      EngineValue externalValue = factory.buildForNumber(value, Units.of("ratio"));
      EngineValue literalValue = factory.buildForNumber(threshold, Units.of("count"));

      // Create a new machine for each patch (simulating per-patch evaluation)
      SingleThreadEventHandlerMachine machine = new SingleThreadEventHandlerMachine(
          mockBridge, mockScope
      );

      machine.push(externalValue);
      machine.push(literalValue);
      machine.gt();
      machine.end();

      EngineValue result = machine.getResult();
      boolean actual = result.getAsBoolean();
      boolean expected = value > threshold;

      assertEquals(expected, actual,
          String.format("Comparison %f ratio > %f count should be %s", value, threshold, expected));

      if (actual) {
        trueCount++;
      } else {
        falseCount++;
      }
    }

    // Verify we got different results
    assertTrue(trueCount > 0, "Should have some true results (values above threshold)");
    assertTrue(falseCount > 0, "Should have some false results (values below threshold)");
    assertEquals(2, trueCount, "Should have 2 values > 0.5 (0.7 and 0.9)");
    assertEquals(3, falseCount, "Should have 3 values <= 0.5 (0.1, 0.3, 0.5)");
  }

  @Test
  void testConditionalExpressionWithRatioComparison() {
    // Test the full conditional expression: "high" if testValue > 0.5 else "low"
    double[] externalValues = {0.2, 0.8};
    String[] expectedResults = {"low", "high"};

    for (int i = 0; i < externalValues.length; i++) {
      double value = externalValues[i];
      String expected = expectedResults[i];

      // External data with ratio units
      EngineValue externalValue = factory.buildForNumber(value, Units.of("ratio"));
      // Literal threshold with count/empty units
      EngineValue literalValue = factory.buildForNumber(0.5, Units.of("count"));
      // String results
      EngineValue highValue = factory.build("high", Units.EMPTY);
      EngineValue lowValue = factory.build("low", Units.EMPTY);

      // Create machine for condition evaluation
      SingleThreadEventHandlerMachine condMachine = new SingleThreadEventHandlerMachine(
          mockBridge, mockScope
      );

      // Simulate the conditional: "high" if testValue > 0.5 else "low"
      // First, evaluate the condition
      condMachine.push(externalValue);
      condMachine.push(literalValue);
      condMachine.gt();
      condMachine.end();

      // Get the condition result
      EngineValue conditionResult = condMachine.getResult();

      // Now simulate branching based on condition
      SingleThreadEventHandlerMachine resultMachine = new SingleThreadEventHandlerMachine(
          mockBridge, mockScope
      );

      if (conditionResult.getAsBoolean()) {
        resultMachine.push(highValue);
      } else {
        resultMachine.push(lowValue);
      }

      resultMachine.end();
      EngineValue result = resultMachine.getResult();

      assertEquals(expected, result.getAsString(),
          String.format("For value %f, expected '%s' but got '%s'", value, expected,
              result.getAsString()));
    }
  }

  @Test
  void testConversionGroupWithMixedUnits() {
    // Test that conversion group handling works correctly with ratio vs count units
    EngineValue ratioValue = factory.buildForNumber(0.6, Units.of("ratio"));
    EngineValue countValue = factory.buildForNumber(0.5, Units.of("count"));

    SingleThreadEventHandlerMachine machine = new SingleThreadEventHandlerMachine(
        mockBridge, mockScope
    );

    // The conversion group logic in gt() should:
    // 1. Pop countValue (0.5, count/empty units) - sets conversionTarget to empty
    // 2. Pop ratioValue (0.6, ratio units) - converts to empty (NoopConversion)
    // 3. Compare 0.6 > 0.5 = true
    machine.push(ratioValue);
    machine.push(countValue);
    machine.gt();
    machine.end();

    EngineValue result = machine.getResult();
    assertTrue(result.getAsBoolean(),
        "After conversion group handling, 0.6 ratio > 0.5 count should be true");
  }

  @Test
  void testCountVsCountComparison_ShouldAlwaysWork() {
    // The bug report says count comparisons work - verify this
    EngineValue value1 = factory.buildForNumber(2.0, Units.of("count"));
    EngineValue value2 = factory.buildForNumber(1.0, Units.of("count"));

    SingleThreadEventHandlerMachine machine = new SingleThreadEventHandlerMachine(
        mockBridge, mockScope
    );

    machine.push(value1);
    machine.push(value2);
    machine.gt();
    machine.end();

    assertTrue(machine.getResult().getAsBoolean(), "2 count > 1 count should be true");
  }

  @Test
  void testEmptyUnitsAsCountAlias() {
    // Verify that Units.of("count") produces empty/blank units
    Units countUnits = Units.of("count");
    assertTrue(countUnits.toString().isBlank(),
        "count units should be blank (count is alias for empty)");
    assertEquals(Units.EMPTY, countUnits, "Units.of('count') should equal Units.EMPTY");
  }

  @Test
  void testRatioUnitsAreNotEmpty() {
    // Verify that ratio units are NOT empty
    Units ratioUnits = Units.of("ratio");
    assertFalse(ratioUnits.toString().isBlank(),
        "ratio units should not be blank");
    assertEquals("ratio", ratioUnits.toString(),
        "ratio units should have 'ratio' as string representation");
  }
}
