/**
 * Structures describing a simulation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.Map;
import org.joshsim.engine.value.EngineValue;

/**
 * Simulation entity with cross-timestep attributes.
 */
public class Simulation extends MutableEntity {

  /**
   * Constructor for a Simulation, which contains 'meta' attributes and event handlers.
   *
   * @param name Name of the entity.
   * @param eventHandlerGroups A map of event keys to their corresponding EventHandlerGroups.
   * @param attributes A map of attribute names to their corresponding EngineValues.
   */
  public Simulation(
      String name,
      Map<EventKey, EventHandlerGroup> eventHandlerGroups,
      Map<String, EngineValue> attributes
  ) {
    super(name, eventHandlerGroups, attributes);
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.SIMULATION;
  }
}
