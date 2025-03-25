/**
 * Structures describing a cell within a simulation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import org.joshsim.engine.geometry.Geometry;

/**
 * Spatial entity representing a patch in a simulation.
 *
 * <p>A patch is a spatial unit that can contain other entities which operates effectively as a cell
 * within the JoshSim gridded simulation.
 * </p>
 */
public class Patch extends SpatialEntity {

  /**
   * Create a new patch.
   *
   * @param geometry The geometry of the patch.
   */
  public Patch(Geometry geometry) {
    super(geometry);
  }

  /**
   * Get a key that uniquely identifies this patch within a replicate.
   *
   * @return Uniquely identifying key which can be hashed and used in equality operations.
   */
  public PatchKey getKey() {
    return new PatchKey(this);
  }
}
