/**
 * Base agent which is a mutable entity found in space.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.Optional;

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
   */
  public Agent(SpatialEntity parent, Optional<EventHandlerGroup> eventHandlerGroup) {
    super(parent);
  }
}
