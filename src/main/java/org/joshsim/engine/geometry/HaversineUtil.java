/**
 * Utilities to perform Haversine functions.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import java.math.BigDecimal;


/**
 * Utility to perform calculations using the Haversine formula.
 */
public class HaversineUtil {

  private static final BigDecimal EARTH_RADIUS_METERS = new BigDecimal("6371000");

  /**
   * Get the distance between two points in meters.
   *
   * @param longitudeStart The longitude of the first point for which distance will be measured.
   * @param latitudeStart The latitude of the first point for which distance will be measured.
   * @param longitudeEnd The longitude of the second point for which distance will be measured.
   * @param latitudeEnd The latitude of the second point for which distance will be measured.
   * @return Distance in meters.
   */
  public static BigDecimal getDistance(BigDecimal longitudeStart, BigDecimal latitudeStart,
      BigDecimal longitudeEnd, BigDecimal latitudeEnd) {

    double angleLatitudeStart = Math.toRadians(latitudeStart.doubleValue());
    double angleLatitudeEnd = Math.toRadians(latitudeEnd.doubleValue());
    double deltaLatitude = Math.toRadians(latitudeEnd.subtract(latitudeStart).doubleValue());
    double deltaLongitude = Math.toRadians(longitudeEnd.subtract(longitudeStart).doubleValue());

    double a = (
        Math.sin(deltaLatitude / 2) * Math.sin(deltaLatitude / 2)
        + Math.cos(angleLatitudeStart) * Math.cos(angleLatitudeEnd)
        * Math.sin(deltaLongitude / 2) * Math.sin(deltaLongitude / 2)
    );

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return EARTH_RADIUS_METERS.multiply(BigDecimal.valueOf(c));
  }
}
