/**
 * Structures describing keys for cells within a simulation across time steps.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import org.joshsim.engine.geometry.Geometry;


/**
 * Represents a key to uniquely identify a Patch within a simulation across time steps.
 */
public class PatchKey {

  private final Patch patch;

  /**
   * Constructs a Key with the specified patch.
   *
   * @param patch The patch to be keyed.
   */
  public PatchKey(Patch patch) {
    this.patch = patch;
  }

  @Override
  public String toString() {
    Geometry geometry = patch.getGeometry();
    return String.format(
      "Patch of type %s at (%.6f, %.6f)",
      patch.getName(),
      geometry.getCenterX(),
      geometry.getCenterY()
    );
  }
}
