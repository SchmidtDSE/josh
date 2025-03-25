/**
 * Strcture describing entities which are in geospace.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.Optional;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.EngineValue;


/**
 * An entity with spatial properties in the system.
 *
 * <p>A type of entity which has a specific location in geospace that can be used to find co-located
 * entities and spatial information.
 * </p>
 */
public abstract class SpatialEntity implements Entity {
  private final Geometry geometry;

  /**
   * Create a new spatial entity with the given location.
   *
   * @param geometry the geographic location of this entity
   */
  public SpatialEntity(Geometry geometry) {
    this.geometry = geometry;
  }

  /**
   * Get the geographic location of this spatial entity.
   *
   * @return the geographic point representing this entity's location
   */
  public Geometry getGeometry() {
    return geometry;
  }

  @Override
  public Iterable<EventHandlerGroup> getEventHandlers() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getEventHandlers'");
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

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getName'");
  }
}
