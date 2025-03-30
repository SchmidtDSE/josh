/**
 * Base agent which is a mutable entity found in space.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.Map;
import org.joshsim.engine.value.EngineValue;

/**
 * Mutable spatial entity.
 *
 * <p>Represent an agent entity in the system where agents are spatial entities that can perform
 * actions and interact with their environment.</p>
 */
public class Agent extends MemberSpatialEntity {
  /**
   * Create a new agent with the given geometry.
   *
   * @param parent The parent containing this entity.
   * @param name The name of the spatial entity.
   * @param eventHandlerGroups A map of event keys to their corresponding event handler groups.
   * @param attributes A map of attribute names to their corresponding engine values.
   */
  public Agent(
      Entity parent,
      String name,
      Map<EventKey, EventHandlerGroup> eventHandlerGroups,
      Map<String, EngineValue> attributes
  ) {
    super(parent, name, eventHandlerGroups, attributes);
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.AGENT;
  }
}
