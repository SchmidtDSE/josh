/**
 * Structures to model a disturbance through spatial entities.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.type;

import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.EntityInitializationInfo;
import org.joshsim.engine.entity.base.MemberSpatialEntity;

/**
 * Mutable spatial entity which represents a distrubance.
 *
 * <p>Agent representing a disturbance entity in the system. Disturbances are events that can affect
 * other entities in the environment such as a fire or a management intervention.</p>
 */
public class Disturbance extends MemberSpatialEntity {

  /**
   * Constructs a disturbance entity with the given parent and sequence.
   *
   * @param parent The parent containing this entity.
   * @param initInfo The initialization information containing all shared entity configuration.
   * @param sequenceId The sequence ID for this disturbance at this location.
   */
  public Disturbance(Entity parent, EntityInitializationInfo initInfo, long sequenceId) {
    super(parent, initInfo, sequenceId);
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.DISTURBANCE;
  }
}
