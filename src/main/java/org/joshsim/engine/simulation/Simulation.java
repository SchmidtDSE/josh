/**
 * Structures describing a simulation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.simulation;

import java.util.Optional;
import org.joshsim.engine.entity.base.DirectLockMutableEntity;
import org.joshsim.engine.entity.base.EntityInitializationInfo;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.geometry.EngineGeometry;

/**
 * Simulation entity with cross-timestep attributes.
 */
public class Simulation extends DirectLockMutableEntity {

  /**
   * Constructor for a Simulation, which contains 'meta' attributes and event handlers.
   *
   * @param initInfo The initialization information containing all shared entity configuration.
   */
  public Simulation(EntityInitializationInfo initInfo) {
    super(initInfo);
  }

  @Override
  public EntityType getEntityType() {
    return EntityType.SIMULATION;
  }

  @Override
  public Optional<EngineGeometry> getGeometry() {
    return Optional.empty();
  }
}
