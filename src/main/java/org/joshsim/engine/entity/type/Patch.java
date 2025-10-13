/**
 * Structures describing a cell within a simulation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.type;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joshsim.engine.entity.base.RootSpatialEntity;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Spatial entity representing a patch in a simulation.
 *
 * <p>A patch is a spatial unit that can contain other entities which operates effectively as a cell
 * within the JoshSim gridded simulation.
 * </p>
 */
public class Patch extends RootSpatialEntity {

  /**
   * Create a new patch.
   *
   * @param geometry The geometry of the patch.
   * @param name The name of the spatial entity.
   * @param eventHandlerGroups An immutable map of event keys to their corresponding
   *     event handler groups. This map is shared across all instances of this entity
   *     type for performance.
   * @param attributes An array of EngineValue objects indexed by attributeNameToIndex.
   * @param attributeNameToIndex Shared immutable map from attribute name to array index.
   * @param attributesWithoutHandlersBySubstep Precomputed map of attributes without
   *     handlers per substep.
   * @param commonHandlerCache Precomputed map of all handler lookups, shared across
   *     all instances of this entity type.
   */
  public Patch(
      EngineGeometry geometry,
      String name,
      Map<EventKey, EventHandlerGroup> eventHandlerGroups,
      EngineValue[] attributes,
      Map<String, Integer> attributeNameToIndex,
      Map<String, Set<String>> attributesWithoutHandlersBySubstep,
      Map<String, List<EventHandlerGroup>> commonHandlerCache
  ) {
    super(geometry, name, eventHandlerGroups, attributes, attributeNameToIndex,
        attributesWithoutHandlersBySubstep, commonHandlerCache);
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.PATCH;
  }
}
