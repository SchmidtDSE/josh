/**
 * Structures describing a cell within a simulation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.type;

import org.joshsim.engine.entity.base.EntityInitializationInfo;
import org.joshsim.engine.entity.base.RootSpatialEntity;
import org.joshsim.engine.geometry.EngineGeometry;


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
   * @param initInfo The initialization information containing all shared entity configuration.
   */
  public Patch(EngineGeometry geometry, EntityInitializationInfo initInfo) {
    super(geometry, initInfo);
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.PATCH;
  }
}
