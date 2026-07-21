
/**
 * Tests for EntityPrototypeStoreBuilder.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.prototype;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.joshsim.engine.entity.base.EntityBuilder;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.value.engine.ValueSupportFactory;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for duplicate-name rejection when building an EntityPrototypeStore.
 */
class EntityPrototypeStoreBuilderTest {

  private EntityPrototype prototype(String name) {
    return new ParentlessEntityPrototype(
        name, EntityType.AGENT, new EntityBuilder(new ValueSupportFactory())
    );
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

}
