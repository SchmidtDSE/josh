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
import org.joshsim.engine.geometry.HaversineUtil;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Decorator to add geo position to the outputs of an inner strategy.
 *
 * <p>Strategy which decorates an inner strategy, adding geo position to the outputs of the inner
 * strategy by reading position.x and position.y and using HaversineUtil to provide
 * position.longitude and position.latitude in degrees. These two additional columns will be added
 * to maps returned from the inner strategy in place. This assumes that entities are provided where
 * their position is provided in grid space such that points need transformation to earth space.</p>
 */
public class MapWithLatLngSerializeStrategy implements
    ExportSerializeStrategy<Map<String, String>> {

  private final ExportSerializeStrategy<Map<String, String>> inner;

  /**
   * Create a new decorator.
   *
   * @param extents The extents of the simulation where these extents are provided in degrees. All
   *     entities' positions are given in grid space corresponding to these extents.
   * @param inner The inner strategy to decorate.
   */
  public MapWithLatLngSerializeStrategy(PatchBuilderExtents extents,
        ExportSerializeStrategy<Map<String, String>> inner) {
    this.inner = inner;
  }

  /**
   * Get a map serialization of the given entity.
   *
   * <p>First get the map for the given entity and, assuming position.x and position.y are both in
   * grid space, calculate and add position.longitude and position.latitude.</p>
   *
   * @param entity The entity to be converted to a map and extended to include position.longitude
   *     and position.latitude.
   */
  @Override
  public Map<String, String> getRecord(Entity entity) {
    Map<String, String> record = inner.getRecord(entity);
    
    if (entity.getGeometry().isPresent()) {
      EngineGeometry geometry = entity.getGeometry().get();
      
      // Convert from grid coordinates to earth coordinates using extents
      BigDecimal gridWidth = extents.getBottomRightX().subtract(extents.getTopLeftX());
      BigDecimal gridHeight = extents.getBottomRightY().subtract(extents.getTopLeftY());
      
      BigDecimal longitudeRange = extents.getBottomRightX().subtract(extents.getTopLeftX());
      BigDecimal latitudeRange = extents.getBottomRightY().subtract(extents.getTopLeftY());
      
      BigDecimal longitude = extents.getTopLeftX().add(
          geometry.getCenterX().multiply(longitudeRange).divide(gridWidth, 10, BigDecimal.ROUND_HALF_UP)
      );
      BigDecimal latitude = extents.getTopLeftY().add(
          geometry.getCenterY().multiply(latitudeRange).divide(gridHeight, 10, BigDecimal.ROUND_HALF_UP)
      );
      
      HaversineUtil.HaversinePoint point = new HaversineUtil.HaversinePoint(longitude, latitude);
      record.put("position.longitude", point.getLongitude().toString());
      record.put("position.latitude", point.getLatitude().toString());
    }
    
    return record;
  }
  
}