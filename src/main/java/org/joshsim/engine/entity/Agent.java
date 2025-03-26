/**
 * Base agent which is a mutable entity found in space.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.Optional;
import org.joshsim.engine.value.EngineValue;

/**
 * Mutable spatial entity.
 *
 * <p>Represent an agent entity in the system where agents are spatial entities that can perform
 * actions and interact with their environment.</p>
 */
public class Agent extends MemberSpatialEntity {

  /**
   * Create a new agent with the given geometry.
   *
   * @param parent The parent containing this entity.
   */
  public Agent(SpatialEntity parent) {
    super(parent);
  }

  @Override
  public Iterable<EventHandlerGroup> getEventHandlers(String attribute, String event) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getEventHandlers'");
  }

  @Override
  public Iterable<EventHandlerGroup> getEventHandlers() {
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

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getName'");
  }

}
