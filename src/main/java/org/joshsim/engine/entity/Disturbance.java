/**
 * Structures to model a disturbance through spatial entities.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.Optional;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.EngineValue;

/**
 * Mutable spatial entity which represents a distrubance.
 *
 * <p>Agent representing a disturbance entity in the system. Disturbances are events that can affect
 * other entities in the environment such as a fire or a management intervention.</p>
 */
public class Disturbance extends SpatialEntity {

  /**
   * Constructs a disturbance entity with the given geometry.
   *
   * @param geometry the geometry of the disturbance
   */
  public Disturbance(Geometry geometry) {
    super(geometry);
  }
}
