/**
 * Tests for ValueResolver.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.interpret;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.handler.EventHandler;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.func.EntityScope;
import org.joshsim.engine.func.Scope;
import org.joshsim.engine.value.engine.EngineValueFactory;
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

  private EngineValueFactory valueFactory;
  private Scope scope;
  private ValueResolver resolver;

  /**
   * Setup an entity hierarchy for testing.
   */
  @BeforeEach
  void setUp() {
    valueFactory = new EngineValueFactory();

    // Set up nested reference on root
    when(mockNestedHandler.getAttributeName()).thenReturn("nested");
    when(mockNestedHandler.getEventName()).thenReturn("init");
    when(mockNestedGroup.getEventHandlers()).thenReturn(Arrays.asList(mockNestedHandler));

    // Setup root test attr
    when(mockHandler.getAttributeName()).thenReturn("testAttr");
    when(mockGroup.getEventHandlers()).thenReturn(Arrays.asList(mockHandler));
    when(mockEntity.getEventHandlers()).thenReturn(Arrays.asList(mockGroup, mockNestedGroup));
    when(mockEntity.getAttributeNames()).thenReturn(Set.of("nested"));

    // Configure mock for direct value test
    when(mockScope.get("direct")).thenReturn(mockDirectValue);
    when(mockScope.has("direct")).thenReturn(true);

    // Configure mocks for nested value test
    when(mockScope.get("entity")).thenReturn(mockEntityValue);
    when(mockScope.has("entity")).thenReturn(true);
    when(mockEntityValue.getSize()).thenReturn(Optional.of(1));
    when(mockEntityValue.getAsMutableEntity()).thenReturn(mockEntity);
    when(mockEntityValue.getAsEntity()).thenReturn(mockEntity);

    // Setup nested entity value.
    when(mockEntity.getAttributeValue("nested")).thenReturn(Optional.of(mockNestedValue));

    // Configure mock for local.value test
    when(mockScope.get("local.value")).thenReturn(mockDirectValue);
    when(mockScope.has("local.value")).thenReturn(true);
  }

  @Test
  void testDirectValueResolution() {
    resolver = new ValueResolver(valueFactory, "direct");
    Optional<EngineValue> result = resolver.get(mockScope);

    assertTrue(result.isPresent(), "Should resolve direct value");
    assertEquals(mockDirectValue, result.get(), "Should return correct direct value");
  }

  @Test
  void testNestedValueResolution() {
    resolver = new ValueResolver(valueFactory, "entity.nested");
    Optional<EngineValue> result = resolver.get(mockScope);

    assertTrue(result.isPresent(), "Should resolve nested value");
    assertEquals(mockNestedValue, result.get(), "Should return correct nested value");
  }

  @Test
  void testLocalDotValueResolution() {
    resolver = new ValueResolver(valueFactory, "local.value");
    Optional<EngineValue> result = resolver.get(mockScope);

    assertTrue(result.isPresent(), "Should resolve local.value");
    assertEquals(mockDirectValue, result.get(), "Should return correct local value");
  }

  @Test
  void testIntegerCachingWithSingleEntityType() {
    // Setup: Create EntityScope with mock entity
    Map<String, Integer> indexMap = Map.of("testAttr", 0);
    when(mockEntity.getAttributeNameToIndex()).thenReturn(indexMap);
    when(mockEntity.getAttributeValue(0)).thenReturn(Optional.of(mockDirectValue));
    when(mockEntity.getAttributeNames()).thenReturn(Set.of("testAttr"));

    EntityScope entityScope = new EntityScope(mockEntity);
    ValueResolver resolver = new ValueResolver(valueFactory, "testAttr");

    // First access: Should miss cache, then cache the index
    Optional<EngineValue> result1 = resolver.get(entityScope);
    assertTrue(result1.isPresent());
    assertEquals(mockDirectValue, result1.get());

    // Second access: Should hit cache (verify by checking mock interactions)
    Optional<EngineValue> result2 = resolver.get(entityScope);
    assertTrue(result2.isPresent());
    assertEquals(mockDirectValue, result2.get());

    // Verify that getAttributeValue(int) was called (fast path used)
    verify(mockEntity, atLeastOnce()).getAttributeValue(0);
  }

  @Test
  void testIntegerCachingWithMultipleEntityTypes() {
    // Setup: Two different entity types (different index maps)
    Map<String, Integer> agentIndexMap = Map.of("testAttr", 0, "otherAttr", 1);
    Map<String, Integer> patchIndexMap = Map.of("testAttr", 1, "differentAttr", 0);

    MutableEntity mockAgent = mock(MutableEntity.class);
    MutableEntity mockPatch = mock(MutableEntity.class);

    when(mockAgent.getAttributeNameToIndex()).thenReturn(agentIndexMap);
    when(mockAgent.getAttributeValue(0)).thenReturn(Optional.of(mockDirectValue));
    when(mockAgent.getAttributeNames()).thenReturn(Set.of("testAttr", "otherAttr"));

    when(mockPatch.getAttributeNameToIndex()).thenReturn(patchIndexMap);
    when(mockPatch.getAttributeValue(1)).thenReturn(Optional.of(mockNestedValue));
    when(mockPatch.getAttributeNames()).thenReturn(Set.of("testAttr", "differentAttr"));

    EntityScope agentScope = new EntityScope(mockAgent);
    EntityScope patchScope = new EntityScope(mockPatch);

    // Same resolver used for both entity types
    ValueResolver resolver = new ValueResolver(valueFactory, "testAttr");

    // Access agent (testAttr at index 0)
    Optional<EngineValue> agentResult = resolver.get(agentScope);
    assertTrue(agentResult.isPresent());
    assertEquals(mockDirectValue, agentResult.get());

    // Access patch (testAttr at index 1, different from agent!)
    Optional<EngineValue> patchResult = resolver.get(patchScope);
    assertTrue(patchResult.isPresent());
    assertEquals(mockNestedValue, patchResult.get());

    // Verify correct indices were used
    verify(mockAgent).getAttributeValue(0); // Agent: testAttr at index 0
    verify(mockPatch).getAttributeValue(1); // Patch: testAttr at index 1
  }

  @Test
  void testCacheHandlesEmptyDistributions() {
    // Setup: Entity with no attributes
    Map<String, Integer> emptyIndexMap = Map.of();
    when(mockEntity.getAttributeNameToIndex()).thenReturn(emptyIndexMap);
    when(mockEntity.getAttributeNames()).thenReturn(Set.of());

    EntityScope entityScope = new EntityScope(mockEntity);
    ValueResolver resolver = new ValueResolver(valueFactory, "nonExistent");

    // Should return empty (not throw exception)
    Optional<EngineValue> result = resolver.get(entityScope);
    assertFalse(result.isPresent());
  }

  @Test
  void testDottedPathFallsBackToSlowPath() {
    // Setup
    when(mockScope.has("entity")).thenReturn(true);
    when(mockScope.get("entity")).thenReturn(mockEntityValue);
    when(mockEntityValue.getSize()).thenReturn(Optional.of(1));
    when(mockEntityValue.getAsEntity()).thenReturn(mockEntity);

    Map<String, Integer> indexMap = Map.of("nested", 0);
    when(mockEntity.getAttributeNameToIndex()).thenReturn(indexMap);
    when(mockEntity.getAttributeValue("nested")).thenReturn(Optional.of(mockNestedValue));
    when(mockEntity.getAttributeValue(0)).thenReturn(Optional.of(mockNestedValue));
    when(mockEntity.getAttributeNames()).thenReturn(Set.of("nested"));

    // Dotted path should use slow path (complex resolution)
    ValueResolver resolver = new ValueResolver(valueFactory, "entity.nested");
    Optional<EngineValue> result = resolver.get(mockScope);

    assertTrue(result.isPresent());
    assertEquals(mockNestedValue, result.get());
  }

  @Test
  void testCachePersistsAcrossMultipleCalls() {
    // Setup
    Map<String, Integer> indexMap = Map.of("testAttr", 5);
    when(mockEntity.getAttributeNameToIndex()).thenReturn(indexMap);
    when(mockEntity.getAttributeValue(5)).thenReturn(Optional.of(mockDirectValue));
    when(mockEntity.getAttributeNames()).thenReturn(Set.of("testAttr"));

    EntityScope entityScope = new EntityScope(mockEntity);
    ValueResolver resolver = new ValueResolver(valueFactory, "testAttr");

    // Make multiple accesses
    for (int i = 0; i < 10; i++) {
      Optional<EngineValue> result = resolver.get(entityScope);
      assertTrue(result.isPresent());
      assertEquals(mockDirectValue, result.get());
    }

    // Verify integer method was called multiple times (cache hit)
    verify(mockEntity, times(10)).getAttributeValue(5);

    // Verify getAttributeNameToIndex was called (to get the index map)
    verify(mockEntity, atLeast(1)).getAttributeNameToIndex();
  }

  @Test
  void testUninitializedAttributeReturnsEmpty() {
    // Setup: Attribute exists in index map but returns empty
    Map<String, Integer> indexMap = Map.of("testAttr", 0);
    when(mockEntity.getAttributeNameToIndex()).thenReturn(indexMap);
    when(mockEntity.getAttributeValue(0)).thenReturn(Optional.empty());
    when(mockEntity.getAttributeNames()).thenReturn(Set.of("testAttr"));

    EntityScope entityScope = new EntityScope(mockEntity);
    ValueResolver resolver = new ValueResolver(valueFactory, "testAttr");

    // Should return empty (attribute exists but uninitialized)
    Optional<EngineValue> result = resolver.get(entityScope);
    assertFalse(result.isPresent());
  }

  @Test
  void testNonExistentAttributeReturnsEmpty() {
    // Setup: Attribute doesn't exist in index map
    Map<String, Integer> indexMap = Map.of("otherAttr", 0);
    when(mockEntity.getAttributeNameToIndex()).thenReturn(indexMap);
    when(mockEntity.getAttributeNames()).thenReturn(Set.of("otherAttr"));
    when(mockEntity.getAttributeValue("nonExistent")).thenReturn(Optional.empty());

    EntityScope entityScope = new EntityScope(mockEntity);
    ValueResolver resolver = new ValueResolver(valueFactory, "nonExistent");

    // Should return empty (attribute doesn't exist)
    Optional<EngineValue> result = resolver.get(entityScope);
    assertFalse(result.isPresent());
  }

}
