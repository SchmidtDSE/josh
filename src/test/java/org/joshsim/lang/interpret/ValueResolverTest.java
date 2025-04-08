/**
 * Tests for ValueResolver.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.handler.EventHandler;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


/**
 * Tests for the ValueResolver which resolves dot-chained paths in scopes.
 */
@ExtendWith(MockitoExtension.class)
public class ValueResolverTest {

  @Mock(lenient = true) private Scope mockScope;
  @Mock(lenient = true) private MutableEntity mockEntity;
  @Mock(lenient = true) private EngineValue mockDirectValue;
  @Mock(lenient = true) private EngineValue mockEntityValue;
  @Mock(lenient = true) private EngineValue mockNestedValue;
  @Mock(lenient = true) private EventHandlerGroup mockGroup;
  @Mock(lenient = true) private EventHandler mockHandler;
  @Mock(lenient = true) private EventHandlerGroup mockNestedGroup;
  @Mock(lenient = true) private EventHandler mockNestedHandler;

  private Scope scope;
  private ValueResolver resolver;

  /**
   * Setup an entity hierarchy for testing.
   */
  @BeforeEach
  void setUp() {
    // Set up nested reference on root
    when(mockNestedHandler.getAttributeName()).thenReturn("nested");
    when(mockNestedHandler.getEventName()).thenReturn("init");
    when(mockNestedGroup.getEventHandlers()).thenReturn(Arrays.asList(mockNestedHandler));

    // Setup root test attr
    when(mockHandler.getAttributeName()).thenReturn("testAttr");
    when(mockGroup.getEventHandlers()).thenReturn(Arrays.asList(mockHandler));
    when(mockEntity.getEventHandlers()).thenReturn(Arrays.asList(mockGroup, mockNestedGroup));
    when(mockEntity.getAttributeNames()).thenReturn(Arrays.asList("nested"));

    // Configure mock for direct value test
    when(mockScope.get("direct")).thenReturn(mockDirectValue);
    when(mockScope.has("direct")).thenReturn(true);

    // Configure mocks for nested value test
    when(mockScope.get("entity")).thenReturn(mockEntityValue);
    when(mockScope.has("entity")).thenReturn(true);
    when(mockEntityValue.getSize()).thenReturn(Optional.of(1));
    when(mockEntityValue.getAsMutableEntity()).thenReturn(mockEntity);

    // Setup nested entity value.
    when(mockEntity.getAttributeValue("nested")).thenReturn(Optional.of(mockNestedValue));

    // Configure mock for local.value test
    when(mockScope.get("local.value")).thenReturn(mockDirectValue);
    when(mockScope.has("local.value")).thenReturn(true);
  }

  @Test
  void testDirectValueResolution() {
    resolver = new ValueResolver("direct");
    Optional<EngineValue> result = resolver.get(mockScope);

    assertTrue(result.isPresent(), "Should resolve direct value");
    assertEquals(mockDirectValue, result.get(), "Should return correct direct value");
  }

  @Test
  void testNestedValueResolution() {
    resolver = new ValueResolver("entity.nested");
    Optional<EngineValue> result = resolver.get(mockScope);

    assertTrue(result.isPresent(), "Should resolve nested value");
    assertEquals(mockNestedValue, result.get(), "Should return correct nested value");
  }

  @Test
  void testLocalDotValueResolution() {
    resolver = new ValueResolver("local.value");
    Optional<EngineValue> result = resolver.get(mockScope);

    assertTrue(result.isPresent(), "Should resolve local.value");
    assertEquals(mockDirectValue, result.get(), "Should return correct local value");
  }

}
