/**
 * Base agent which is a mutable entity found in space.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.Optional;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.EngineValue;

/**
 * Mutable spatial entity.
 *
 * <p>Represent an agent entity in the system where agents are spatial entities that can perform
 * actions and interact with their environment.</p>
 */
public class Agent extends SpatialEntity {

  /**
   * Create a new agent with the given geometry.
   *
   * @param geometry The geometry of the agent.
   */
  public Agent(Geometry geometry) {
    super(geometry);
  }
}
