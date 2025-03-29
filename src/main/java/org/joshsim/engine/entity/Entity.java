/**
 * Base entity which is a mutable entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.EngineValue;


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
  Optional<Geometry> getGeometry();

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
   * @return An Iterable of attribute names as Strings
   */
  Iterable<String> getAttributeNames();

  /**
   * Get the type of this entity.
   *
   * @return The EntityType (AGENT, DISTURBANCE, EXTERNAL_RESOURCE, PATCH, SIMULATION, or REFERENCE)
   */
  EntityType getEntityType();

  /**
   * Creates an immutable copy of this entity.
   *
   * @return A new immutable FrozenEntity containing the current state
   */
  Entity freeze();

}
