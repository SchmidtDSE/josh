/**
 * Test cases for the EarthGeometryFactory class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.geo.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.engine.geometry.grid.GridShape;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Unit tests for the EarthGeometryFactory class.
 */
public class EarthGeometryFactoryTest {

  private EngineGeometryFactory engineGeometryFactoryWgs84;
  private EngineGeometryFactory engineGeometryFactoryUtm11n;
  private GeometryFactory geometryFactory;
  private CoordinateReferenceSystem wgs84;
  private CoordinateReferenceSystem utm11n;
  private RealizedGridCrsTest realizedGridCrs;

  /**
   * Set up contexts for each test.
   */
  @BeforeEach
  public void setUp() throws FactoryException {
    geometryFactory = new GeometryFactory();
    // Using Apache SIS for CRS definitions
    wgs84 = CommonCRS.WGS84.geographic();
    utm11n = CRS.forCode("EPSG:32611"); // UTM Zone 11N

    engineGeometryFactoryWgs84 = new EarthGeometryFactory(wgs84);
    engineGeometryFactoryUtm11n = new EarthGeometryFactory(utm11n);
  }

  @Test
  @DisplayName("Create GridCRS from definition")
  public void testCreateGridCrsFromDefinition() throws IOException, TransformException {
    // Define extents similar to the simulation example
    BigDecimal topLeftX = new BigDecimal("-116.0");
    BigDecimal topLeftY = new BigDecimal("35.0");
    BigDecimal bottomRightX = new BigDecimal("-115.0");
    BigDecimal bottomRightY = new BigDecimal("34.0");
    BigDecimal cellSize = new BigDecimal("30");

    // Create a custom CRS definition
    GridCrsDefinition definition = new GridCrsDefinition(
        "TestGrid",
        "EPSG:4326",
        new PatchBuilderExtents(topLeftX, topLeftY, bottomRightX, bottomRightY),
        cellSize,
        "m");

    // Set up the EarthGeometryFactory with grid CRS
    EarthGeometryFactory factory = new EarthGeometryFactory(wgs84);
    factory.setRealizedGridCrsFromDefition(definition);

    // Verify grid CRS is set
    assertNotNull(factory.getGridCrs(), "Grid CRS should be set");
    assertNotNull(factory.getEarthCrs(), "Earth CRS should be set");
    assertEquals(wgs84, factory.getEarthCrs(), "Earth CRS should match what was provided");
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
      EarthGeometry geometry = (EarthGeometry) engineGeometryFactoryWgs84.createSquare(
          centerX, centerY, width
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
    @DisplayName("Create square with UTM projection")
    public void testCreateSquareWithUtm() {
      // Given
      BigDecimal width = BigDecimal.valueOf(2.0);
      BigDecimal centerX = BigDecimal.valueOf(20.0); // easting (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // northing (y)

      // When
      EarthGeometry geometry = (EarthGeometry) engineGeometryFactoryUtm11n.createSquare(
          centerX, centerY, width
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
    public void testCreateSquareWithPrecision() {
      // Given
      BigDecimal width = new BigDecimal("1.123456789");
      BigDecimal centerX = new BigDecimal("20.123456789"); // longitude (x)
      BigDecimal centerY = new BigDecimal("10.987654321"); // latitude (y)

      // When
      EarthGeometry geometry = (EarthGeometry) engineGeometryFactoryWgs84.createSquare(
          centerX, centerY, width
      );

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
    public void testCreateSquareWithNullWidth() {
      // Given
      BigDecimal width = null;
      BigDecimal centerX = BigDecimal.valueOf(20.0); // longitude (x)
      BigDecimal centerY = BigDecimal.valueOf(10.0); // latitude (y)

      // When/Then
      assertThrows(NullPointerException.class, () -> {
        engineGeometryFactoryWgs84.createSquare(centerX, centerY, width);
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
        engineGeometryFactoryWgs84.createSquare(centerX, centerY, width);
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
        engineGeometryFactoryWgs84.createSquare(centerX, centerY, width);
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
      EarthGeometry geometry = (EarthGeometry) engineGeometryFactoryWgs84.createSquare(
          topLeftX,
          topLeftY,
          bottomRightX,
          bottomRightY
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
      EarthGeometry geometry = (EarthGeometry) engineGeometryFactoryUtm11n.createSquare(
          topLeftX,
          topLeftY,
          bottomRightX,
          bottomRightY
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

      // When/Then
      assertThrows(IllegalArgumentException.class, () -> {
        engineGeometryFactoryWgs84.createSquare(
            topLeftX,
            topLeftY,
            bottomRightX,
            bottomRightY
        );
      }, "Should throw IllegalArgumentException when shape is not square");
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
      EarthGeometry geometry = (EarthGeometry) engineGeometryFactoryWgs84.createCircle(
          centerX, centerY, radius
      );

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
      EarthGeometry geometry = (EarthGeometry) engineGeometryFactoryUtm11n.createCircle(
          centerX, centerY, radius
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
      EarthGeometry geometry = (EarthGeometry) engineGeometryFactoryWgs84.createCircle(
          pointX,
          pointY,
          centerX,
          centerY
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
      EarthGeometry geometry = (EarthGeometry) engineGeometryFactoryUtm11n.createCircle(
          pointX,
          pointY,
          centerX,
          centerY
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
  }

  @Nested
  class GridCrsConversionTests {

    private GridCrsDefinition createGridCrsDefinition() {
      // Define extents similar to the simulation example
      BigDecimal topLeftX = new BigDecimal("-116.0");
      BigDecimal topLeftY = new BigDecimal("35.0");
      BigDecimal bottomRightX = new BigDecimal("-115.0");
      BigDecimal bottomRightY = new BigDecimal("34.0");
      BigDecimal cellSize = new BigDecimal("30");

      return new GridCrsDefinition(
          "TestGrid",
          "EPSG:4326",
          new PatchBuilderExtents(topLeftX, topLeftY, bottomRightX, bottomRightY),
          cellSize,
          "m");
    }

    @Test
    @DisplayName("Convert point from Grid to Earth CRS")
    public void testGridToEarthCrsPointConversion() throws IOException, TransformException {
      // Set up the factory with grid CRS
      EarthGeometryFactory factory = new EarthGeometryFactory(wgs84);
      factory.setRealizedGridCrsFromDefition(createGridCrsDefinition());

      // Create a point in grid coordinates (cell indices)
      BigDecimal gridX = BigDecimal.valueOf(10);  // 10th cell in X direction
      BigDecimal gridY = BigDecimal.valueOf(5);   // 5th cell in Y direction

      // Create point geometry in grid space
      EngineGeometry gridGeometry = new GridGeometryFactory().createPoint(gridX, gridY);

      // Convert to Earth CRS
      EngineGeometry earthGeometry = factory.createPointFromGrid((GridShape) gridGeometry);

      // Verify result
      assertNotNull(earthGeometry, "Earth geometry should not be null");
      assertTrue(
          earthGeometry.getOnEarth() instanceof EarthShape,
          "Result should be an EarthShape"
      );
      assertEquals(wgs84, earthGeometry.getOnEarth().getCrs(), "Result should use Earth CRS");

      // The actual coordinate values will depend on the grid transformation,
      // so we just check that they're within reasonable bounds
      Point earthPoint = (Point) earthGeometry.getOnEarth().getInnerGeometry();
      assertTrue(earthPoint.getX() >= -116.0 && earthPoint.getX() <= -115.0,
          "X coordinate should be within bounds");
      assertTrue(earthPoint.getY() >= 34.0 && earthPoint.getY() <= 35.0,
          "Y coordinate should be within bounds");
    }

    @Test
    @DisplayName("Convert square from Grid to Earth CRS")
    public void testGridToEarthCrsSquareConversion() throws IOException, TransformException {
      // Set up the factory with grid CRS
      EarthGeometryFactory factory = new EarthGeometryFactory(wgs84);
      factory.setRealizedGridCrsFromDefition(createGridCrsDefinition());

      // Create a square in grid coordinates (cell indices)
      BigDecimal centerX = BigDecimal.valueOf(10);
      BigDecimal centerY = BigDecimal.valueOf(5);
      BigDecimal width = BigDecimal.valueOf(2);  // 2 cells wide

      // Create square geometry in grid space
      EngineGeometry gridGeometry = new GridGeometryFactory().createSquare(centerX, centerY, width);

      // Convert to Earth CRS
      EngineGeometry earthGeometry = factory.createFromGrid((GridShape) gridGeometry);

      // Verify result
      assertNotNull(earthGeometry, "Earth geometry should not be null");
      assertTrue(
          earthGeometry.getOnEarth() instanceof EarthShape,
          "Result should be an EarthShape"
      );
      assertEquals(wgs84, earthGeometry.getOnEarth().getCrs(), "Result should use Earth CRS");

      // The actual coordinate values will depend on the grid transformation,
      // but we can check that the result is a polygon
      Geometry earthGeom = earthGeometry.getOnEarth().getInnerGeometry();
      assertTrue(earthGeom instanceof Polygon, "Result should be a polygon");
    }
  }
}