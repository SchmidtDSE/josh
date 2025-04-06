/**
 * Test cases for the GeometryFactory class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.apache.sis.referencing.CRS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.SpatialContextFactory;
import org.locationtech.spatial4j.shape.Circle;
import org.locationtech.spatial4j.shape.Rectangle;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 * Unit tests for the GeometryFactory class.
 */
public class GeometryFactoryTest {

  private SpatialContext geoContext;
  private SpatialContext projectedContext;
  private CoordinateReferenceSystem wgs84;
  private CoordinateReferenceSystem utm11n;

  /**
   * Set up contexts for each test.
   */
  @BeforeEach
  public void setUp() throws FactoryException {
    // Standard WGS84 geographic context
    geoContext = SpatialContext.GEO;
    wgs84 = CRS.forCode("EPSG:4326"); // WGS84

    // Create a projected (Cartesian) context
    SpatialContextFactory factory = new SpatialContextFactory();
    factory.geo = false; // Makes it a Cartesian/projected system
    projectedContext = factory.newSpatialContext();
    utm11n = CRS.forCode("EPSG:32611"); // UTM Zone 11N
  }

  @Nested
  class CreateSquareWithCenterTests {

    @Test
    @DisplayName("Create square with default context (WGS84)")
    public void testCreateSquareWithDefaultContext() {
      // Given
      BigDecimal width = BigDecimal.valueOf(2.0);
      BigDecimal centerX = BigDecimal.valueOf(20.0); // longitude (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // latitude (y)

      // When
      Geometry geometry = GeometryFactory.createSquare(width, centerX, centerY,
          GeometryFactory.getDefaultSpatialContext(), wgs84);

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle) geometry.shape;
      assertEquals(19.0, rect.getMinX(), 0.000001);
      assertEquals(21.0, rect.getMaxX(), 0.000001);
      assertEquals(9.0, rect.getMinY(), 0.000001);
      assertEquals(11.0, rect.getMaxY(), 0.000001);
      assertTrue(
          GeometryFactory.getDefaultSpatialContext().isGeo(),
          "Default context should be geographic"
      );
      assertEquals(wgs84, geometry.getCrs(), "CRS should be WGS84");
    }

