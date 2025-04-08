/**
 * Test cases for the GeometryFactory class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;


/**
 * Unit tests for the EngineGeometryFactory class.
 */
public class EngineGeometryFactoryTest {

  private GeometryFactory geometryFactory;
  private CoordinateReferenceSystem wgs84;
  private CoordinateReferenceSystem utm11n;

  /**
   * Set up contexts for each test.
   */
  @BeforeEach
  public void setUp() throws FactoryException {
    geometryFactory = new GeometryFactory();
    wgs84 = CRS.decode("EPSG:4326"); // WGS84
    utm11n = CRS.decode("EPSG:32611"); // UTM Zone 11N
  }

  @Nested
  class CreateSquareWithCenterTests {

    @Test
    @DisplayName("Create square with center (WGS84)")
    public void testCreateSquareWithWgs84() {
      // Given
      BigDecimal width = BigDecimal.valueOf(2.0);
      BigDecimal centerX = BigDecimal.valueOf(20.0); // longitude (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // latitude (y)

      // When
      EngineGeometry geometry = EngineGeometryFactory.createSquare(width, centerX, centerY, wgs84);

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Polygon polygon = (Polygon) geometry.getInnerGeometry();
      
      assertEquals(19.0, polygon.getEnvelopeInternal().getMinX(), 0.000001);
      assertEquals(21.0, polygon.getEnvelopeInternal().getMaxX(), 0.000001);
      assertEquals(9.0, polygon.getEnvelopeInternal().getMinY(), 0.000001);
      assertEquals(11.0, polygon.getEnvelopeInternal().getMaxY(), 0.000001);
      assertEquals(wgs84, geometry.getCrs(), "CRS should be WGS84");
    }

    @Test
    @DisplayName("Create square with UTM projection")
    public void testCreateSquareWithUtm() {
      // Given
      BigDecimal width = BigDecimal.valueOf(2.0);
      BigDecimal centerX = BigDecimal.valueOf(20.0); // easting (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // northing (y)

      // When
      EngineGeometry geometry = EngineGeometryFactory.createSquare(
          width, centerX, centerY, utm11n
      );

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Polygon polygon = (Polygon) geometry.getInnerGeometry();
      
      assertEquals(19.0, polygon.getEnvelopeInternal().getMinX(), 0.000001);
      assertEquals(21.0, polygon.getEnvelopeInternal().getMaxX(), 0.000001);
      assertEquals(9.0, polygon.getEnvelopeInternal().getMinY(), 0.000001);
      assertEquals(11.0, polygon.getEnvelopeInternal().getMaxY(), 0.000001);
      assertEquals(utm11n, geometry.getCrs(), "CRS should be UTM11N");
    }

    @Test
    public void testCreateSquareWithZeroWidth() {
      // Given
      BigDecimal width = BigDecimal.ZERO;
      BigDecimal centerX = BigDecimal.valueOf(20.0); // longitude (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // latitude (y)

      // When
      EngineGeometry geometry = EngineGeometryFactory.createSquare(width, centerX, centerY, wgs84);

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Point point = (Point) geometry.getInnerGeometry();

      assertEquals(20.0, point.getX(), 0.000001, "X should equal centerX for zero width");
      assertEquals(10.0, point.getY(), 0.000001, "Y should equal centerY for zero width");
      assertEquals(wgs84, geometry.getCrs(), "CRS should be WGS84");
    }

    @Test
    public void testCreateSquareWithZeroWidthUtm() {
      // Given
      BigDecimal width = BigDecimal.ZERO;
      BigDecimal centerX = BigDecimal.valueOf(20.0); // easting (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // northing (y)

      // When
      EngineGeometry geometry = EngineGeometryFactory.createSquare(width, centerX, centerY, utm11n);

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Point point = (Point) geometry.getInnerGeometry();

      assertEquals(20.0, point.getX(), 0.000001, "X should equal centerX for zero width");
      assertEquals(10.0, point.getY(), 0.000001, "Y should equal centerY for zero width");
      assertEquals(utm11n, geometry.getCrs(), "CRS should be UTM11N");
    }

