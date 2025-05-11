/**
 * Strategy for table-like serialization.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.HaversineUtil;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.lang.io.strategy.MapExportSerializeStrategy;

/**
 * Decorator to add geo position to the outputs of an inner strategy.
 *
 * <p>Strategy which decorates an inner strategy, adding geo position to the outputs of the inner
 * strategy by reading position.x and position.y and using HaversineUtil to provide
 * position.longitude and position.latitude in degrees. These two additional columns will be added
 * to maps returned from the inner strategy in place. This assumes that entities are provided where
 * their position is provided in grid space such that points need transformation to earth space.</p>
 */
public class MapWithLatLngSerializeStrategy implements MapExportSerializeStrategy {

  private final MapExportSerializeStrategy inner;
  private final PatchBuilderExtents extents;
  private final BigDecimal width;
  private final BigDecimal gridWidthMeters;
  private final BigDecimal gridHeightMeters;

  /**
   * Create a new decorator.
   *
   * @param extents The extents of the simulation where these extents are provided in degrees. All
   *     entities' positions are given in grid space corresponding to these extents.
   * @param width The width and height of each patch in grid space where 1, 1 in grid-space is to
   *     the left to the bottom by 1 width. This is provided in meters.
   * @param inner The inner strategy to decorate.
   */
  public MapWithLatLngSerializeStrategy(PatchBuilderExtents extents, BigDecimal width,
        MapExportSerializeStrategy inner) {
    this.inner = inner;
    this.extents = extents;
    this.width = width;

    HaversineUtil.HaversinePoint topLeft = new HaversineUtil.HaversinePoint(
        extents.getTopLeftX(),
        extents.getTopLeftY()
    );
    HaversineUtil.HaversinePoint topRight = new HaversineUtil.HaversinePoint(
        extents.getBottomRightX(),
        extents.getTopLeftY()
    );
    HaversineUtil.HaversinePoint bottomLeft = new HaversineUtil.HaversinePoint(
        extents.getTopLeftX(),
        extents.getBottomRightY()
    );
    
    gridWidthMeters = HaversineUtil.getDistance(topLeft, topRight);
    gridHeightMeters = HaversineUtil.getDistance(topLeft, bottomLeft);
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
    Map<String, String> result = inner.getRecord(entity);

    if (entity.getGeometry().isPresent()) {
      EngineGeometry geometry = entity.getGeometry().get();
      
      BigDecimal distanceFromLeftMeters = geometry.getCenterX().multiply(width);
      BigDecimal distanceFromTopMeters = geometry.getCenterY().multiply(width);
      
      HaversineUtil.HaversinePoint topLeft = new HaversineUtil.HaversinePoint(
          extents.getTopLeftX(),
          extents.getTopLeftY()
      );
      HaversineUtil.HaversinePoint eastPoint = HaversineUtil.getAtDistanceFrom(
          topLeft,
          distanceFromLeftMeters,
          "E"
      );
      HaversineUtil.HaversinePoint finalPoint = HaversineUtil.getAtDistanceFrom(
          eastPoint,
          distanceFromTopMeters,
          "S"
      );
      
      BigDecimal longitude = finalPoint.getLongitude();
      BigDecimal latitude = finalPoint.getLatitude();

      result.put("position.longitude", longitude.toString());
      result.put("position.latitude", latitude.toString());
    }

    return result;
  }

}
