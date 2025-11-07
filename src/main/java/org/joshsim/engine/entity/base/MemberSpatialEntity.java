/**
 * A spatial entity which has geometry inherited from a parent entity.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.base;

import java.util.Optional;
import org.joshsim.engine.geometry.EngineGeometry;


/**
 * An entity with spatial properties in the system as a member of another entity.
 *
 * <p>A type of entity which has a specific location in geospace that can be used to find co-located
 * entities and spatial information. This specifically refers to those which recieve geometry by
 * being part of another entity like a Patch.</p>
 */
public abstract class MemberSpatialEntity extends DirectLockMutableEntity {

  private final Entity parent;
  private final long sequenceId;

  /**
   * Create a new spatial entity with the given location and sequence.
   *
   * @param parent The parent entity like Patch which houses this entity.
   * @param initInfo The initialization information containing all shared entity configuration.
   * @param sequenceId The sequence ID for this entity at this location.
   */
  public MemberSpatialEntity(Entity parent, EntityInitializationInfo initInfo, long sequenceId) {
    super(initInfo);
    this.parent = parent;
    this.sequenceId = sequenceId;
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

  /**
   * Get the sequence ID for this entity.
   *
   * @return The sequence ID distinguishing this entity from others at the same location.
   */
  @Override
  public long getSequenceId() {
    return sequenceId;
  }
}
