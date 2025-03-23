
package org.joshsim.engine.func;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.joshsim.engine.value.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SingleValueScopeTest {

  @Mock private EngineValue mockValue;
  private SingleValueScope scope;

  @BeforeEach
  void setUp() {
    scope = new SingleValueScope(mockValue);
  }

  @Test
  void testGetCurrent() {
    assertEquals(mockValue, scope.get("current"));
  }

  @Test
  void testGetNonCurrent() {
    assertThrows(IllegalArgumentException.class, () -> scope.get("other"));
  }

  @Test
  void testHas() {
    assertTrue(scope.has("current"));
    assertFalse(scope.has("other"));
  }

  @Test
  void testGetAttributes() {
    Iterable<String> attributes = scope.getAttributes();
    assertTrue(attributes.iterator().hasNext());
    assertEquals("current", attributes.iterator().next());
  }
}
