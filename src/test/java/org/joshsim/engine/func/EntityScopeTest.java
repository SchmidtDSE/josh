/**
 * Tests for EntityScope.
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
import java.util.Optional;
import org.joshsim.engine.entity.Entity;
import org.joshsim.engine.entity.EventHandler;
import org.joshsim.engine.entity.EventHandlerGroup;
import org.joshsim.engine.entity.MutableEntity;
import org.joshsim.engine.value.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for the EntityScope which provides attribute access to entities.
 */
@ExtendWith(MockitoExtension.class)
class EntityScopeTest {

  @Mock(lenient = true) private MutableEntity mockEntity;
  @Mock(lenient = true) private EngineValue mockValue;
  @Mock(lenient = true) private EventHandlerGroup mockGroup;
  @Mock(lenient = true) private EventHandler mockHandler;

  private EntityScope scope;

  /**
   * Set up the test environment before each test.
   */
  @BeforeEach
  void setUp() {
    when(mockEntity.getEventHandlers()).thenReturn(Arrays.asList(mockGroup));
    when(mockEntity.getAttributeNames()).thenReturn(Arrays.asList("testAttr"));
    when(mockGroup.getEventHandlers()).thenReturn(Arrays.asList(mockHandler));
    when(mockHandler.getAttributeName()).thenReturn("testAttr");

    scope = new EntityScope(mockEntity);
  }

  @Test
  void testGetExistingAttribute() {
    when(mockEntity.getAttributeValue("testAttr")).thenReturn(Optional.of(mockValue));

    EngineValue result = scope.get("testAttr");
    assertEquals(mockValue, result);
  }

  @Test
  void testGetNonExistentAttribute() {
    when(mockEntity.getAttributeValue("nonexistent")).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> scope.get("nonexistent"));
  }

  @Test
  void testHasAttribute() {
    assertTrue(scope.has("testAttr"));
    assertFalse(scope.has("nonexistent"));
  }

  @Test
  void testGetAttributes() {
    Iterable<String> attributes = scope.getAttributes();
    assertTrue(attributes.iterator().hasNext());
    assertEquals("testAttr", attributes.iterator().next());
  }
}
