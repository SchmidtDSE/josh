/**
 * Structures describing a simulation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.simulation;

import java.util.Map;
import java.util.Optional;
import org.joshsim.engine.entity.base.DirectLockMutableEntity;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Simulation entity with cross-timestep attributes.
 */
public class Simulation extends DirectLockMutableEntity {

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

  @Override
  public Optional<Geometry> getGeometry() {
    return Optional.empty();
  }
}
