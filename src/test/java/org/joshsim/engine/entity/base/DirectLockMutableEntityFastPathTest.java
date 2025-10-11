package org.joshsim.engine.entity.base;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.handler.EventHandler;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.func.CompiledCallable;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;
import org.junit.jupiter.api.Test;

/**
 * Tests for substep-aware no-handler tracking optimization in DirectLockMutableEntity.
 */
public class DirectLockMutableEntityFastPathTest {

  @Test
  public void testAttributeWithNoInitHandlerButHasStepHandler() {
    // Setup: Attribute has .step handler but no .init handler
    final CompiledCallable mockCallable = mock(CompiledCallable.class);
    final EngineValue mockValue = mock(EngineValue.class);

    // Create handler for "step" substep only
    EventKey stepKey = EventKey.of("temperature", "step");
    EventHandler stepHandler = mock(EventHandler.class);
    when(stepHandler.getAttributeName()).thenReturn("temperature");
    EventHandlerGroup stepGroup = mock(EventHandlerGroup.class);
    when(stepGroup.getEventKey()).thenReturn(stepKey);
    when(stepGroup.getEventHandlers()).thenReturn(List.of(stepHandler));

    Map<EventKey, EventHandlerGroup> handlers = Map.of(stepKey, stepGroup);

    // Initial attributes (includes temperature)
    Map<String, EngineValue> initialAttrs = new HashMap<>();
    initialAttrs.put("temperature", mockValue);

    // Create test entity
    TestDirectLockMutableEntity entity =
        new TestDirectLockMutableEntity("test", handlers, initialAttrs);

    // Verify: temperature has NO handler for "init" but HAS handler for "step"
    assertTrue(entity.hasNoHandlers("temperature", "init"),
        "temperature should have no init handler");
    assertFalse(entity.hasNoHandlers("temperature", "step"),
        "temperature should have step handler");
  }

  @Test
  public void testAttributeWithHandlersForAllSubsteps() {
    // Setup: Attribute has handlers for multiple substeps
    final CompiledCallable mockCallable = mock(CompiledCallable.class);
    final EngineValue mockValue = mock(EngineValue.class);

    EventKey initKey = EventKey.of("age", "init");
    EventKey stepKey = EventKey.of("age", "step");

    EventHandler initHandler = mock(EventHandler.class);
    when(initHandler.getAttributeName()).thenReturn("age");
    EventHandler stepHandler = mock(EventHandler.class);
    when(stepHandler.getAttributeName()).thenReturn("age");

    EventHandlerGroup initGroup = mock(EventHandlerGroup.class);
    when(initGroup.getEventKey()).thenReturn(initKey);
    when(initGroup.getEventHandlers()).thenReturn(List.of(initHandler));

    EventHandlerGroup stepGroup = mock(EventHandlerGroup.class);
    when(stepGroup.getEventKey()).thenReturn(stepKey);
    when(stepGroup.getEventHandlers()).thenReturn(List.of(stepHandler));

    Map<EventKey, EventHandlerGroup> handlers = new HashMap<>();
    handlers.put(initKey, initGroup);
    handlers.put(stepKey, stepGroup);

    Map<String, EngineValue> initialAttrs = new HashMap<>();
    initialAttrs.put("age", mockValue);

    TestDirectLockMutableEntity entity =
        new TestDirectLockMutableEntity("test", handlers, initialAttrs);

    // Verify: age has handlers for both init and step
    assertFalse(entity.hasNoHandlers("age", "init"),
        "age should have init handler");
    assertFalse(entity.hasNoHandlers("age", "step"),
        "age should have step handler");
  }

  @Test
  public void testAttributeNotInInitialAttributesReturnsFalse() {
    // Setup: Attribute defined only in handlers, not in initial attributes
    CompiledCallable mockCallable = mock(CompiledCallable.class);

    EventKey stepKey = EventKey.of("computed", "step");
    EventHandler stepHandler = mock(EventHandler.class);
    when(stepHandler.getAttributeName()).thenReturn("computed");
    EventHandlerGroup stepGroup = mock(EventHandlerGroup.class);
    when(stepGroup.getEventKey()).thenReturn(stepKey);
    when(stepGroup.getEventHandlers()).thenReturn(List.of(stepHandler));

    Map<EventKey, EventHandlerGroup> handlers = Map.of(stepKey, stepGroup);
    Map<String, EngineValue> initialAttrs = new HashMap<>();  // Empty!

    TestDirectLockMutableEntity entity =
        new TestDirectLockMutableEntity("test", handlers, initialAttrs);

    // Verify: returns false because attribute not in initial attributes
    // (can't optimize what we don't know about)
    assertFalse(entity.hasNoHandlers("computed", "init"),
        "computed not in initial attrs, so cannot optimize");
  }

  @Test
  public void testUnknownSubstepReturnsFalse() {
    // Setup: Check for substep that doesn't exist
    EngineValue mockValue = mock(EngineValue.class);

    Map<String, EngineValue> initialAttrs = new HashMap<>();
    initialAttrs.put("test", mockValue);

    TestDirectLockMutableEntity entity =
        new TestDirectLockMutableEntity("test", new HashMap<>(), initialAttrs);

    // Verify: unknown substep returns false (safe default - use slow path)
    assertFalse(entity.hasNoHandlers("test", "unknown_substep"),
        "unknown substep should return false (safe fallback)");
  }

