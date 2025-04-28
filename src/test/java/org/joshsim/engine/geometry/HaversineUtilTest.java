
package org.joshsim.engine.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the HaversineUtil class.
 */
class HaversineUtilTest {
  private HaversineUtil haversineUtil;

  @BeforeEach
  void setUp() {
    haversineUtil = new HaversineUtil();
  }

  @Test
  @DisplayName("Should calculate distance between two points correctly")
  void getDistanceShouldCalculateCorrectly() {
    // Los Angeles to New York approximate coordinates
    BigDecimal laLong = new BigDecimal("-118.2437");
    BigDecimal laLat = new BigDecimal("34.0522");
    BigDecimal nyLong = new BigDecimal("-74.0060");
    BigDecimal nyLat = new BigDecimal("40.7128");

    BigDecimal distance = haversineUtil.getDistance(laLong, laLat, nyLong, nyLat);
    
    // Expected distance is approximately 3935km = 3,935,000m
    // Allow 1% margin of error
    BigDecimal expected = new BigDecimal("3935000");
    BigDecimal difference = distance.subtract(expected).abs();
    BigDecimal tolerance = expected.multiply(new BigDecimal("0.01")); // 1% tolerance
    
    assertTrue(difference.compareTo(tolerance) < 0, 
        "Distance should be within 1% of expected value");
  }

  @Test
  @DisplayName("Should return zero for same point")
  void getDistanceShouldReturnZeroForSamePoint() {
    BigDecimal longitude = new BigDecimal("-118.2437");
    BigDecimal latitude = new BigDecimal("34.0522");

    BigDecimal distance = haversineUtil.getDistance(longitude, latitude, longitude, latitude);
    
    assertEquals(BigDecimal.ZERO.setScale(2), distance.setScale(2),
        "Distance between same point should be 0");
  }
}
