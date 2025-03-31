
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
    EngineValue value1 = mock(EngineValue.class);
    EngineValue value2 = mock(EngineValue.class);
    EngineValue result = mock(EngineValue.class);
    when(value1.add(value2)).thenReturn(result);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.add();

    // Then
    assertEquals(result, machine.getResult());
  }

  @Test
  void subtract_shouldSubtractTwoValues() {
    // Given
    EngineValue value1 = mock(EngineValue.class);
    EngineValue value2 = mock(EngineValue.class);
    EngineValue result = mock(EngineValue.class);
    when(value1.subtract(value2)).thenReturn(result);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.subtract();

    // Then
    assertEquals(result, machine.getResult());
  }

  @Test
  void multiply_shouldMultiplyTwoValues() {
    // Given
    EngineValue value1 = mock(EngineValue.class);
    EngineValue value2 = mock(EngineValue.class);
    EngineValue result = mock(EngineValue.class);
    when(value1.multiply(value2)).thenReturn(result);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.multiply();

    // Then
    assertEquals(result, machine.getResult());
  }

  @Test
  void divide_shouldDivideTwoValues() {
    // Given
    EngineValue value1 = mock(EngineValue.class);
    EngineValue value2 = mock(EngineValue.class);
    EngineValue result = mock(EngineValue.class);
    when(value1.divide(value2)).thenReturn(result);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.divide();

    // Then
    assertEquals(result, machine.getResult());
  }

  @Test
  void pow_shouldRaiseToPower() {
    // Given
    EngineValue value1 = mock(EngineValue.class);
    EngineValue value2 = mock(EngineValue.class);
    EngineValue result = mock(EngineValue.class);
    when(value1.pow(value2)).thenReturn(result);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.pow();

    // Then
    assertEquals(result, machine.getResult());
  }

  @Test
  void applyMap_shouldApplyMapOperation() {
    // Given
    EngineValue value1 = mock(EngineValue.class);
    EngineValue value2 = mock(EngineValue.class);
    EngineValue result = mock(EngineValue.class);
    when(value1.applyMap(value2)).thenReturn(result);

    // When
    machine.push(value1);
    machine.push(value2);
    machine.applyMap();

    // Then
    assertEquals(result, machine.getResult());
  }
}
