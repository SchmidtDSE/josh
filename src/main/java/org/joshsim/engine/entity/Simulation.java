/**
 * Structures describing a simulation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.HashMap;
import org.joshsim.engine.value.EngineValue;

/**
 * Simulation entity with cross-timestep attributes.
 */
public class Simulation extends Entity {

  /**
   * Constructor for a Simulation, which contains 'meta' attributes and event handlers.
   *
   * @param name Name of the entity.
   * @param eventHandlerGroups A map of event keys to their corresponding EventHandlerGroups.
   * @param attributes A map of attribute names to their corresponding EngineValues.
   */
  public Simulation(
      String name,
      HashMap<EventKey, EventHandlerGroup> eventHandlerGroups,
      HashMap<String, EngineValue> attributes
  ) {
    super(name, eventHandlerGroups, attributes);
  }
}
