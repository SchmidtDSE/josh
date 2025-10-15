/**
 * A spatial entity with its own geometry.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.base;

import java.util.Optional;
import org.joshsim.engine.geometry.EngineGeometry;

/**
 * RootSpatialEntity is a type of SpatialEntity which has its own geometry.
 *
 * <p>RootSpatialEntity is a type of SpatialEntity which has its own geometry as opposed to
 * inhering that geometry by being part of another entity.</p>
 */
public abstract class RootSpatialEntity extends DirectLockMutableEntity {
  private final EngineGeometry geometry;

  /**
   * Constructs a RootSpatialEntity with the specified geometry.
   *
   * @param geometry the geometry associated with this entity.
   * @param initInfo The initialization information containing all shared entity configuration.
   */
  public RootSpatialEntity(EngineGeometry geometry, EntityInitializationInfo initInfo) {
    super(initInfo);
    this.geometry = geometry;
  }

  @Override
  public Optional<EngineGeometry> getGeometry() {
    return Optional.of(geometry);
  }
}
