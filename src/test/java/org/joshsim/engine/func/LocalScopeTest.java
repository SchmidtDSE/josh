
/**
 * Tests for LocalScope.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.func;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Iterator;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


/**
 * Tests for the LocalScope which provides a temporary local variable scope with an enclosing scope.
 */
@ExtendWith(MockitoExtension.class)
class LocalScopeTest {

  @Mock private Scope mockContainingScope;
  @Mock private EngineValue mockValue;
  @Mock private EngineValue mockOuterValue;
  
  private LocalScope scope;

  /**
   * Set up the test environment before each test.
   */
  @BeforeEach
  void setUp() {
    scope = new LocalScope(mockContainingScope);
  }

  @Test
  void testGetLocalValue() {
    scope.defineConstant("localVar", mockValue);
    assertEquals(mockValue, scope.get("localVar"));
  }

  @Test
  void testGetContainingValue() {
    when(mockContainingScope.get("outerVar")).thenReturn(mockOuterValue);
    assertEquals(mockOuterValue, scope.get("outerVar"));
  }

  @Test
  void testHasLocalValue() {
    scope.defineConstant("localVar", mockValue);
    assertTrue(scope.has("localVar"));
  }

  @Test
  void testHasContainingValue() {
    when(mockContainingScope.has("outerVar")).thenReturn(true);
    assertTrue(scope.has("outerVar"));
  }

  @Test
  void testDefineConstantExists() {
    scope.defineConstant("testVar", mockValue);
    assertThrows(RuntimeException.class, () -> scope.defineConstant("testVar", mockValue));
  }

  @Test
  void testDefineConstantExistsInContaining() {
    when(mockContainingScope.has("outerVar")).thenReturn(true);
    assertThrows(RuntimeException.class, () -> scope.defineConstant("outerVar", mockValue));
  }

  @Test
  void testGetAttributes() {
    when(mockContainingScope.getAttributes())
        .thenReturn(Arrays.asList("outerVar1", "outerVar2"));
    scope.defineConstant("localVar", mockValue);

    Iterator<String> attributes = scope.getAttributes().iterator();
    assertTrue(attributes.hasNext());
    assertEquals("outerVar1", attributes.next());
    assertTrue(attributes.hasNext());
    assertEquals("outerVar2", attributes.next());
    assertTrue(attributes.hasNext());
    assertEquals("localVar", attributes.next());
    assertFalse(attributes.hasNext());
  }
}
