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

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.handler.EventHandler;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.value.type.EngineValue;
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
    when(mockEntity.getEventHandlers()).thenReturn(Set.of(mockGroup));
    when(mockEntity.getAttributeNames()).thenReturn(Set.of("testAttr"));
    when(mockGroup.getEventHandlers()).thenReturn(Set.of(mockHandler));
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

  @Test
  void testGetByIndexExistingAttribute() {
    // Setup: testAttr is at some index
    when(mockEntity.getAttributeNameToIndex()).thenReturn(Map.of("testAttr", 0));
    when(mockEntity.getAttributeValue(0)).thenReturn(Optional.of(mockValue));

    EngineValue result = scope.get(0);

    assertEquals(mockValue, result);
  }

  @Test
  void testGetByIndexNonExistentAttribute() {
    when(mockEntity.getAttributeNameToIndex()).thenReturn(Map.of("testAttr", 0));
    when(mockEntity.getAttributeValue(5)).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> scope.get(5));
  }

  @Test
  void testGetByIndexAndNameReturnSame() {
    when(mockEntity.getAttributeNameToIndex()).thenReturn(Map.of("testAttr", 0));
    when(mockEntity.getAttributeValue("testAttr")).thenReturn(Optional.of(mockValue));
    when(mockEntity.getAttributeValue(0)).thenReturn(Optional.of(mockValue));

    EngineValue byName = scope.get("testAttr");
    EngineValue byIndex = scope.get(0);

    assertEquals(byName, byIndex);
  }

  @Test
  void testGetAttributeNameToIndex() {
    Map<String, Integer> expectedMap = Map.of("testAttr", 0);
    when(mockEntity.getAttributeNameToIndex()).thenReturn(expectedMap);

    Map<String, Integer> result = scope.getAttributeNameToIndex();

    assertEquals(expectedMap, result);
  }
}
