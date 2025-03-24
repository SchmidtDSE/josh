/**
 * Structures describing a cell within a simulation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.Optional;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.geometry.GeometryMomento;
import org.joshsim.engine.value.EngineValue;

/**
 * Spatial entity representing a patch in a simulation.
 *
 * <p>A patch is a spatial unit that can contain other entities which operates effectively as a cell
 * within the JoshSim gridded simulation.
 * </p>
 */
public abstract class Patch extends SpatialEntity {

  private final Key key;
  
  /**
   * Create a new patch.
   *
   * @param geometry The geometry of the patch.
   */
  public Patch(GeometryMomento geometryMomento) {
    super(geometryMomento.build());
    key = new Key(geometryMomento, getName());
  }

  /**
   * Get a key that uniquely identifies this patch within a replicate.
   *
   * @return Uniquely identifying key which can be hashed and used in equality operations.
   */
  public Key getKey() {
    return key;
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
  public Geometry getGeometry() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getGeometry'");
  }

  /**
   * Represents a key to uniquely identify a Patch within a simulation.
   *
   * <p>The key consists of a GeometryMomento and a patch type, which together
   * provide a unique identifier for the patch. This key can be used for equality
   * checks and hashing within collections and maps for patches.</p>
   */
  public class Key {

    private final GeometryMomento geometryMomento;
    private final String patchType;

    
    /**
     * Constructs a Key with the specified geometry momento and patch type.
     *
     * @param geometryMomento The geometry momento that reflects the state of the geometry at a 
     *     specific point in time.
     * @param patchType A string representing the type of patch for which the key is being 
     *     constructed.
     */
    public Key(GeometryMomento geometryMomento, String patchType) {
      this.geometryMomento = geometryMomento;
      this.patchType = patchType;
    }
    
    /**
     * Get the GeometryMomento associated with this key.
     *
     * @return The GeometryMomento that uniquely identifies the geometry of the patch.
     */
    public GeometryMomento getGeometryMomento() {
        return geometryMomento;
    }

    /**
     * Get the patch type associated with this key.
     *
     * @return A string representing the type of the patch.
     */
    public String getPatchType() {
        return patchType;
    }

  }
  
}
