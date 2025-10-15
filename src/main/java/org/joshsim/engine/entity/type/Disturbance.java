/**
 * Structures to model a disturbance through spatial entities.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.type;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MemberSpatialEntity;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.value.type.EngineValue;

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
   * @param eventHandlerGroups An immutable map of event keys to their corresponding
   *     event handler groups. This map is shared across all instances of this entity
   *     type for performance.
   * @param attributes An array of EngineValue objects indexed by attributeNameToIndex.
   * @param attributeNameToIndex Shared immutable map from attribute name to array index.
   * @param indexToAttributeName Shared immutable array from index to attribute name.
   * @param attributesWithoutHandlersBySubstep Precomputed map of attributes without
   *     handlers per substep.
   * @param commonHandlerCache Precomputed map of all handler lookups, shared across
   *     all instances of this entity type.
   * @param sharedAttributeNames Precomputed immutable set of attribute names, shared
   *     across all instances of this entity type.
   */
  public Disturbance(
      Entity parent,
      String name,
      Map<EventKey, EventHandlerGroup> eventHandlerGroups,
      EngineValue[] attributes,
      Map<String, Integer> attributeNameToIndex,
      String[] indexToAttributeName,
      Map<String, Set<String>> attributesWithoutHandlersBySubstep,
      Map<String, List<EventHandlerGroup>> commonHandlerCache,
      Set<String> sharedAttributeNames
  ) {
    super(parent, name, eventHandlerGroups, attributes, attributeNameToIndex,
        indexToAttributeName, attributesWithoutHandlersBySubstep, commonHandlerCache,
        sharedAttributeNames);
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.DISTURBANCE;
  }
}
