/**
 * Base agent which is a mutable entity found in space.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.type;

import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.EntityInitializationInfo;
import org.joshsim.engine.entity.base.MemberSpatialEntity;

/**
 * Mutable spatial entity.
 *
 * <p>Represent an agent entity in the system where agents are spatial entities that can perform
 * actions and interact with their environment.</p>
 */
public class Agent extends MemberSpatialEntity {
  /**
   * Create a new agent with the given geometry.
   *
   * @param parent The parent containing this entity.
   * @param initInfo The initialization information containing all shared entity configuration.
   */
  public Agent(Entity parent, EntityInitializationInfo initInfo) {
    super(parent, initInfo);
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.AGENT;
  }
}
