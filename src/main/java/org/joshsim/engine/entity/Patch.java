/**
 * Structures describing a cell within a simulation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.Optional;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.EngineValue;

/**
 * Spatial entity representing a patch in a simulation.
 *
 * <p>A patch is a spatial unit that can contain other entities which operates effectively as a cell
 * within the JoshSim gridded simulation.
 * </p>
 */
public class Patch extends RootSpatialEntity {

  /**
   * Create a new patch.
   *
   * @param geometry The geometry of the patch.
   */
  public Patch(Geometry geometry) {
    super(geometry);
  }

  /**
   * Get the geometry of the patch.
   */
  public Geometry getPatchGeometry() {
    return getGeometry();
  }
}
