package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import java.util.List;
import org.joshsim.engine.entity.base.MutableEntity;

/**
 * Utility to perform calulations using the Haversine formula.
 */
public class HaversineUtil {

  /**
   * Get the distance between two points in meters.
   *
   * @param longitudeStart The longitude of the first point for which distance will be measured.
   * @param latitudeStart The latitude of the first point for which distance will be measured.
   * @param longitudeStart The longitude of the second point for which distance will be measured.
   * @param latitudeStart The latitude of the second point for which distance will be measured.
   * @returns Distance in meters.
   */
  BigDecimal getDistance(BigDecimal longitudeStart, BigDecimal latitudeStart,
      BigDecimal longitudeEnd, BigDecimal latitudeEnd) {
    // TODO
  }
  
}
