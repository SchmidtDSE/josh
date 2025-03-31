
/**
 * Tests for SingleThreadEventHandlerMachine.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.engine.value.type.IntScalar;
import org.joshsim.engine.value.type.BooleanScalar;
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
    assertEquals(mockValue, machine.getResult());
  }

  @Test
  void add_shouldAddTwoValues() {
    // Given
    EngineValue value1 = new IntScalar(5);
    EngineValue value2 = new IntScalar(3);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.add();

    // Then
    assertEquals(new IntScalar(8), machine.getResult());
  }

  @Test
  void subtract_shouldSubtractTwoValues() {
    // Given
    EngineValue value1 = new IntScalar(10);
    EngineValue value2 = new IntScalar(4);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.subtract();

    // Then
    assertEquals(new IntScalar(6), machine.getResult());
  }

  @Test
  void multiply_shouldMultiplyTwoValues() {
    // Given
    EngineValue value1 = new IntScalar(6);
    EngineValue value2 = new IntScalar(7);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.multiply();

    // Then
    assertEquals(new IntScalar(42), machine.getResult());
  }

  @Test
  void divide_shouldDivideTwoValues() {
    // Given
    EngineValue value1 = new IntScalar(15);
    EngineValue value2 = new IntScalar(3);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.divide();

    // Then
    assertEquals(new IntScalar(5), machine.getResult());
  }

  @Test
  void pow_shouldRaiseToPower() {
    // Given
    EngineValue value1 = new IntScalar(2);
    EngineValue value2 = new IntScalar(3);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.pow();

    // Then
    assertEquals(new IntScalar(8), machine.getResult());
  }

  @Test
  void applyMap_shouldApplyMapOperation() {
    // Given
    EngineValue operand = new IntScalar(5);      // Value to map
    EngineValue fromLow = new IntScalar(0);      // Original range start
    EngineValue fromHigh = new IntScalar(10);    // Original range end
    EngineValue toLow = new IntScalar(0);        // Target range start
    EngineValue toHigh = new IntScalar(100);     // Target range end

    // When - stack order: operand, fromLow, fromHigh, toLow, toHigh
    machine.push(operand);
    machine.push(fromLow);
    machine.push(fromHigh);
    machine.push(toLow);
    machine.push(toHigh);
    machine.applyMap("linear");

    // Then - 5 is 50% of way from 0 to 10, so result should be 50 (50% of way from 0 to 100)
    assertEquals(new IntScalar(50), machine.getResult());
  }

  @Test
  void and_shouldPerformLogicalAnd() {
    // Given
    EngineValue value1 = new BooleanScalar(true);
    EngineValue value2 = new BooleanScalar(false);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.and();

    // Then
    assertEquals(new BooleanScalar(false), machine.getResult());
  }

  @Test
  void or_shouldPerformLogicalOr() {
    // Given
    EngineValue value1 = new BooleanScalar(true);
    EngineValue value2 = new BooleanScalar(false);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.or();

    // Then
    assertEquals(new BooleanScalar(true), machine.getResult());
  }

  @Test
  void xor_shouldPerformLogicalXor() {
    // Given
    EngineValue value1 = new BooleanScalar(true);
    EngineValue value2 = new BooleanScalar(true);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.xor();

    // Then
    assertEquals(new BooleanScalar(false), machine.getResult());
  }
}
