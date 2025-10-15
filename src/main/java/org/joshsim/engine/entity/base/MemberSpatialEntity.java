/**
 * A spatial entity which has geometry inherited from a parent entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.base;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;


/**
 * An entity with spatial properties in the system as a member of another entity.
 *
 * <p>A type of entity which has a specific location in geospace that can be used to find co-located
 * entities and spatial information. This specifically refers to those which recieve geometry by
 * being part of another entity like a Patch.</p>
 */
public abstract class MemberSpatialEntity extends DirectLockMutableEntity {

  private final Entity parent;

  /**
   * Create a new spatial entity with the given location.
   *
   * @param parent The parent entity like Patch which houses this entity.
   * @param name The name of the spatial entity.
   * @param eventHandlerGroups An immutable map of event keys to their corresponding
   *     event handler groups. This map is shared across all instances of this entity type.
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
  public MemberSpatialEntity(
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
    super(name, eventHandlerGroups, attributes, attributeNameToIndex,
        indexToAttributeName, attributesWithoutHandlersBySubstep, commonHandlerCache,
        sharedAttributeNames);
    this.parent = parent;
  }

  /**
   * Get the geographic location of this spatial entity.
   *
   * @return the geographic point representing this entity's location
   */
  @Override
  public Optional<EngineGeometry> getGeometry() {
    return parent.getGeometry();
  }


  /**
   * Get the entity that houses this entity.
   *
   * @return The parent which houses this entity.
   */
  public Entity getParent() {
    return parent;
  }
}