    @Test
    @DisplayName("Create square with explicit geographic context")
    public void testCreateSquareWithGeoContext() {
      // Given
      BigDecimal width = BigDecimal.valueOf(2.0);
      BigDecimal centerX = BigDecimal.valueOf(20.0); // longitude (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // latitude (y)

      // When
      Geometry geometry = GeometryFactory.createSquare(width, centerX, centerY, geoContext, wgs84);

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle) geometry.shape;
      assertEquals(19.0, rect.getMinX(), 0.000001);
      assertEquals(21.0, rect.getMaxX(), 0.000001);
      assertEquals(9.0, rect.getMinY(), 0.000001);
      assertEquals(11.0, rect.getMaxY(), 0.000001);
      assertTrue(geoContext.isGeo(), "Should be a geographic context");
      assertEquals(wgs84, geometry.getCrs(), "CRS should be WGS84");
    }

    @Test
    @DisplayName("Create square with projected (Cartesian) context")
    public void testCreateSquareWithProjectedContext() {
      // Given
      BigDecimal width = BigDecimal.valueOf(2.0);
      BigDecimal centerX = BigDecimal.valueOf(20.0); // easting (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // northing (y)

      // When
      Geometry geometry = GeometryFactory.createSquare(
          width, centerX, centerY, projectedContext, utm11n
      );

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle) geometry.shape;
      assertEquals(19.0, rect.getMinX(), 0.000001);
      assertEquals(21.0, rect.getMaxX(), 0.000001);
      assertEquals(9.0, rect.getMinY(), 0.000001);
      assertEquals(11.0, rect.getMaxY(), 0.000001);
      assertFalse(
          projectedContext.isGeo(),
          "Should be a projected (non-geographic) context"
      );
      assertEquals(utm11n, geometry.getCrs(), "CRS should be UTM11N");
    }

    @Test
    public void testCreateSquareWithZeroWidthGeoContext() {
      // Given
      BigDecimal width = BigDecimal.ZERO;
      BigDecimal centerX = BigDecimal.valueOf(20.0); // longitude (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // latitude (y)

      // When
      Geometry geometry = GeometryFactory.createSquare(
          width, centerX, centerY, geoContext, wgs84
      );

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle) geometry.shape;

      assertEquals(20.0, rect.getMinX(), 0.000001, "minX should equal centerX for zero width");
      assertEquals(20.0, rect.getMaxX(), 0.000001, "maxX should equal centerX for zero width");
      assertEquals(10.0, rect.getMinY(), 0.000001, "minY should equal centerY for zero width");
      assertEquals(10.0, rect.getMaxY(), 0.000001, "maxY should equal centerY for zero width");
      assertEquals(wgs84, geometry.getCrs(), "CRS should be WGS84");
    }

    @Test
    public void testCreateSquareWithZeroWidthProjectedContext() {
      // Given
      BigDecimal width = BigDecimal.ZERO;
      BigDecimal centerX = BigDecimal.valueOf(20.0); // easting (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // northing (y)

      // When
      Geometry geometry = GeometryFactory.createSquare(
          width, centerX, centerY, projectedContext, utm11n
      );

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle) geometry.shape;

      assertEquals(20.0, rect.getMinX(), 0.000001, "minX should equal centerX for zero width");
      assertEquals(20.0, rect.getMaxX(), 0.000001, "maxX should equal centerX for zero width");
      assertEquals(10.0, rect.getMinY(), 0.000001, "minY should equal centerY for zero width");
      assertEquals(10.0, rect.getMaxY(), 0.000001, "maxY should equal centerY for zero width");
      assertEquals(utm11n, geometry.getCrs(), "CRS should be UTM11N");
    }

    @Test
    public void testCreateSquareWithPrecisionGeoContext() {
      // Given
      BigDecimal width = new BigDecimal("1.123456789");
      BigDecimal centerX = new BigDecimal("20.123456789"); // longitude (x)
      BigDecimal centerY = new BigDecimal("10.987654321"); // latitude (y)

      // When
      Geometry geometry = GeometryFactory.createSquare(width, centerX, centerY, geoContext, wgs84);

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle) geometry.shape;

      double halfWidth = width.doubleValue() / 2.0;
      assertEquals(20.123456789 - halfWidth, rect.getMinX(), 0.000001);
      assertEquals(20.123456789 + halfWidth, rect.getMaxX(), 0.000001);
      assertEquals(10.987654321 - halfWidth, rect.getMinY(), 0.000001);
      assertEquals(10.987654321 + halfWidth, rect.getMaxY(), 0.000001);
      assertEquals(wgs84, geometry.getCrs(), "CRS should be WGS84");
    }

    @Test
    public void testCreateSquareWithPrecisionProjectedContext() {
      // Given
      BigDecimal width = new BigDecimal("1.123456789");
      BigDecimal centerX = new BigDecimal("20.123456789"); // easting (x)
      BigDecimal centerY = new BigDecimal("10.987654321"); // northing (y)

      // When
      Geometry geometry = GeometryFactory.createSquare(
          width, centerX, centerY, projectedContext, utm11n
      );

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle) geometry.shape;

      double halfWidth = width.doubleValue() / 2.0;
      assertEquals(20.123456789 - halfWidth, rect.getMinX(), 0.000001);
      assertEquals(20.123456789 + halfWidth, rect.getMaxX(), 0.000001);
      assertEquals(10.987654321 - halfWidth, rect.getMinY(), 0.000001);
      assertEquals(10.987654321 + halfWidth, rect.getMaxY(), 0.000001);
      assertEquals(utm11n, geometry.getCrs(), "CRS should be UTM11N");
    }

    @Test
    public void testNullContextThrowsException() {
      // Given
      BigDecimal width = BigDecimal.valueOf(2.0);
      BigDecimal centerX = BigDecimal.valueOf(20.0); // longitude (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // latitude (y)
      SpatialContext nullContext = null;

      // When/Then
      assertThrows(NullPointerException.class, () -> {
        GeometryFactory.createSquare(width, centerX, centerY, nullContext, wgs84);
      }, "Should throw NullPointerException when context is null");
    }

    @Test
    public void testNullCrsThrowsException() {
      // Given
      BigDecimal width = BigDecimal.valueOf(2.0);
      BigDecimal centerX = BigDecimal.valueOf(20.0); // longitude (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // latitude (y)
      CoordinateReferenceSystem nullCrs = null;

      // When/Then
      assertThrows(NullPointerException.class, () -> {
        GeometryFactory.createSquare(width, centerX, centerY, geoContext, nullCrs);
      }, "Should throw NullPointerException when CRS is null");
    }

    @Test
    public void testCreateSquareWithNullWidth() {
      // Given
      BigDecimal width = null;
      BigDecimal centerX = BigDecimal.valueOf(20.0); // longitude (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // latitude (y)

      // When/Then
      assertThrows(NullPointerException.class, () -> {
        GeometryFactory.createSquare(width, centerX, centerY, geoContext, wgs84);
      }, "Should throw NullPointerException when width is null");
    }

    @Test
    public void testCreateSquareWithNullCenterX() {
      // Given
      BigDecimal width = BigDecimal.valueOf(2.0);
      BigDecimal centerX = null;
      BigDecimal centerY = BigDecimal.valueOf(10.0); // latitude (y)

      // When/Then
      assertThrows(NullPointerException.class, () -> {
        GeometryFactory.createSquare(width, centerX, centerY, geoContext, wgs84);
      }, "Should throw NullPointerException when centerX is null");
    }

    @Test
    public void testCreateSquareWithNullCenterY() {
      // Given
      BigDecimal width = BigDecimal.valueOf(2.0);
      BigDecimal centerX = BigDecimal.valueOf(20.0); // longitude (x)
      BigDecimal centerY = null;

      // When/Then
      assertThrows(NullPointerException.class, () -> {
        GeometryFactory.createSquare(width, centerX, centerY, geoContext, wgs84);
      }, "Should throw NullPointerException when centerY is null");
    }
  }

  @Nested
  class CreateSquareWithCornersTests {

    @Test
    @DisplayName("Should create square from valid corner coordinates with default context")
    public void testCreateSquareWithValidCornersDefaultContext() {
      // Given
      BigDecimal topLeftX = BigDecimal.valueOf(19.0); // longitude (x)
      BigDecimal topLeftY = BigDecimal.valueOf(11.0); // latitude (y)
      BigDecimal bottomRightX = BigDecimal.valueOf(21.0); // longitude (x)
      BigDecimal bottomRightY = BigDecimal.valueOf(9.0); // latitude (y)

      // When
      Geometry geometry = GeometryFactory.createSquare(
          topLeftX, topLeftY, bottomRightX, bottomRightY,
          GeometryFactory.getDefaultSpatialContext(),
          wgs84
      );

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle) geometry.shape;

      assertEquals(19.0, rect.getMinX(), 0.000001);
      assertEquals(21.0, rect.getMaxX(), 0.000001);
      assertEquals(9.0, rect.getMinY(), 0.000001);
      assertEquals(11.0, rect.getMaxY(), 0.000001);
      assertEquals(wgs84, geometry.getCrs(), "CRS should be WGS84");
    }

    @Test
    @DisplayName("Should create square from valid corner coordinates with geographic context")
    public void testCreateSquareWithValidCornersGeoContext() {
      // Given
      BigDecimal topLeftX = BigDecimal.valueOf(19.0); // longitude (x)
      BigDecimal topLeftY = BigDecimal.valueOf(11.0); // latitude (y)
      BigDecimal bottomRightX = BigDecimal.valueOf(21.0); // longitude (x)
      BigDecimal bottomRightY = BigDecimal.valueOf(9.0); // latitude (y)

      // When
      Geometry geometry = GeometryFactory.createSquare(
          topLeftX, topLeftY, bottomRightX, bottomRightY, geoContext, wgs84
      );

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle) geometry.shape;

      assertEquals(19.0, rect.getMinX(), 0.000001);
      assertEquals(21.0, rect.getMaxX(), 0.000001);
      assertEquals(9.0, rect.getMinY(), 0.000001);
      assertEquals(11.0, rect.getMaxY(), 0.000001);
      assertEquals(wgs84, geometry.getCrs(), "CRS should be WGS84");
    }

    @Test
    @DisplayName("Should create square from valid corner coordinates with projected context")
    public void testCreateSquareWithValidCornersProjectedContext() {
      // Given
      BigDecimal topLeftX = BigDecimal.valueOf(19.0); // easting (x)
      BigDecimal topLeftY = BigDecimal.valueOf(11.0); // northing (y)
      BigDecimal bottomRightX = BigDecimal.valueOf(21.0); // easting (x)
      BigDecimal bottomRightY = BigDecimal.valueOf(9.0); // northing (y)

      // When
      Geometry geometry = GeometryFactory.createSquare(
          topLeftX, topLeftY, bottomRightX, bottomRightY, projectedContext, utm11n
      );

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Rectangle rect = (Rectangle) geometry.shape;

      assertEquals(19.0, rect.getMinX(), 0.000001);
      assertEquals(21.0, rect.getMaxX(), 0.000001);
      assertEquals(9.0, rect.getMinY(), 0.000001);
      assertEquals(11.0, rect.getMaxY(), 0.000001);
      assertFalse(projectedContext.isGeo(), "Should be a projected (non-geographic) context");
      assertEquals(utm11n, geometry.getCrs(), "CRS should be UTM11N");
    }

    @Test
    public void testCreateSquareWithNonSquareCoordinatesGeoContext() {
      // Given
      BigDecimal topLeftX = BigDecimal.valueOf(19.0); // longitude (x) - Width = 2
      BigDecimal topLeftY = BigDecimal.valueOf(12.0); // latitude (y) - Height = 3
      BigDecimal bottomRightX = BigDecimal.valueOf(21.0); // longitude (x)
      BigDecimal bottomRightY = BigDecimal.valueOf(9.0); // latitude (y)

      // When/Then
      Geometry geometry = GeometryFactory.createSquare(
            topLeftX,
            topLeftY,
            bottomRightX,
            bottomRightY,
            geoContext,
            wgs84
        );

      assertEquals(null, geometry, "Geometry should be null if it is not reasonably square");
    }

    @Test
    public void testCreateSquareWithNonSquareCoordinatesProjectedContext() {
      // Given
      BigDecimal topLeftX = BigDecimal.valueOf(19.0); // easting (x) - Width = 2
      BigDecimal topLeftY = BigDecimal.valueOf(12.0); // northing (y) - Height = 3
      BigDecimal bottomRightX = BigDecimal.valueOf(21.0); // easting (x)
      BigDecimal bottomRightY = BigDecimal.valueOf(9.0); // northing (y)

      // When/Then
      Geometry geometry = GeometryFactory.createSquare(
            topLeftX,
            topLeftY,
            bottomRightX,
            bottomRightY,
            projectedContext,
            utm11n
        );

      assertEquals(null, geometry, "Geometry should be null if it is not reasonably square");
    }

    @Test
    public void testNullContextWithCornersThrowsException() {
      // Given
      BigDecimal topLeftX = BigDecimal.valueOf(19.0); // longitude (x)
      BigDecimal topLeftY = BigDecimal.valueOf(11.0); // latitude (y)
      BigDecimal bottomRightX = BigDecimal.valueOf(21.0); // longitude (x)
      BigDecimal bottomRightY = BigDecimal.valueOf(9.0); // latitude (y)
      SpatialContext nullContext = null;

      // When/Then
      assertThrows(NullPointerException.class, () -> {
        GeometryFactory.createSquare(
            topLeftX, topLeftY, bottomRightX, bottomRightY, nullContext, wgs84
        );
      }, "Should throw NullPointerException when context is null");
    }

    @Test
    public void testNullCrsWithCornersThrowsException() {
      // Given
      BigDecimal topLeftX = BigDecimal.valueOf(19.0); // longitude (x)
      BigDecimal topLeftY = BigDecimal.valueOf(11.0); // latitude (y)
      BigDecimal bottomRightX = BigDecimal.valueOf(21.0); // longitude (x)
      BigDecimal bottomRightY = BigDecimal.valueOf(9.0); // latitude (y)
      CoordinateReferenceSystem nullCrs = null;

      // When/Then
      assertThrows(NullPointerException.class, () -> {
        GeometryFactory.createSquare(
            topLeftX, topLeftY, bottomRightX, bottomRightY, geoContext, nullCrs
        );
      }, "Should throw NullPointerException when CRS is null");
    }
  }

  @Nested
  class CreateCircleTests {

    @Test
    @DisplayName("Create circle with radius and center with default context")
    public void testCreateCircleWithRadiusAndCenterDefaultContext() {
      // Given
      BigDecimal radius = BigDecimal.valueOf(1.0);
      BigDecimal centerX = BigDecimal.valueOf(20.0); // longitude (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // latitude (y)

      // When
      Geometry geometry = GeometryFactory.createCircle(radius, centerX, centerY, wgs84);

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Circle circle = (Circle) geometry.shape;
      assertEquals(20.0, circle.getCenter().getX(), 0.000001);
      assertEquals(10.0, circle.getCenter().getY(), 0.000001);
      assertEquals(1.0, circle.getRadius(), 0.000001);
      assertEquals(wgs84, geometry.getCrs(), "CRS should be WGS84");
    }

    @Test
    @DisplayName("Create circle with radius and center with geo context")
    public void testCreateCircleWithRadiusAndCenterGeoContext() {
      // Given
      BigDecimal radius = BigDecimal.valueOf(1.0);
      BigDecimal centerX = BigDecimal.valueOf(20.0); // longitude (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // latitude (y)

      // When
      Geometry geometry = GeometryFactory.createCircle(radius, centerX, centerY, geoContext, wgs84);

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Circle circle = (Circle) geometry.shape;
      assertEquals(20.0, circle.getCenter().getX(), 0.000001);
      assertEquals(10.0, circle.getCenter().getY(), 0.000001);
      assertEquals(1.0, circle.getRadius(), 0.000001);
      assertEquals(wgs84, geometry.getCrs(), "CRS should be WGS84");
    }

    @Test
    @DisplayName("Create circle with radius and center with projected context")
    public void testCreateCircleWithRadiusAndCenterProjectedContext() {
      // Given
      BigDecimal radius = BigDecimal.valueOf(1.0);
      BigDecimal centerX = BigDecimal.valueOf(20.0); // easting (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // northing (y)

      // When
      Geometry geometry = GeometryFactory.createCircle(
          radius, centerX, centerY, projectedContext, utm11n
      );

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Circle circle = (Circle) geometry.shape;
      assertEquals(20.0, circle.getCenter().getX(), 0.000001);
      assertEquals(10.0, circle.getCenter().getY(), 0.000001);
      assertEquals(1.0, circle.getRadius(), 0.000001);
      assertEquals(utm11n, geometry.getCrs(), "CRS should be UTM11N");
    }

    @Test
    @DisplayName("Create circle from point on circumference and center with default context")
    public void testCreateCircleFromPointAndCenterDefaultContext() {
      // Given
      BigDecimal pointX = BigDecimal.valueOf(21.0); // longitude (x)
      BigDecimal pointY = BigDecimal.valueOf(10.0); // latitude (y)
      BigDecimal centerX = BigDecimal.valueOf(20.0); // longitude (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // latitude (y)

      // When
      Geometry geometry = GeometryFactory.createCircle(pointX, pointY, centerX, centerY, wgs84);

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Circle circle = (Circle) geometry.shape;
      assertEquals(20.0, circle.getCenter().getX(), 0.000001);
      assertEquals(10.0, circle.getCenter().getY(), 0.000001);
      assertEquals(1.0, circle.getRadius(), 0.000001); // Distance from (20,10) to (21,10) is 1.0
      assertEquals(wgs84, geometry.getCrs(), "CRS should be WGS84");
    }

    @Test
    @DisplayName("Create circle with non-default radius calculation")
    public void testCreateCircleWithNonDefaultRadiusCalculation() {
      // Given
      BigDecimal pointX = BigDecimal.valueOf(22.0); // longitude (x)
      BigDecimal pointY = BigDecimal.valueOf(14.0); // latitude (y)
      BigDecimal centerX = BigDecimal.valueOf(20.0); // longitude (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // latitude (y)

      // When
      Geometry geometry = GeometryFactory.createCircle(
          pointX, pointY, centerX, centerY, projectedContext, utm11n
      );

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Circle circle = (Circle) geometry.shape;
      double expectedRadius = Math.sqrt(
          Math.pow(22.0 - 20.0, 2) + Math.pow(14.0 - 10.0, 2)
      ); // √(2² + 4²) = √20 = 4.47...
      assertEquals(20.0, circle.getCenter().getX(), 0.000001);
      assertEquals(10.0, circle.getCenter().getY(), 0.000001);
      assertEquals(expectedRadius, circle.getRadius(), 0.000001);
      assertEquals(utm11n, geometry.getCrs(), "CRS should be UTM11N");
    }

    @Test
    public void testNullCrsWithCircleThrowsException() {
      // Given
      BigDecimal radius = BigDecimal.valueOf(1.0);
      BigDecimal centerX = BigDecimal.valueOf(20.0);
      BigDecimal centerY = BigDecimal.valueOf(10.0);
      CoordinateReferenceSystem nullCrs = null;

      // When/Then
      assertThrows(NullPointerException.class, () -> {
        GeometryFactory.createCircle(radius, centerX, centerY, geoContext, nullCrs);
      }, "Should throw NullPointerException when CRS is null");
    }
  }

  @Nested
  class SpatialContextTests {

    @Test
    public void testDefaultContextMethod() {
      // Verify the default context is SpatialContext.GEO
      SpatialContext defaultContext = GeometryFactory.getDefaultSpatialContext();
      assertEquals(SpatialContext.GEO, defaultContext);
      assertTrue(defaultContext.isGeo(), "Default context should be geographic");
    }

    @Test
    public void testGeoAndProjectedContextDifferences() {
      assertTrue(geoContext.isGeo(), "Geographic context should have isGeo() = true");
      assertFalse(projectedContext.isGeo(), "Projected context should have isGeo() = false");
    }
  }
}
