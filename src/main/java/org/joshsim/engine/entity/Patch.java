/**
 * Structures describing a cell within a simulation.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.Map;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.EngineValue;

/**
 * Spatial entity representing a patch in a simulation.
 *
 * <p>A patch is a spatial unit that can contain other entities which operates effectively as a cell
 * within the JoshSim gridded simulation.
 * </p>
 */
public class Patch extends RootSpatialEntity {

  /**
   * Create a new patch.
   *
   * @param geometry The geometry of the patch.
   * @param name The name of the spatial entity.
   * @param eventHandlerGroups A map of event keys to their corresponding event handler groups.
   * @param attributes A map of attribute names to their corresponding engine values.
   */
  public Patch(
      Geometry geometry,
      String name,
      Map<EventKey, EventHandlerGroup> eventHandlerGroups,
      Map<String, EngineValue> attributes
  ) {
    super(geometry, name, eventHandlerGroups, attributes);
  }

  /**
   * Get the geometry of the patch.
   */
  public Geometry getPatchGeometry() {
    return getGeometry();
  }
}
