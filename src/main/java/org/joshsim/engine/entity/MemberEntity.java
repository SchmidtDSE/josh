/**
 * Strcture describing entities which are in patches.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import org.joshsim.engine.geometry.Geometry;


/**
 * An entity with spatial properties in the system as a member of another entity.
 *
 * <p>A type of entity which has a specific location in geospace that can be used to find co-located
 * entities and spatial information. This specifically refers to those which recieve geometry by
 * being part of another entity like a Patch.</p>
 */
public abstract class MemberEntity extends SpatialEntity {
  private final SpatialEntity parent;

  /**
   * Create a new spatial entity with the given location.
   *
   * @param path The parent entity like Patch which houses this entity.
   */
  public MemberEntity(SpatialEntity parent) {
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
