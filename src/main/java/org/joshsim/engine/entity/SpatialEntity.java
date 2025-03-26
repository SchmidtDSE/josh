/**
 * Strcture describing entities which are in geospace.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import org.joshsim.engine.geometry.Geometry;


/**
 * An entity with spatial properties in the system.
 *
 * <p>A type of entity which has a specific location in geospace that can be used to find co-located
 * entities and spatial information.
 * </p>
 */
public interface SpatialEntity extends Entity {

  /**
   * Get the geographic location of this spatial entity.
   *
   * @return The geographic point representing this entity's location.
   */
  Geometry getGeometry();
}
