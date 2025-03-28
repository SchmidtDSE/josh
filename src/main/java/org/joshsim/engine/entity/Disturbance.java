/**
 * Structures to model a disturbance through spatial entities.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.Map;
import org.joshsim.engine.value.EngineValue;

/**
 * Mutable spatial entity which represents a distrubance.
 *
 * <p>Agent representing a disturbance entity in the system. Disturbances are events that can affect
 * other entities in the environment such as a fire or a management intervention.</p>
 */
public class Disturbance extends MemberSpatialEntity {

  /**
   * Constructs a disturbance entity with the given geometry.
   *
   * @param parent The parent containing this entity.
   * @param name The name of the spatial entity.
   * @param eventHandlerGroups A map of event keys to their corresponding event handler groups.
   * @param attributes A map of attribute names to their corresponding engine values.
   */
  public Disturbance(
      SpatialEntity parent,
      String name,
      Map<EventKey, EventHandlerGroup> eventHandlerGroups,
      Map<String, EngineValue> attributes
  ) {
    super(parent, name, eventHandlerGroups, attributes);
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.DISTURBANCE;
  }
}
