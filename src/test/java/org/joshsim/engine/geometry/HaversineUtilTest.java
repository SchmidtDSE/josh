
package org.joshsim.engine.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
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
}
