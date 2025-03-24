/**
 * Tests for ReturnCurrentCallable.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.func;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.joshsim.engine.value.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.when;

/**
 * Tests for the ReturnCurrentCallable which returns the current value from scope.
 */
@ExtendWith(MockitoExtension.class)
class ReturnCurrentCallableTest {

  @Mock private Scope mockScope;
  @Mock private EngineValue mockValue;
  private ReturnCurrentCallable callable;

  @BeforeEach
  void setUp() {
    callable = new ReturnCurrentCallable();
  }

  @Test
  void testEvaluate() {
    when(mockScope.get("current")).thenReturn(mockValue);
    
    EngineValue result = callable.evaluate(mockScope);
    assertEquals(mockValue, result);
  }
}