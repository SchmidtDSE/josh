/**
 * Structures describing a simulation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.simulation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.base.DirectLockMutableEntity;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Simulation entity with cross-timestep attributes.
 */
public class Simulation extends DirectLockMutableEntity {

  /**
   * Constructor for a Simulation, which contains 'meta' attributes and event handlers.
   *
   * @param name Name of the entity.
   * @param eventHandlerGroups An immutable map of event keys to their corresponding
   *     EventHandlerGroups. This map is shared across all instances of this entity type
   *     for performance.
   * @param attributes An array of EngineValue objects indexed by attributeNameToIndex.
   * @param attributeNameToIndex Shared immutable map from attribute name to array index.
   * @param attributesWithoutHandlersBySubstep Precomputed map of attributes without
   *     handlers per substep.
   * @param commonHandlerCache Precomputed map of all handler lookups, shared across
   *     all instances of this entity type.
   * @param sharedAttributeNames Precomputed immutable set of attribute names, shared
   *     across all instances of this entity type.
   */
  public Simulation(
      String name,
      Map<EventKey, EventHandlerGroup> eventHandlerGroups,
      EngineValue[] attributes,
      Map<String, Integer> attributeNameToIndex,
      Map<String, Set<String>> attributesWithoutHandlersBySubstep,
      Map<String, List<EventHandlerGroup>> commonHandlerCache,
      Set<String> sharedAttributeNames
  ) {
    super(name, eventHandlerGroups, attributes, attributeNameToIndex,
        attributesWithoutHandlersBySubstep, commonHandlerCache, sharedAttributeNames);
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.SIMULATION;
  }

  @Override
  public Optional<EngineGeometry> getGeometry() {
    return Optional.empty();
  }
}