    @Test
    public void testCreateSquareWithPrecision() {
      // Given
      BigDecimal width = new BigDecimal("1.123456789");
      BigDecimal centerX = new BigDecimal("20.123456789"); // longitude (x)
      BigDecimal centerY = new BigDecimal("10.987654321"); // latitude (y)

      // When
      EngineGeometry geometry = EngineGeometryFactory.createSquare(width, centerX, centerY, wgs84);

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Polygon polygon = (Polygon) geometry.getInnerGeometry();

      double halfWidth = width.doubleValue() / 2.0;
      assertEquals(20.123456789 - halfWidth, polygon.getEnvelopeInternal().getMinX(), 0.000001);
      assertEquals(20.123456789 + halfWidth, polygon.getEnvelopeInternal().getMaxX(), 0.000001);
      assertEquals(10.987654321 - halfWidth, polygon.getEnvelopeInternal().getMinY(), 0.000001);
      assertEquals(10.987654321 + halfWidth, polygon.getEnvelopeInternal().getMaxY(), 0.000001);
      assertEquals(wgs84, geometry.getCrs(), "CRS should be WGS84");
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
        EngineGeometryFactory.createSquare(width, centerX, centerY, nullCrs);
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
        EngineGeometryFactory.createSquare(width, centerX, centerY, wgs84);
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
        EngineGeometryFactory.createSquare(width, centerX, centerY, wgs84);
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
        EngineGeometryFactory.createSquare(width, centerX, centerY, wgs84);
      }, "Should throw NullPointerException when centerY is null");
    }
  }

  @Nested
  class CreateSquareWithCornersTests {

    @Test
    @DisplayName("Should create square from valid corner coordinates")
    public void testCreateSquareWithValidCorners() {
      // Given
      BigDecimal topLeftX = BigDecimal.valueOf(19.0); // longitude (x)
      BigDecimal topLeftY = BigDecimal.valueOf(11.0); // latitude (y)
      BigDecimal bottomRightX = BigDecimal.valueOf(21.0); // longitude (x)
      BigDecimal bottomRightY = BigDecimal.valueOf(9.0); // latitude (y)

      // When
      EngineGeometry geometry = EngineGeometryFactory.createSquare(
          topLeftX, topLeftY, bottomRightX, bottomRightY, wgs84
      );

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Polygon polygon = (Polygon) geometry.getInnerGeometry();

      assertEquals(19.0, polygon.getEnvelopeInternal().getMinX(), 0.000001);
      assertEquals(21.0, polygon.getEnvelopeInternal().getMaxX(), 0.000001);
      assertEquals(9.0, polygon.getEnvelopeInternal().getMinY(), 0.000001);
      assertEquals(11.0, polygon.getEnvelopeInternal().getMaxY(), 0.000001);
      assertEquals(wgs84, geometry.getCrs(), "CRS should be WGS84");
    }

    @Test
    @DisplayName("Should create square from valid corner coordinates with UTM")
    public void testCreateSquareWithValidCornersUtm() {
      // Given
      BigDecimal topLeftX = BigDecimal.valueOf(19.0); // easting (x)
      BigDecimal topLeftY = BigDecimal.valueOf(11.0); // northing (y)
      BigDecimal bottomRightX = BigDecimal.valueOf(21.0); // easting (x)
      BigDecimal bottomRightY = BigDecimal.valueOf(9.0); // northing (y)

      // When
      EngineGeometry geometry = EngineGeometryFactory.createSquare(
          topLeftX, topLeftY, bottomRightX, bottomRightY, utm11n
      );

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Polygon polygon = (Polygon) geometry.getInnerGeometry();

      assertEquals(19.0, polygon.getEnvelopeInternal().getMinX(), 0.000001);
      assertEquals(21.0, polygon.getEnvelopeInternal().getMaxX(), 0.000001);
      assertEquals(9.0, polygon.getEnvelopeInternal().getMinY(), 0.000001);
      assertEquals(11.0, polygon.getEnvelopeInternal().getMaxY(), 0.000001);
      assertEquals(utm11n, geometry.getCrs(), "CRS should be UTM11N");
    }

    @Test
    public void testCreateSquareWithNonSquareCoordinates() {
      // Given
      BigDecimal topLeftX = BigDecimal.valueOf(19.0); // longitude (x) - Width = 2
      BigDecimal topLeftY = BigDecimal.valueOf(12.0); // latitude (y) - Height = 3
      BigDecimal bottomRightX = BigDecimal.valueOf(21.0); // longitude (x)
      BigDecimal bottomRightY = BigDecimal.valueOf(9.0); // latitude (y)

      // When
      EngineGeometry geometry = EngineGeometryFactory.createSquare(
          topLeftX, topLeftY, bottomRightX, bottomRightY, wgs84
      );

      // Then
      assertNotNull(geometry, "Geometry should not be null even if not square");
      Polygon polygon = (Polygon) geometry.getInnerGeometry();
      
      // Should create a square with equal sides (using the smaller dimension)
      double width = polygon.getEnvelopeInternal().getWidth();
      double height = polygon.getEnvelopeInternal().getHeight();
      assertEquals(width, height, 0.000001, "Width and height should be equal");
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
        EngineGeometryFactory.createSquare(
            topLeftX, topLeftY, bottomRightX, bottomRightY, nullCrs
        );
      }, "Should throw NullPointerException when CRS is null");
    }
  }

  @Nested
  class CreateCircleTests {

    @Test
    @DisplayName("Create circle with radius and center")
    public void testCreateCircleWithRadiusAndCenter() {
      // Given
      BigDecimal radius = BigDecimal.valueOf(1.0);
      BigDecimal centerX = BigDecimal.valueOf(20.0); // longitude (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // latitude (y)

      // When
      EngineGeometry geometry = EngineGeometryFactory.createCircle(radius, centerX, centerY, wgs84);

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Geometry circle = geometry.getInnerGeometry();
      // Check if the center of the circle's envelope is at the specified center
      assertEquals(20.0, circle.getCentroid().getX(), 0.000001);
      assertEquals(10.0, circle.getCentroid().getY(), 0.000001);
      assertEquals(wgs84, geometry.getCrs(), "CRS should be WGS84");
      
      // Verify the distance from center to edge is approximately the radius
      Point center = geometryFactory.createPoint(new Coordinate(20.0, 10.0));
      double distance = center.distance(circle.getBoundary());
      assertEquals(1.0, distance, 0.01);
    }

    @Test
    @DisplayName("Create circle with radius and center with UTM")
    public void testCreateCircleWithRadiusAndCenterUtm() {
      // Given
      BigDecimal radius = BigDecimal.valueOf(1.0);
      BigDecimal centerX = BigDecimal.valueOf(20.0); // easting (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // northing (y)

      // When
      EngineGeometry geometry = EngineGeometryFactory.createCircle(
          radius, centerX, centerY, utm11n
      );

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Geometry circle = geometry.getInnerGeometry();
      assertEquals(20.0, circle.getCentroid().getX(), 0.000001);
      assertEquals(10.0, circle.getCentroid().getY(), 0.000001);
      assertEquals(utm11n, geometry.getCrs(), "CRS should be UTM11N");
      
      // Verify the distance from center to edge is approximately the radius
      Point center = geometryFactory.createPoint(new Coordinate(20.0, 10.0));
      double distance = center.distance(circle.getBoundary());
      assertEquals(1.0, distance, 0.01);
    }

    @Test
    @DisplayName("Create circle from point on circumference and center")
    public void testCreateCircleFromPointAndCenter() {
      // Given
      BigDecimal pointX = BigDecimal.valueOf(21.0); // longitude (x)
      BigDecimal pointY = BigDecimal.valueOf(10.0); // latitude (y)
      BigDecimal centerX = BigDecimal.valueOf(20.0); // longitude (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // latitude (y)

      // When
      EngineGeometry geometry = EngineGeometryFactory.createCircle(
          pointX, pointY, centerX, centerY, wgs84
      );

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Geometry circle = geometry.getInnerGeometry();
      assertEquals(20.0, circle.getCentroid().getX(), 0.000001);
      assertEquals(10.0, circle.getCentroid().getY(), 0.000001);
      assertEquals(wgs84, geometry.getCrs(), "CRS should be WGS84");
      
      // Point is 1.0 units away from center (horizontal)
      Point center = geometryFactory.createPoint(new Coordinate(20.0, 10.0));
      double distance = center.distance(circle.getBoundary());
      assertEquals(1.0, distance, 0.01);
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
      EngineGeometry geometry = EngineGeometryFactory.createCircle(
          pointX, pointY, centerX, centerY, utm11n
      );

      // Then
      assertNotNull(geometry, "Geometry should not be null");
      Geometry circle = geometry.getInnerGeometry();
      assertEquals(20.0, circle.getCentroid().getX(), 0.000001);
      assertEquals(10.0, circle.getCentroid().getY(), 0.000001);
      
      // Expected radius is the Euclidean distance between points
      double expectedRadius = Math.sqrt(
          Math.pow(22.0 - 20.0, 2) + Math.pow(14.0 - 10.0, 2)
      ); // √(2² + 4²) = √20 = 4.47...
      
      Point center = geometryFactory.createPoint(new Coordinate(20.0, 10.0));
      double distance = center.distance(circle.getBoundary());
      assertEquals(expectedRadius, distance, 0.01);
      
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
        EngineGeometryFactory.createCircle(radius, centerX, centerY, nullCrs);
      }, "Should throw NullPointerException when CRS is null");
    }
  }
}