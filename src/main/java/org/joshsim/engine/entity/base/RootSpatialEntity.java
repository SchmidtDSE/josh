/**
 * A spatial entity with its own geometry.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.base;

import java.util.Map;
import java.util.Optional;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.geometry.Geometry;
import org.joshsim.engine.value.type.EngineValue;

/**
 * RootSpatialEntity is a type of SpatialEntity which has its own geometry.
 *
 * <p>RootSpatialEntity is a type of SpatialEntity which has its own geometry as opposed to
 * inhering that geometry by being part of another entity.</p>
 */
public abstract class RootSpatialEntity extends MutableEntity {
  private final Geometry geometry;

  /**
   * Constructs a RootSpatialEntity with the specified geometry.
   *
   * @param geometry the geometry associated with this entity.
   * @param name The name of the spatial entity.
   * @param eventHandlerGroups A map of event keys to their corresponding event handler groups.
   * @param attributes A map of attribute names to their corresponding engine values.
   */
  public RootSpatialEntity(
      Geometry geometry,
      String name,
      Map<EventKey, EventHandlerGroup> eventHandlerGroups,
      Map<String, EngineValue> attributes
  ) {
    super(name, eventHandlerGroups, attributes);
    this.geometry = geometry;
  }

  @Override
  public Optional<Geometry> getGeometry() {
    return Optional.of(geometry);
  }
}
