/**
 * Base entity which is a mutable entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.base;

import java.util.Optional;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Represents a base entity that is mutable.
 * This class provides mechanisms for managing attributes and event handlers,
 * and supports locking to be thread-safe.
 */
public interface Entity {

  /**
   * Get the geographic location of this spatial entity.
   *
   * @return The geographic point representing this entity's location.
   */
  Optional<EngineGeometry> getGeometry();

  /**
   * Get the name of this type of entity.
   *
   * @returns unique name of this entity type.
   */
  String getName();

  /**
   * Get the value of an attribute by name.
   *
   * @param name the attribute name
   * @return an Optional containing the attribute value, or empty if not found
   */
  Optional<EngineValue> getAttributeValue(String name);

  /**
   * Get the names of all attributes associated with this entity.
   *
   * @return All attribute names available on this entity as strings. This may include non-
   *     initialized names.
   */
  Iterable<String> getAttributeNames();

  /**
   * Get the type of this entity.
   *
   * @return The EntityType for this entity.
   */
  EntityType getEntityType();

  /**
   * An immutable copy of this entity's attributes or a reference to this entity if already frozen.
   *
   * @return An immutable FrozenEntity containing the attributes of this entity.
   */
  Entity freeze();

  /**
   * Get a key that uniquely identifies the location of this entity within a replicate.
   *
   * @return Uniquely identifying key which can be hashed and used in equality operations or empty
   *     if there is no location associated with this entity.
   */
  Optional<GeoKey> getKey();
}
