/**
 * Structures to help build a replicate.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.simulation;

import org.joshsim.engine.entity.EntityBuilder;


/**
 * Builder of replicates which can be used to bulid for each step.
 */
public interface ReplicateBuilder {
  /**
   * Add an entity builder to the replicate being constructed.
   *
   * @param entity the entity builder to add
   * @return this builder for method chaining
   */
  ReplicateBuilder addEntity(EntityBuilder entity);

  /**
   * Finalize the building process and returns the constructed Replicate.
   *
   * @return the constructed Replicate object
   */
  Replicate step();
}