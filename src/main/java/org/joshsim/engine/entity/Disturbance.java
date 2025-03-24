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

  @Override
  public Iterable<EventHandlerGroup> getEventHandlers(String attribute, String event) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getEventHandlers'");
  }

  @Override
  public Optional<EngineValue> getAttributeValue(String name) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getAttributeValue'");
  }

  @Override
  public void setAttributeValue(String name, EngineValue value) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'setAttributeValue'");
  }

  @Override
  public void lock() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'lock'");
  }

  @Override
  public void unlock() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'unlock'");
  }
}
