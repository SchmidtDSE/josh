
/**
 * Tests for EntityPrototypeStoreBuilder.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.prototype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.EntityBuilder;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for duplicate-name rejection when building an EntityPrototypeStore.
 */
class EntityPrototypeStoreBuilderTest {

  private EntityPrototype prototype(String name) {
    return prototype(name, EntityType.AGENT);
  }

  private EntityPrototype prototype(String name, EntityType type) {
    EntityBuilder builder = new EntityBuilder(new ValueSupportFactory());
    builder.setName(name);
    return new ParentlessEntityPrototype(name, type, builder);
  }

  private EntityPrototype prototypeWithHandler(String name, EventKey key, EventHandlerGroup group) {
    EntityBuilder builder = new EntityBuilder(new ValueSupportFactory());
    builder.setName(name).addEventHandlerGroup(key, group);
    return new ParentlessEntityPrototype(name, EntityType.AGENT, builder);
  }

  @Test
  void addingTwoEntitiesWithDifferentNamesSucceeds() {
    EntityPrototypeStoreBuilder builder = new EntityPrototypeStoreBuilder();
    builder.add(prototype("Tree"));
    builder.add(prototype("Shrub"));

    EntityPrototypeStore store = builder.build();
    store.get("Tree");
    store.get("Shrub");
  }

  @Test
  void addingTheSameEntityNameTwiceThrows() {
    EntityPrototypeStoreBuilder builder = new EntityPrototypeStoreBuilder();
    builder.add(prototype("Tree"));

    assertThrows(IllegalStateException.class, () -> builder.add(prototype("Tree")));
  }

  @Test
  void replaceWithoutPriorEntityThrows() {
    EntityPrototypeStoreBuilder builder = new EntityPrototypeStoreBuilder();

    assertThrows(IllegalStateException.class, () -> builder.replace(prototype("Tree")));
  }

  @Test
  void replaceFullyOverwritesThePriorEntity() {
    EntityPrototypeStoreBuilder builder = new EntityPrototypeStoreBuilder();
    builder.add(prototype("Tree"));
    EntityPrototype replacement = prototype("Tree");
    builder.replace(replacement);

    assertSame(replacement, builder.build().get("Tree"));
  }

  @Test
  void replaceRejectsAnEntityTypeMismatch() {
    EntityPrototypeStoreBuilder builder = new EntityPrototypeStoreBuilder();
    builder.add(prototype("Tree", EntityType.AGENT));

    assertThrows(IllegalStateException.class,
        () -> builder.replace(prototype("Tree", EntityType.PATCH)));
  }

  @Test
  void updateWithoutPriorEntityThrows() {
    EntityPrototypeStoreBuilder builder = new EntityPrototypeStoreBuilder();

    assertThrows(IllegalStateException.class, () -> builder.update(prototype("Tree")));
  }

  @Test
  void updateRejectsAnEntityTypeMismatch() {
    EntityPrototypeStoreBuilder builder = new EntityPrototypeStoreBuilder();
    builder.add(prototype("Tree", EntityType.AGENT));

    assertThrows(IllegalStateException.class,
        () -> builder.update(prototype("Tree", EntityType.PATCH)));
  }

  @Test
  void updateMergesOntoThePriorEntityKeepingBothHandlers() {
    EventKey ageKey = EventKey.of("age", "step");
    EventKey heightKey = EventKey.of("height", "step");
    EventHandlerGroup ageGroup = mock(EventHandlerGroup.class);
    EventHandlerGroup heightGroup = mock(EventHandlerGroup.class);

    EntityPrototypeStoreBuilder builder = new EntityPrototypeStoreBuilder();
    builder.add(prototypeWithHandler("Tree", ageKey, ageGroup));
    builder.update(prototypeWithHandler("Tree", heightKey, heightGroup));

    Entity parent = mock(Entity.class);
    MutableEntity merged = builder.build().get("Tree").buildSpatial(parent);
    assertEquals(ageGroup, merged.getEventHandlers(ageKey).orElseThrow());
    assertEquals(heightGroup, merged.getEventHandlers(heightKey).orElseThrow());
  }

  @Test
  void updateLetsOverrideWinOnSharedHandlerKey() {
    EventKey heightKey = EventKey.of("height", "step");
    EventHandlerGroup baseGroup = mock(EventHandlerGroup.class);
    EventHandlerGroup overrideGroup = mock(EventHandlerGroup.class);

    EntityPrototypeStoreBuilder builder = new EntityPrototypeStoreBuilder();
    builder.add(prototypeWithHandler("Tree", heightKey, baseGroup));
    builder.update(prototypeWithHandler("Tree", heightKey, overrideGroup));

    Entity parent = mock(Entity.class);
    MutableEntity merged = builder.build().get("Tree").buildSpatial(parent);
    assertEquals(overrideGroup, merged.getEventHandlers(heightKey).orElseThrow());
  }

}
