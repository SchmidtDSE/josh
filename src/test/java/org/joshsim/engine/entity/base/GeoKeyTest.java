/**
 * Tests for GeoKey.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Tests for the integral coordinate accessors used by grid data lookups.
 */
class GeoKeyTest {

  private EngineGeometryFactory geometryFactory;

  @BeforeEach
  void setUp() {
    geometryFactory = new GridGeometryFactory();
  }

  @Test
  void testIntegralCoordinatesMatchCenters() {
    EngineGeometry geometry = geometryFactory.createPoint(
        BigDecimal.valueOf(3),
        BigDecimal.valueOf(7)
    );
    GeoKey key = new GeoKey(Optional.of(geometry), "patch");

    assertEquals(key.getCenterX().longValue(), key.getHorizontalAsLong());
    assertEquals(key.getCenterY().longValue(), key.getVerticalAsLong());
    assertEquals(3L, key.getHorizontalAsLong());
    assertEquals(7L, key.getVerticalAsLong());
  }

  @Test
  void testIntegralCoordinatesStableAcrossReads() {
    EngineGeometry geometry = geometryFactory.createPoint(
        BigDecimal.valueOf(11),
        BigDecimal.valueOf(13)
    );
    GeoKey key = new GeoKey(Optional.of(geometry), "patch");

    for (int i = 0; i < 5; i++) {
      assertEquals(11L, key.getHorizontalAsLong());
      assertEquals(13L, key.getVerticalAsLong());
    }
  }

  @Test
  void testIntegralCoordinatesTruncateTowardZero() {
    EngineGeometry geometry = geometryFactory.createPoint(
        BigDecimal.valueOf(4.9),
        BigDecimal.valueOf(-4.9)
    );
    GeoKey key = new GeoKey(Optional.of(geometry), "patch");

    assertEquals(key.getCenterX().longValue(), key.getHorizontalAsLong());
    assertEquals(key.getCenterY().longValue(), key.getVerticalAsLong());
  }

  @Test
  void testIntegralCoordinatesRequireGeometry() {
    GeoKey key = new GeoKey(Optional.empty(), "simulation");

    assertThrows(NoSuchElementException.class, () -> key.getHorizontalAsLong());
    assertThrows(NoSuchElementException.class, () -> key.getVerticalAsLong());
  }

}
