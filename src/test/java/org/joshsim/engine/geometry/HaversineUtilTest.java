/**
 * Tests for Haversine operations.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;


/**
 * Tests for the HaversineUtil class.
 */
class HaversineUtilTest {

  @Test
  void getDistanceShouldCalculateCorrectly() {
    BigDecimal laLong = new BigDecimal("-118.24");
    BigDecimal laLat = new BigDecimal("34.05");
    BigDecimal sfLong = new BigDecimal("-122.45");
    BigDecimal sfLat = new BigDecimal("37.73");

    BigDecimal distance = HaversineUtil.getDistance(
        new HaversineUtil.HaversinePoint(laLong, laLat),
        new HaversineUtil.HaversinePoint(sfLong, sfLat)
    );

    BigDecimal expected = new BigDecimal("557787");
    BigDecimal difference = distance.subtract(expected).abs();
    BigDecimal tolerance = expected.multiply(new BigDecimal("0.05"));

    assertTrue(
        difference.compareTo(tolerance) < 0,
        "Distance should be within 5% of expected value"
    );
  }

  @Test
  void getDistanceShouldReturnZeroForSamePoint() {
    BigDecimal longitude = new BigDecimal("1.23");
    BigDecimal latitude = new BigDecimal("1.23");

    BigDecimal distance = HaversineUtil.getDistance(
        new HaversineUtil.HaversinePoint(longitude, latitude),
        new HaversineUtil.HaversinePoint(longitude, latitude)
    );

    assertEquals(
        BigDecimal.ZERO.setScale(2),
        distance.setScale(2),
        "Distance between same point should be 0"
    );
  }

  @Test
  void getAtDistanceFromShouldMoveNorthCorrectly() {
    BigDecimal sfLong = new BigDecimal("-122.45");
    BigDecimal sfLat = new BigDecimal("37.73");
    HaversineUtil.HaversinePoint start = new HaversineUtil.HaversinePoint(sfLong, sfLat);

    HaversineUtil.HaversinePoint result = HaversineUtil.getAtDistanceFrom(
        start,
        new BigDecimal("5000"),
        "N"
    );

    assertTrue(result.getLatitude().setScale(2, RoundingMode.HALF_UP).compareTo(sfLat) > 0);
    assertEquals(
        0,
        result.getLongitude().setScale(2, RoundingMode.HALF_UP).compareTo(sfLong)
    );
  }

  @Test
  void getAtDistanceFromShouldMoveSouthCorrectly() {
    BigDecimal sfLong = new BigDecimal("-122.45");
    BigDecimal sfLat = new BigDecimal("37.73");
    HaversineUtil.HaversinePoint start = new HaversineUtil.HaversinePoint(sfLong, sfLat);

    HaversineUtil.HaversinePoint result = HaversineUtil.getAtDistanceFrom(
        start, new BigDecimal("5000"), "S");

    BigDecimal expectedLat = new BigDecimal("37.69");
    assertTrue(result.getLatitude().setScale(2, RoundingMode.HALF_UP).compareTo(sfLat) < 0);
    assertEquals(
        0,
        result.getLongitude().setScale(2, RoundingMode.HALF_UP).compareTo(sfLong)
    );
  }

  @Test
  void getAtDistanceFromShouldMoveEastCorrectly() {
    BigDecimal sfLong = new BigDecimal("-122.45");
    BigDecimal sfLat = new BigDecimal("37.73");
    HaversineUtil.HaversinePoint start = new HaversineUtil.HaversinePoint(sfLong, sfLat);

    HaversineUtil.HaversinePoint result = HaversineUtil.getAtDistanceFrom(
        start,
        new BigDecimal("5000"),
        "E"
    );

    BigDecimal expectedLong = new BigDecimal("-122.41");
    assertTrue(result.getLatitude().setScale(2, RoundingMode.HALF_UP).compareTo(sfLong) > 0);
    assertEquals(
        0,
        result.getLatitude().setScale(2, RoundingMode.HALF_UP).compareTo(sfLat)
    );
  }

  @Test
  void getAtDistanceFromShouldMoveWestCorrectly() {
    BigDecimal sfLong = new BigDecimal("-122.45");
    BigDecimal sfLat = new BigDecimal("37.73");
    HaversineUtil.HaversinePoint start = new HaversineUtil.HaversinePoint(sfLong, sfLat);

    HaversineUtil.HaversinePoint result = HaversineUtil.getAtDistanceFrom(
        start,
        new BigDecimal("5000"),
        "W"
    );

    BigDecimal expectedLong = new BigDecimal("-122.49");
    assertTrue(result.getLatitude().setScale(2, RoundingMode.HALF_UP).compareTo(sfLong) < 0);
    assertEquals(
        0,
        result.getLatitude().setScale(2, RoundingMode.HALF_UP).compareTo(sfLat)
    );
  }

  @Test
  void encodesAndDecodesInAgreementNorth() {
    BigDecimal sfLong = new BigDecimal("-122.45");
    BigDecimal sfLat = new BigDecimal("37.73");
    HaversineUtil.HaversinePoint start = new HaversineUtil.HaversinePoint(sfLong, sfLat);

    HaversineUtil.HaversinePoint result = HaversineUtil.getAtDistanceFrom(
        start,
        new BigDecimal("5000"),
        "N"
    );

    double delta = HaversineUtil.getDistance(start, result)
        .subtract(new BigDecimal("5000"))
        .abs()
        .doubleValue();

    assertTrue(delta < 0.0001);
  }

  @Test
  void encodesAndDecodesInAgreementEast() {
    BigDecimal sfLong = new BigDecimal("-122.45");
    BigDecimal sfLat = new BigDecimal("37.73");
    HaversineUtil.HaversinePoint start = new HaversineUtil.HaversinePoint(sfLong, sfLat);

    HaversineUtil.HaversinePoint result = HaversineUtil.getAtDistanceFrom(
        start,
        new BigDecimal("5000"),
        "E"
    );

    double delta = HaversineUtil.getDistance(start, result)
        .subtract(new BigDecimal("5000"))
        .abs()
        .doubleValue();

    assertTrue(delta < 0.0001);
  }

  @Test
  void encodesAndDecodesInAgreementSouth() {
    BigDecimal sfLong = new BigDecimal("-122.45");
    BigDecimal sfLat = new BigDecimal("37.73");
    HaversineUtil.HaversinePoint start = new HaversineUtil.HaversinePoint(sfLong, sfLat);

    HaversineUtil.HaversinePoint result = HaversineUtil.getAtDistanceFrom(
        start,
        new BigDecimal("5000"),
        "S"
    );

    double delta = HaversineUtil.getDistance(start, result)
        .subtract(new BigDecimal("5000"))
        .abs()
        .doubleValue();

    assertTrue(delta < 0.0001);
  }

  @Test
  void encodesAndDecodesInAgreementWest() {
    BigDecimal sfLong = new BigDecimal("-122.45");
    BigDecimal sfLat = new BigDecimal("37.73");
    HaversineUtil.HaversinePoint start = new HaversineUtil.HaversinePoint(sfLong, sfLat);

    HaversineUtil.HaversinePoint result = HaversineUtil.getAtDistanceFrom(
        start,
        new BigDecimal("5000"),
        "W"
    );

    double delta = HaversineUtil.getDistance(start, result)
        .subtract(new BigDecimal("5000"))
        .abs()
        .doubleValue();

    assertTrue(delta < 0.0001);
  }
}
