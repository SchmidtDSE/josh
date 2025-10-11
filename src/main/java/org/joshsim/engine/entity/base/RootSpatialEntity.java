/**
 * A spatial entity with its own geometry.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.base;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.entity.handler.EventHandlerGroup;
import org.joshsim.engine.entity.handler.EventKey;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;

/**
 * RootSpatialEntity is a type of SpatialEntity which has its own geometry.
 *
 * <p>RootSpatialEntity is a type of SpatialEntity which has its own geometry as opposed to
 * inhering that geometry by being part of another entity.</p>
 */
public abstract class RootSpatialEntity extends DirectLockMutableEntity {
  private final EngineGeometry geometry;

  /**
   * Constructs a RootSpatialEntity with the specified geometry.
   *
   * @param geometry the geometry associated with this entity.
   * @param name The name of the spatial entity.
   * @param eventHandlerGroups An immutable map of event keys to their corresponding
   *     event handler groups. This map is shared across all instances of this entity type.
   * @param attributes A map of attribute names to their corresponding engine values.
   * @param attributesWithoutHandlersBySubstep Precomputed map of attributes without
   *     handlers per substep.
   */
  public RootSpatialEntity(
      EngineGeometry geometry,
      String name,
      Map<EventKey, EventHandlerGroup> eventHandlerGroups,
      Map<String, EngineValue> attributes,
      Map<String, Set<String>> attributesWithoutHandlersBySubstep
  ) {
    super(name, eventHandlerGroups, attributes, attributesWithoutHandlersBySubstep);
    this.geometry = geometry;
  }

  @Override
  public Optional<EngineGeometry> getGeometry() {
    return Optional.of(geometry);
  }
}
