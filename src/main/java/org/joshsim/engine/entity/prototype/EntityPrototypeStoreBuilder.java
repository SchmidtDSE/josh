/**
 * Structure to assist in constructing an EntityPrototypeStore.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.prototype;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder class for constructing an EntityPrototypeStore.
 */
public class EntityPrototypeStoreBuilder {

  private final Map<String, EntityPrototype> prototypes;

  /**
   * Creates a new EntityPrototypeStoreBuilder with an empty prototype collection.
   */
  public EntityPrototypeStoreBuilder() {
    prototypes = new HashMap<>();
  }

  /**
   * Adds a newly declared ({@code start}) entity prototype to the store being built.
   *
   * @param prototype The EntityPrototype to be added to the store.
   * @throws IllegalStateException if an entity with the same name was already added.
   */
  public void add(EntityPrototype prototype) {
    String name = prototype.getIdentifier();
    if (prototypes.containsKey(name)) {
      throw new IllegalStateException("Entity \"" + name + "\" is already defined.");
    }
    prototypes.put(name, prototype);
  }

  /**
   * Fully replaces a prior same-named entity prototype ({@code replace}).
   *
   * @param prototype The replacement EntityPrototype.
   * @throws IllegalStateException if no prior entity of the same name exists, or if it is of a
   *     different entity type.
   */
  public void replace(EntityPrototype prototype) {
    requireExisting(prototype, "replace");
    prototypes.put(prototype.getIdentifier(), prototype);
  }

  /**
   * Merges an {@code update} declaration onto a prior same-named entity prototype.
   *
   * @param prototype The {@code update} declaration's own EntityPrototype, whose handlers take
   *     priority over the prior entity's matching handlers.
   * @throws IllegalStateException if no prior entity of the same name exists, or if it is of a
   *     different entity type.
   */
  public void update(EntityPrototype prototype) {
    EntityPrototype prior = requireExisting(prototype, "update");
    prototypes.put(prototype.getIdentifier(), EntityPrototypeCombineUtil.combine(prior, prototype));
  }

  /**
   * Look up the prior entity a {@code replace} or {@code update} declaration refers to.
   *
   * @param prototype The declaration's own EntityPrototype.
   * @param verb The declaration keyword, used only to phrase the error message.
   * @return The prior entity prototype of the same name.
   * @throws IllegalStateException if no prior entity of the same name exists, or if it is of a
   *     different entity type.
   */
  private EntityPrototype requireExisting(EntityPrototype prototype, String verb) {
    String name = prototype.getIdentifier();
    EntityPrototype prior = prototypes.get(name);
    if (prior == null) {
      throw new IllegalStateException(
          "Cannot " + verb + " entity \"" + name + "\"; no prior definition exists.");
    }
    if (prior.getEntityType() != prototype.getEntityType()) {
      throw new IllegalStateException(String.format(
          "Cannot %s entity \"%s\" as entity type %s; it was originally defined as %s.",
          verb, name, prototype.getEntityType(), prior.getEntityType()));
    }
    return prior;
  }

  /**
   * Retrieves an entity prototype by name from the store being built.
   *
   * @param name The identifier of the entity prototype to retrieve.
   * @return The EntityPrototype associated with the given name.
   * @throws IllegalArgumentException if the entity name is not found in the store.
   */
  public EntityPrototype get(String name) {
    if (!prototypes.containsKey(name)) {
      throw new IllegalArgumentException("Unknown entity type: " + name);
    }
    return prototypes.get(name);
  }

  /**
   * Constructs and returns a new EntityPrototypeStore containing all added prototypes.
   *
   * @return A new MapEntityPrototypeStore instance containing all prototypes given to this builder.
   */
  public EntityPrototypeStore build() {
    return new MapEntityPrototypeStore(prototypes);
  }

}
