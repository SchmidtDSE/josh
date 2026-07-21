
/**
 * Utility to merge an {@code update} declaration onto a prior same-named entity prototype.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.prototype;

import org.joshsim.engine.entity.base.EntityBuilder;


/**
 * Combines two entity prototypes for an {@code update} declaration.
 */
public final class EntityPrototypeCombineUtil {

  private EntityPrototypeCombineUtil() {}

  /**
   * Combine a prior entity prototype with an {@code update} override.
   *
   * @param base The prior entity prototype being updated.
   * @param override The {@code update} declaration's own prototype, whose handlers take priority.
   * @return A new prototype merging both, under the same name and entity type as base.
   * @throws IllegalArgumentException if either prototype was not built directly from a stanza
   *     (i.e. is not a {@link ParentlessEntityPrototype}), which should never happen for entities
   *     reaching this point at program-build time.
   */
  public static EntityPrototype combine(EntityPrototype base, EntityPrototype override) {
    if (!(base instanceof ParentlessEntityPrototype baseProto)
        || !(override instanceof ParentlessEntityPrototype overrideProto)) {
      throw new IllegalArgumentException(
          "Can only combine entity prototypes built directly from a stanza.");
    }

    EntityBuilder combinedBuilder = baseProto.getEntityBuilder()
        .combineWith(overrideProto.getEntityBuilder());

    return new ParentlessEntityPrototype(base.getIdentifier(), base.getEntityType(),
        combinedBuilder);
  }

}
