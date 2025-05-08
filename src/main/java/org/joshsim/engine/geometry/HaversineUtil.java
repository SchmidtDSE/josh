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

    double distanceMeters = meters.doubleValue();
    double lat1 = Math.toRadians(start.getLatitude().doubleValue());
    double lon1 = Math.toRadians(start.getLongitude().doubleValue());

    double lat2 = lat1;
    double lon2 = lon1;

    double radius = EARTH_RADIUS_METERS.doubleValue();

    switch (direction.toUpperCase()) {
      case "N":
        lat2 = Math.asin(Math.sin(lat1) * Math.cos(distanceMeters / radius)
            + Math.cos(lat1) * Math.sin(distanceMeters / radius));
        break;
      case "S":
        lat2 = Math.asin(Math.sin(lat1) * Math.cos(distanceMeters / radius)
            - Math.cos(lat1) * Math.sin(distanceMeters / radius));
        break;
      case "E":
        lon2 = lon1 + Math.atan2(Math.sin(distanceMeters / radius) * Math.cos(lat1),
               Math.cos(distanceMeters / radius));
        break;
      case "W":
        lon2 = lon1 - Math.atan2(Math.sin(distanceMeters / radius) * Math.cos(lat1),
               Math.cos(distanceMeters / radius));
        break;
      default:
        throw new IllegalArgumentException("Direction must be N, S, E, or W");
    }

    // Convert back to degrees
    BigDecimal newLat = new BigDecimal(String.valueOf(Math.toDegrees(lat2)));
    BigDecimal newLon = new BigDecimal(String.valueOf(Math.toDegrees(lon2)));

    return new HaversinePoint(newLon, newLat);
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
