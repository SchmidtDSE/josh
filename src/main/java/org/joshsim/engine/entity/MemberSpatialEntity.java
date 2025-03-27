/**
 * A spatial entity which has geometry inherited from a parent entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.HashMap;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.EngineValue;


/**
 * An entity with spatial properties in the system as a member of another entity.
 *
 * <p>A type of entity which has a specific location in geospace that can be used to find co-located
 * entities and spatial information. This specifically refers to those which recieve geometry by
 * being part of another entity like a Patch.</p>
 */
public abstract class MemberSpatialEntity extends SpatialEntity {
  
  private final SpatialEntity parent;

  /**
   * Create a new spatial entity with the given location.
   *
   * @param parent The parent entity like Patch which houses this entity.
   * @param name The name of the spatial entity.
   * @param eventHandlerGroups A map of event keys to their corresponding event handler groups.
   * @param attributes A map of attribute names to their corresponding engine values.
   */
  public MemberSpatialEntity(
      SpatialEntity parent,
      String name,
      HashMap<EventKey, EventHandlerGroup> eventHandlerGroups,
      HashMap<String, EngineValue> attributes
  ) {
    super(name, eventHandlerGroups, attributes);
    this.parent = parent;
  }

  /**
   * Get the geographic location of this spatial entity.
   *
   * @return the geographic point representing this entity's location
   */
  @Override
  public Geometry getGeometry() {
    return parent.getGeometry();
  }

  
  /**
   * Get the entity that houses this entity.
   *
   * @return The parent which houses this entity.
   */
  public SpatialEntity getParent() {
    return parent;
  }
}
