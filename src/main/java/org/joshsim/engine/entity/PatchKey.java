
/**
 * Structures describing keys for cells within a simulation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import org.joshsim.engine.geometry.GeometryMomento;

/**
 * Represents a key to uniquely identify a Patch within a simulation.
 *
 * <p>The key consists of a GeometryMomento and a patch type, which together
 * provide a unique identifier for the patch. This key can be used for equality
 * checks and hashing within collections and maps for patches.</p>
 */
public class PatchKey {

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
  public PatchKey(GeometryMomento geometryMomento, String patchType) {
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
