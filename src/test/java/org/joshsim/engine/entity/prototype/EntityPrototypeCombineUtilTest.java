
/**
 * Tests for EntityPrototypeCombineUtil.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.prototype;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Unit tests for merging an {@code update} declaration onto a prior entity prototype.
 */
class EntityPrototypeCombineUtilTest {

  @Test
  void combineKeepsTheBaseIdentifierAndEntityType() {
    EntityBuilder baseBuilder = new EntityBuilder(new ValueSupportFactory());
    baseBuilder.setName("Tree");
    EntityPrototype base = new ParentlessEntityPrototype("Tree", EntityType.AGENT, baseBuilder);

    EntityBuilder overrideBuilder = new EntityBuilder(new ValueSupportFactory());
    overrideBuilder.setName("Tree");
    EntityPrototype override = new ParentlessEntityPrototype("Tree", EntityType.AGENT,
        overrideBuilder);

    EntityPrototype combined = EntityPrototypeCombineUtil.combine(base, override);

    assertEquals("Tree", combined.getIdentifier());
    assertEquals(EntityType.AGENT, combined.getEntityType());
  }

  @Test
  void combineRejectsPrototypeNotBuiltFromStanza() {
    EntityPrototype base = mock(EntityPrototype.class);
    EntityPrototype override = mock(EntityPrototype.class);

    assertThrows(IllegalArgumentException.class,
        () -> EntityPrototypeCombineUtil.combine(base, override));
  }

  @Test
  void combineMergesHandlersFromBothPrototypes() {
    EventKey ageKey = EventKey.of("age", "step");
    EventKey heightKey = EventKey.of("height", "step");
    EventHandlerGroup ageGroup = mock(EventHandlerGroup.class);
    EventHandlerGroup heightGroup = mock(EventHandlerGroup.class);

    EntityBuilder baseBuilder = new EntityBuilder(new ValueSupportFactory());
    baseBuilder.setName("Tree").addEventHandlerGroup(ageKey, ageGroup);
    EntityPrototype base = new ParentlessEntityPrototype("Tree", EntityType.AGENT, baseBuilder);

    EntityBuilder overrideBuilder = new EntityBuilder(new ValueSupportFactory());
    overrideBuilder.setName("Tree").addEventHandlerGroup(heightKey, heightGroup);
    EntityPrototype override = new ParentlessEntityPrototype("Tree", EntityType.AGENT,
        overrideBuilder);

    EntityPrototype combined = EntityPrototypeCombineUtil.combine(base, override);

    Entity parent = mock(Entity.class);
    MutableEntity entity = combined.buildSpatial(parent);
    assertEquals(ageGroup, entity.getEventHandlers(ageKey).orElseThrow());
    assertEquals(heightGroup, entity.getEventHandlers(heightKey).orElseThrow());
  }

}
