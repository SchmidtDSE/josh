/**
 * Strcture describing entities which are in geospace.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;


import java.util.Map;
import java.util.Optional;

import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.EngineValue;

/**
 * An entity with spatial properties in the system.
 *
 * <p>A type of entity which has a specific location in geospace that can be used to find co-located
 * entities and spatial information.
 * </p>
 */
public abstract class SpatialEntity extends MutableEntity {

  /**
   * Constructs a SpatialEntity with the specified name, event handler groups, and attributes.
   *
   * @param name The name of the spatial entity.
   * @param eventHandlerGroups A map of event keys to their corresponding event handler groups.
   * @param attributes A map of attribute names to their corresponding engine values.
   */
  public SpatialEntity(
      String name,
      Map<EventKey, EventHandlerGroup> eventHandlerGroups,
      Map<String, EngineValue> attributes
  ) {
    super(name, eventHandlerGroups, attributes);
  }

}
