/**
 * Structures describing a read only external data soruce.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import java.util.HashMap;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.Distribution;
import org.joshsim.engine.value.EngineValue;


/**
 * Immutable external data source.
 *
 * <p>Represents an external resource entity in the system which provides access to distributed
 * values based on a geometry and attribute-sensitive paths.</p>
 */
public abstract class ExternalResource extends MutableEntity {
  /**
   * Constructor for a ExternalResource, which allows access to external data to influence Entities.
   *
   * @param name Name of the entity.
   * @param eventHandlerGroups A map of event keys to their corresponding EventHandlerGroups.
   * @param attributes A map of attribute names to their corresponding EngineValues.
   */
  public ExternalResource(
      String name,
      HashMap<EventKey, EventHandlerGroup> eventHandlerGroups,
      HashMap<String, EngineValue> attributes
  ) {
    super(name, eventHandlerGroups, attributes);
  }

  /**
   * Get distribution values for the specified geometry.
   *
   * @param geometry the geometry to query values for
   * @return the distribution of values for the given geometry
   */
  abstract Distribution getDistribution(Geometry geometry);

  @Override
  public EntityType getEntityType() {
    return EntityType.EXTERNAL_RESOURCE;
  }
}
