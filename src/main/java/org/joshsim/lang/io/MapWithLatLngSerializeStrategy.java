/**
 * Strategy for table-like serialization.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Decorator to add geo position to the outputs of an inner strategy.
 *
 * <p>Strategy which decorates an inner strategy, adding geo position to the outputs of the inner
 * strategy by reading position.x and position.y and using HaversineUtil to provide
 * position.longitude and position.latitude in degrees. These two additional columns will be added
 * to maps returned from the inner strategy in place.</p>
 */
public class MapWithLatLngSerializeStrategy implements
    ExportSerializeStrategy<Map<String, String>> {

  private final ExportSerializeStrategy<Map<String, String>> inner;

  /**
   * Create a new decorator.
   *
   * @param extents The extents of the simulation where these extents are provided in degrees.
   * @param inner The inner strategy to decorate.
   */
  public MapWithLatLngSerializeStrategy(PatchBuilderExtents extents,
        ExportSerializeStrategy<Map<String, String>> inner) {
    this.inner = inner;
  }

  @Override
  public Map<String, String> getRecord(Entity entity) {
  }
  
}