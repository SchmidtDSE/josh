/**
 * Utilities to perform Haversine functions.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import java.math.RoundingMode;


/**
 * Utility to perform calculations using the Haversine formula.
 */
public class HaversineUtil {

  private static final double EARTH_RADIUS_METERS_DOUBLE = 6371000;
  private static final BigDecimal EARTH_RADIUS_METERS = BigDecimal.valueOf(
      EARTH_RADIUS_METERS_DOUBLE
  );

  /**
   * Get the distance between two points in meters.
   *
   * @param start The first point for which distance will be measured.
   * @param end The second point for which distance will be measured.
   * @return Distance in meters.
   */
  public static BigDecimal getDistance(HaversinePoint start, HaversinePoint end) {

    BigDecimal longitudeStart = start.getLongitude();
    BigDecimal latitudeStart = start.getLatitude();
    BigDecimal longitudeEnd = end.getLongitude();
    BigDecimal latitudeEnd = end.getLatitude();

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

  /**
   * Get the location of a point in longitude / latitude by traveling in a direction.
   *
   * @param start The starting point before beginning travel.
   * @param meters The number of meters to travel.
   * @param direction The direction which must be N, S, E, W to go due north, due south, due east,
   *     or due west respectively.
   * @return The point after traveling.
   */
  public static HaversinePoint getAtDistanceFrom(HaversinePoint start, BigDecimal meters,
      String direction) {
    double lat = Math.toRadians(start.getLatitude().doubleValue());
    double lng = Math.toRadians(start.getLongitude().doubleValue());
    double distance = meters.doubleValue();

    double newLat = lat;
    double newLng = lng;

    switch(direction) {
      case "N":
        // Moving north increases latitude
        newLat = lat + (distance / EARTH_RADIUS_METERS_DOUBLE);
        break;
      case "S":
        // Moving south decreases latitude
        newLat = lat - (distance / EARTH_RADIUS_METERS_DOUBLE);
        break;
      case "E":
        // Moving east increases longitude, adjusted for latitude
        newLng = lng + (distance / (EARTH_RADIUS_METERS_DOUBLE * Math.cos(lat)));
        break;
      case "W":
        // Moving west decreases longitude, adjusted for latitude
        newLng = lng - (distance / (EARTH_RADIUS_METERS_DOUBLE * Math.cos(lat)));
        break;
      default:
        throw new IllegalArgumentException("Direction must be N, S, E, or W");
    }

    // Convert back to degrees
    double newLatDegrees = Math.toDegrees(newLat);
    double newLngDegrees = Math.toDegrees(newLng);

    return new HaversinePoint(BigDecimal.valueOf(newLngDegrees), BigDecimal.valueOf(newLatDegrees));
  }

  /**
   * Geographical point with longitude and latitude values.
   */
  public static class HaversinePoint {
    private final BigDecimal longitude;
    private final BigDecimal latitude;

    /**
     * Construct a geographical point with specified longitude and latitude values.
     *
     * @param longitude the longitude of the point, represented as a BigDecimal
     * @param latitude the latitude of the point, represented as a BigDecimal
     */
    public HaversinePoint(BigDecimal longitude, BigDecimal latitude) {
      this.longitude = longitude;
      this.latitude = latitude;
    }

    /**
     * Get the longitude value of this geographical point.
     *
     * @return the longitude of this point as a BigDecimal
     */
    public BigDecimal getLongitude() {
      return longitude;
    }

    /**
     * Get the latitude value of this geographical point.
     *
     * @return the latitude of this point as a BigDecimal
     */
    public BigDecimal getLatitude() {
      return latitude;
    }
  }
}
