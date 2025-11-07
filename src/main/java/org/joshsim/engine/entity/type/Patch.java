/**
 * Structures describing a cell within a simulation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.type;

import java.util.concurrent.atomic.AtomicLong;
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

  private final AtomicLong nextSequenceAtLocation = new AtomicLong(0);

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

  /**
   * Get the next sequence ID for an entity being created at this location.
   *
   * <p>This method is thread-safe and returns monotonically increasing sequence IDs
   * starting from 0. Each call increments the counter.</p>
   *
   * @return The next sequence ID for this location.
   */
  public long getNextSequence() {
    return nextSequenceAtLocation.getAndIncrement();
  }
}
