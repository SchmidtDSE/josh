/**
 * Base agent which is a mutable entity found in space.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.type;

import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.EntityBuilder;
import org.joshsim.engine.entity.base.MemberSpatialEntity;

/**
 * Mutable spatial entity.
 *
 * <p>Represent an agent entity in the system where agents are spatial entities that can perform
 * actions and interact with their environment.</p>
 */
public class Agent extends MemberSpatialEntity {
  /**
   * Create a new agent with the given parent.
   *
   * @param parent The parent containing this entity.
   * @param builder The entity builder containing configuration for this agent.
   */
  public Agent(Entity parent, EntityBuilder builder) {
    super(parent, builder);
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.AGENT;
  }
}