  @Test
  public void testMultipleAttributesMixOfHandlers() {
    // Setup: Mix of attributes with handlers and without
    final EngineValue mockValue = mock(EngineValue.class);

    // temperature has only .step handler
    EventKey tempStepKey = EventKey.of("temperature", "step");
    EventHandler tempStepHandler = mock(EventHandler.class);
    when(tempStepHandler.getAttributeName()).thenReturn("temperature");
    EventHandlerGroup tempStepGroup = mock(EventHandlerGroup.class);
    when(tempStepGroup.getEventKey()).thenReturn(tempStepKey);
    when(tempStepGroup.getEventHandlers()).thenReturn(List.of(tempStepHandler));

    // age has both .init and .step handlers
    EventKey ageInitKey = EventKey.of("age", "init");
    EventHandler ageInitHandler = mock(EventHandler.class);
    when(ageInitHandler.getAttributeName()).thenReturn("age");
    EventHandlerGroup ageInitGroup = mock(EventHandlerGroup.class);
    when(ageInitGroup.getEventKey()).thenReturn(ageInitKey);
    when(ageInitGroup.getEventHandlers()).thenReturn(List.of(ageInitHandler));

    EventKey ageStepKey = EventKey.of("age", "step");
    EventHandler ageStepHandler = mock(EventHandler.class);
    when(ageStepHandler.getAttributeName()).thenReturn("age");
    EventHandlerGroup ageStepGroup = mock(EventHandlerGroup.class);
    when(ageStepGroup.getEventKey()).thenReturn(ageStepKey);
    when(ageStepGroup.getEventHandlers()).thenReturn(List.of(ageStepHandler));

    Map<EventKey, EventHandlerGroup> handlers = new HashMap<>();
    handlers.put(tempStepKey, tempStepGroup);
    handlers.put(ageInitKey, ageInitGroup);
    handlers.put(ageStepKey, ageStepGroup);

    Map<String, EngineValue> initialAttrs = new HashMap<>();
    initialAttrs.put("temperature", mockValue);
    initialAttrs.put("age", mockValue);
    initialAttrs.put("id", mockValue);  // No handlers at all

    TestDirectLockMutableEntity entity =
        new TestDirectLockMutableEntity("test", handlers, initialAttrs);

    // Verify temperature: has no init handler but has step handler
    assertTrue(entity.hasNoHandlers("temperature", "init"));
    assertFalse(entity.hasNoHandlers("temperature", "step"));

    // Verify age: has both handlers
    assertFalse(entity.hasNoHandlers("age", "init"));
    assertFalse(entity.hasNoHandlers("age", "step"));

    // Verify id: has no handlers for any substep
    assertTrue(entity.hasNoHandlers("id", "init"));
    assertTrue(entity.hasNoHandlers("id", "step"));
    assertTrue(entity.hasNoHandlers("id", "start"));
    assertTrue(entity.hasNoHandlers("id", "end"));
    assertTrue(entity.hasNoHandlers("id", "constant"));
  }

  @Test
  public void testAllCommonSubsteps() {
    // Setup: Test all common substeps (init, step, start, end, constant)
    final EngineValue mockValue = mock(EngineValue.class);

    // Create handler for "start" substep only
    EventKey startKey = EventKey.of("height", "start");
    EventHandler startHandler = mock(EventHandler.class);
    when(startHandler.getAttributeName()).thenReturn("height");
    EventHandlerGroup startGroup = mock(EventHandlerGroup.class);
    when(startGroup.getEventKey()).thenReturn(startKey);
    when(startGroup.getEventHandlers()).thenReturn(List.of(startHandler));

    Map<EventKey, EventHandlerGroup> handlers = Map.of(startKey, startGroup);

    Map<String, EngineValue> initialAttrs = new HashMap<>();
    initialAttrs.put("height", mockValue);

    TestDirectLockMutableEntity entity =
        new TestDirectLockMutableEntity("test", handlers, initialAttrs);

    // Verify: height has NO handler except for "start"
    assertTrue(entity.hasNoHandlers("height", "init"));
    assertTrue(entity.hasNoHandlers("height", "step"));
    assertFalse(entity.hasNoHandlers("height", "start"));
    assertTrue(entity.hasNoHandlers("height", "end"));
    assertTrue(entity.hasNoHandlers("height", "constant"));
  }

  /**
   * Helper method to compute attributes without handlers map.
   */
  private static Map<String, Set<String>> computeOptimizationMap(
      String name,
      Map<EventKey, EventHandlerGroup> handlers,
      Map<String, EngineValue> attributes) {
    // Use EntityBuilder to compute the optimization map
    EntityBuilder builder = new EntityBuilder();
    builder.setName(name);
    handlers.forEach(builder::addEventHandlerGroup);
    attributes.forEach(builder::addAttribute);

    // Invoke private method via reflection to get the precomputed map
    // In real code, EntityBuilder.build methods do this automatically
    try {
      java.lang.reflect.Method method = EntityBuilder.class.getDeclaredMethod(
          "computeAttributesWithoutHandlersBySubstep");
      method.setAccessible(true);
      return (Map<String, Set<String>>) method.invoke(builder);
    } catch (Exception e) {
      throw new RuntimeException("Failed to compute optimization map", e);
    }
  }

  /**
   * Concrete test implementation of DirectLockMutableEntity for testing.
   */
  private static class TestDirectLockMutableEntity extends DirectLockMutableEntity {
    public TestDirectLockMutableEntity(
        String name,
        Map<EventKey, EventHandlerGroup> handlers,
        Map<String, EngineValue> attributes) {
      super(name, handlers, attributes, computeOptimizationMap(name, handlers, attributes),
          Collections.emptyMap());
    }

    @Override
    public EntityType getEntityType() {
      return EntityType.PATCH;
    }

    @Override
    public Optional<EngineGeometry> getGeometry() {
      return Optional.empty();
    }
  }
}
