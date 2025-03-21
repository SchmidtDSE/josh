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
public class Patch extends SpatialEntity {

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
  public Geometry getGeometry() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getGeometry'");
  }
}
