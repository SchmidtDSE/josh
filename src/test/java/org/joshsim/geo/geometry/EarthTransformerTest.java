package org.joshsim.geo.geometry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Map;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.joshsim.engine.geometry.grid.GridShape;
import org.joshsim.engine.geometry.grid.GridShapeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Tests for EarthTransformer that verify the correct conversion between
 * grid coordinates and Earth-based coordinate reference systems.
 */
public class EarthTransformerTest {

  // Constants for the simulation example in the requirements
  private static final String INPUT_CRS_CODE = "EPSG:4326";  // WGS84
  private static final String TARGET_CRS_CODE = "EPSG:32611"; // UTM 11N
  CoordinateReferenceSystem targetCrs;
  CoordinateReferenceSystem targetCrsWgs84;

  // Coordinates from the simulation example
  private static final BigDecimal WEST_LON = new BigDecimal("-116");
  private static final BigDecimal NORTH_LAT = new BigDecimal("35");
  private static final BigDecimal EAST_LON = new BigDecimal("-115");
  private static final BigDecimal SOUTH_LAT = new BigDecimal("34");
  private static final BigDecimal CELL_SIZE = new BigDecimal("30"); // 30m

  // Known locations, in UTM 11N, above WGS84 points
  private static final BigDecimal WEST_EASTING = new BigDecimal("591253.2528339219");
  private static final BigDecimal NORTH_NORTHING = new BigDecimal("3873499.8508478715");
  private static final BigDecimal EAST_EASTING = new BigDecimal("684709.8311321359");
  private static final BigDecimal SOUTH_NORTHING = new BigDecimal("3763959.1395987775");

  // Conversion factors for meters to degrees
  private double metersToDegreesLat;
  private double metersToDegreesLon;

  private GridCrsDefinition gridCrsDefinition;
  private GridShape gridPoint;
  private GridShape gridCircle;
  private GridShape gridSquare;

  @BeforeEach
  public void setUp() {
    // Create extents matching the simulation example
    PatchBuilderExtents extents = new PatchBuilderExtents(
        WEST_LON,   // Top left X (longitude)
        NORTH_LAT,  // Top left Y (latitude)
        EAST_LON,   // Bottom right X (longitude)
        SOUTH_LAT   // Bottom right Y (latitude)
    );

    // Create grid CRS definition
    gridCrsDefinition = new GridCrsDefinition(
        "TestGrid",
        INPUT_CRS_CODE,
        extents,
        CELL_SIZE,
        "m"  // Cell size unit
    );

    // Create target CRS
    try {
      targetCrs = CRS.forCode(TARGET_CRS_CODE);
      CoordinateReferenceSystem unsafeTargetCrsWgs84 = CRS.forCode("EPSG:4326");
      targetCrsWgs84 =
          AbstractCRS.castOrCopy(unsafeTargetCrsWgs84).forConvention(AxesConvention.RIGHT_HANDED);


    } catch (FactoryException e) {
      fail("Failed to create target CRS: " + e.getMessage());
    }

    // Calculate meters to degrees conversion factors
    // 1 degree latitude ≈ 111320 meters
    metersToDegreesLat = CELL_SIZE.doubleValue() / 111320.0;

    // 1 degree longitude depends on latitude: cos(lat) * 111320 meters
    double latRad = Math.toRadians(NORTH_LAT.doubleValue());
    metersToDegreesLon = CELL_SIZE.doubleValue() / (Math.cos(latRad) * 111320.0);

    // Create mock grid shapes for testing
    gridPoint = createMockGridPoint(BigDecimal.valueOf(5), BigDecimal.valueOf(5));
    gridCircle = createMockGridCircle(
        BigDecimal.valueOf(10), BigDecimal.valueOf(10), BigDecimal.valueOf(2));
    gridSquare = createMockGridSquare(
        BigDecimal.valueOf(15), BigDecimal.valueOf(15), BigDecimal.valueOf(3));
  }

  @Nested
  @DisplayName("Tests for gridToEarth with GridCrsDefinition")
  class GridCrsDefinitionTests {

    @Test
    @DisplayName("Converting a grid point to Earth geometry")
    public void testPointConversionWgs84toUtm11n() {
      // Convert grid point to Earth geometry
      EarthGeometry earthGeometry = EarthTransformer.gridToEarth(
          gridPoint, gridCrsDefinition, TARGET_CRS_CODE);

      // Verify results
      assertNotNull(earthGeometry, "Earth geometry should not be null");
      assertTrue(earthGeometry.getInnerGeometry() instanceof Point,
          "The geometry should be a Point");
      assertEquals(targetCrs, earthGeometry.getCrs(),
          "CRS should be the target CRS");

      // Verify the coordinates are as expected - translating grid coordinates (in indices)
      // to meters (according to cell size)
      Point point = (Point) earthGeometry.getInnerGeometry();
      double expectedX = WEST_EASTING.doubleValue()
          + gridPoint.getCenterX().doubleValue() * CELL_SIZE.doubleValue();
      double expectedY = NORTH_NORTHING.doubleValue()
          - gridPoint.getCenterY().doubleValue() * CELL_SIZE.doubleValue();

      // Check the coordinates are within a small tolerance
      assertEquals(expectedX, point.getX(), 10, "X coordinate should match expected value");
      assertEquals(expectedY, point.getY(), 10, "Y coordinate should match expected value");
    }

    @Test
    @DisplayName("Converting a grid point to Earth geometry (WGS84)")
    public void testPointConversionWgs84toWgs84() {
      // Convert grid point to Earth geometry
      EarthGeometry earthGeometry = EarthTransformer.gridToEarth(
          gridPoint, gridCrsDefinition, "EPSG:4326");

      // Verify results
      assertNotNull(earthGeometry, "Earth geometry should not be null");
      assertTrue(earthGeometry.getInnerGeometry() instanceof Point,
          "The geometry should be a Point");
      assertEquals(targetCrsWgs84, earthGeometry.getCrs(),
          "CRS should be the target CRS");

      // Calculate expected coordinates using degree-based cell size
      Point point = (Point) earthGeometry.getInnerGeometry();
      double expectedX = WEST_LON.doubleValue()
          + gridPoint.getCenterX().doubleValue() * metersToDegreesLon;
      double expectedY = NORTH_LAT.doubleValue()
          - gridPoint.getCenterY().doubleValue() * metersToDegreesLat;

      // Check the coordinates are within a small tolerance
      assertEquals(expectedX, point.getX(), 0.001, "X coordinate should match expected value");
      assertEquals(expectedY, point.getY(), 0.001, "Y coordinate should match expected value");
    }

    @Test
    @DisplayName("Converting a grid circle to Earth geometry")
    public void testCircleConversion() {
      // Convert grid circle to Earth geometry
      EarthGeometry earthGeometry = EarthTransformer.gridToEarth(
          gridCircle, gridCrsDefinition, TARGET_CRS_CODE);

      // Verify results
      assertNotNull(earthGeometry, "Earth geometry should not be null");
      assertTrue(earthGeometry.getInnerGeometry() instanceof Polygon,
          "The geometry should be a Polygon (circle approximation)");
      assertEquals(targetCrs, earthGeometry.getCrs(),
          "CRS should be the target CRS");

      // Verify the center coordinates are as expected
      Polygon polygon = (Polygon) earthGeometry.getInnerGeometry();
      Point centroid = polygon.getCentroid();

      double expectedX = WEST_EASTING.doubleValue()
          + gridCircle.getCenterX().doubleValue() * CELL_SIZE.doubleValue();
      double expectedY = NORTH_NORTHING.doubleValue()
          - gridCircle.getCenterY().doubleValue() * CELL_SIZE.doubleValue();

      assertEquals(
          expectedX,
          centroid.getX(),
          5.0,
          "Circle center X coordinate should match expected value"
      );
      assertEquals(
          expectedY,
          centroid.getY(),
          5.0,
          "Circle center Y coordinate should match expected value"
      );
    }

    @Test
    @DisplayName("Converting a grid square to Earth geometry")
    public void testSquareConversion() {
      // Convert grid square to Earth geometry
      EarthGeometry earthGeometry = EarthTransformer.gridToEarth(
          gridSquare, gridCrsDefinition, TARGET_CRS_CODE);

      // Verify results
      assertNotNull(earthGeometry, "Earth geometry should not be null");
      assertTrue(earthGeometry.getInnerGeometry() instanceof Polygon,
          "The geometry should be a Polygon");
      assertEquals(targetCrs, earthGeometry.getCrs(),
          "CRS should be the target CRS");

      // Verify the center coordinates are as expected
      Polygon polygon = (Polygon) earthGeometry.getInnerGeometry();
      Point centroid = polygon.getCentroid();

      double expectedX = WEST_EASTING.doubleValue()
          + gridSquare.getCenterX().doubleValue() * CELL_SIZE.doubleValue();
      double expectedY = NORTH_NORTHING.doubleValue()
          - gridSquare.getCenterY().doubleValue() * CELL_SIZE.doubleValue();

      assertEquals(expectedX, centroid.getX(), 10.0,
          "Square center X coordinate should match expected value");
      assertEquals(expectedY, centroid.getY(), 10.0,
          "Square center Y coordinate should match expected value");
    }
  }

  @Nested
  @DisplayName("Tests for gridToEarth with GridCrsManager")
  class GridCrsManagerTests {

    @Test
    @DisplayName("Converting a grid point using GridCrsManager")
    public void testPointConversionWithRealizedCrs()
        throws FactoryException, IOException, TransformException {
      // Get a GridCrsManager
      GridCrsManager gridCrs = EarthTransformer.getGridCrsManager(gridCrsDefinition);

      // Convert grid point to Earth geometry
      EarthGeometry earthGeometry = EarthTransformer.gridToEarth(
          gridPoint, gridCrs, TARGET_CRS_CODE);

      // Verify results
      assertNotNull(earthGeometry, "Earth geometry should not be null");
      assertTrue(earthGeometry.getInnerGeometry() instanceof Point,
          "The geometry should be a Point");
      assertEquals(targetCrs, earthGeometry.getCrs(),
          "CRS should be the target CRS");

      // Verify the coordinates are as expected
      Point point = (Point) earthGeometry.getInnerGeometry();
      double expectedX = WEST_EASTING.doubleValue()
          + gridPoint.getCenterX().doubleValue() * CELL_SIZE.doubleValue();
      double expectedY = NORTH_NORTHING.doubleValue()
          - gridPoint.getCenterY().doubleValue() * CELL_SIZE.doubleValue();

      assertEquals(expectedX, point.getX(), 2, "X coordinate should match expected value");
      assertEquals(expectedY, point.getY(), 2, "Y coordinate should match expected value");
    }

    @Test
    @DisplayName("Converting shapes with different target CRS")
    public void testConversionWithDifferentTargetCrs()
        throws FactoryException, IOException, TransformException {
      // Use UTM Zone 11N (appropriate for the test area)
      String utmCrsCode = "EPSG:32611";
      GridCrsManager gridCrs = EarthTransformer.getGridCrsManager(gridCrsDefinition);

      // Convert grid circle to Earth geometry with UTM CRS
      EarthGeometry earthGeometry = EarthTransformer.gridToEarth(
          gridCircle, gridCrs, utmCrsCode);

      // Verify results
      assertNotNull(earthGeometry, "Earth geometry should not be null");
      assertEquals(targetCrs, earthGeometry.getCrs(), "CRS should be the UTM CRS");

      // Verify the center coordinates are as expected
      Polygon polygon = (Polygon) earthGeometry.getInnerGeometry();
      Point centroid = polygon.getCentroid();

      double expectedX = WEST_EASTING.doubleValue()
          + gridCircle.getCenterX().doubleValue() * CELL_SIZE.doubleValue();
      double expectedY = NORTH_NORTHING.doubleValue()
          - gridCircle.getCenterY().doubleValue() * CELL_SIZE.doubleValue();

      assertEquals(expectedX, centroid.getX(), 10.0,
          "Circle center X coordinate should match expected value");
      assertEquals(expectedY, centroid.getY(), 10.0,
          "Circle center Y coordinate should match expected value");
    }
  }

  @Nested
  @DisplayName("Tests for caching mechanism")
  class CachingTests {
    @Test
    @DisplayName("GridCrsManager caching mechanism works")
    public void testGridCrsManagerCaching()
        throws FactoryException, IOException, TransformException {
      // Get a GridCrsManager
      GridCrsManager gridCrs1 = EarthTransformer.getGridCrsManager(gridCrsDefinition);

      // Get another GridCrsManager with the same definition
      GridCrsManager gridCrs2 = EarthTransformer.getGridCrsManager(gridCrsDefinition);

      // They should be the same instance due to caching
      assertSame(gridCrs1, gridCrs2,
          "Same GridCrsDefinition should return the same cached GridCrsManager instance");

      // Create a different definition
      PatchBuilderExtents differentExtents = new PatchBuilderExtents(
          WEST_LON,
          NORTH_LAT.add(BigDecimal.ONE),
          EAST_LON,
          SOUTH_LAT
      );

      GridCrsDefinition differentDefinition = new GridCrsDefinition(
          "DifferentTestGrid",
          INPUT_CRS_CODE,
          differentExtents,
          CELL_SIZE,
          "m"
      );

      // Get a GridCrsManager with different definition
      GridCrsManager gridCrs3 = EarthTransformer.getGridCrsManager(differentDefinition);

      // Should be a different instance
      assertNotSame(gridCrs1, gridCrs3,
          "Different GridCrsDefinition should return a different GridCrsManager instance");
    }

    @Test
    @DisplayName("CRS caching mechanism works")
    public void testCrsCaching() throws Exception {
      // Access the private CRS_CACHE field using reflection
      Field crsCacheField = EarthTransformer.class.getDeclaredField("CRS_CACHE");
      crsCacheField.setAccessible(true);
      @SuppressWarnings("unchecked")
      Map<String, CoordinateReferenceSystem> crsCache =
          (Map<String, CoordinateReferenceSystem>) crsCacheField.get(null);

      // Clear the cache to ensure a clean test
      crsCache.clear();

      // First call should create and cache the CRS
      EarthTransformer.gridToEarth(gridPoint, gridCrsDefinition, TARGET_CRS_CODE);
      assertTrue(crsCache.containsKey(TARGET_CRS_CODE),
          "CRS should be cached after first use");

      // Get the cached CRS
      CoordinateReferenceSystem cachedCrs = crsCache.get(TARGET_CRS_CODE);

      // Second call should reuse the cached CRS
      EarthTransformer.gridToEarth(gridPoint, gridCrsDefinition, TARGET_CRS_CODE);

      // The CRS in cache should still be the same instance
      assertSame(cachedCrs, crsCache.get(TARGET_CRS_CODE),
          "Cached CRS instance should be reused");
    }

    @Test
    @DisplayName("EarthGeometryFactory caching mechanism works")
    public void testFactoryCaching() throws Exception {
      // Access the private FACTORY_CACHE field using reflection
      Field factoryCacheField = EarthTransformer.class.getDeclaredField("FACTORY_CACHE");
      factoryCacheField.setAccessible(true);
      @SuppressWarnings("unchecked")
      Map<String, EarthGeometryFactory> factoryCache =
          (Map<String, EarthGeometryFactory>) factoryCacheField.get(null);

      // Clear the cache to ensure a clean test
      factoryCache.clear();

      // Get a GridCrsManager for testing
      GridCrsManager gridCrs = EarthTransformer.getGridCrsManager(gridCrsDefinition);

      // First call should create and cache the factory
      EarthTransformer.gridToEarth(gridPoint, gridCrs, TARGET_CRS_CODE);

      // Verify at least one factory is cached
      assertTrue(factoryCache.size() > 0, "Factory should be cached after first use");

      // Store the size to check no more factories are created
      int cacheSize = factoryCache.size();

      // Second call should reuse the cached factory
      EarthTransformer.gridToEarth(gridPoint, gridCrs, TARGET_CRS_CODE);

      // Cache size should remain the same (no new factories created)
      assertEquals(cacheSize, factoryCache.size(),
          "No new factories should be created for the same parameters");
    }
  }

  @Test
  @DisplayName("Error handling for invalid CRS code")
  public void testInvalidCrsCodeHandling() {
    // Try to convert with an invalid CRS code
    Exception exception = assertThrows(RuntimeException.class, () -> {
      EarthTransformer.gridToEarth(gridPoint, gridCrsDefinition, "INVALID_CRS_CODE");
    });

    assertTrue(exception.getMessage().contains("Failed to convert"),
        "Exception should indicate conversion failure");
  }

  @Nested
  @DisplayName("Tests for projected CRS input (UTM)")
  class ProjectedCrsInputTests {
    // UTM Zone 11N coordinates for test area
    private static final BigDecimal UTM_WEST_EASTING = new BigDecimal("591253.2528339219");
    private static final BigDecimal UTM_NORTH_NORTHING = new BigDecimal("3873499.8508478715");
    private static final BigDecimal UTM_EAST_EASTING = new BigDecimal("684709.8311321359");
    private static final BigDecimal UTM_SOUTH_NORTHING = new BigDecimal("3763959.1395987775");

    private GridCrsDefinition utmGridCrsDefinition;
    private CoordinateReferenceSystem wgs84Crs;

    @BeforeEach
    public void setUpUtm() {
      try {
        wgs84Crs = CRS.forCode("EPSG:4326"); // WGS84

        // Create extents using UTM coordinates
        PatchBuilderExtents utmExtents = new PatchBuilderExtents(
            UTM_WEST_EASTING,    // Top left X (easting)
            UTM_NORTH_NORTHING,  // Top left Y (northing)
            UTM_EAST_EASTING,    // Bottom right X (easting)
            UTM_SOUTH_NORTHING   // Bottom right Y (northing)
        );

        // Create grid CRS definition based on UTM
        utmGridCrsDefinition = new GridCrsDefinition(
            "UtmTestGrid",
            "EPSG:32611", // UTM 11N
            utmExtents,
            CELL_SIZE,
            "m"  // Cell size unit
        );
      } catch (FactoryException e) {
        fail("Failed to create CRS: " + e.getMessage());
      }
    }

    @Test
    @DisplayName("Converting a grid point with UTM input CRS to same UTM zone")
    public void testPointConversionFromUtmToSameUtm() {
      // Use the same UTM zone for input and output (UTM 11N)
      String targetUtmCode = "EPSG:32611";

      // Convert grid point from UTM 11N to UTM 11N (through grid coordinate space)
      EarthGeometry earthGeometry = EarthTransformer.gridToEarth(
          gridPoint, utmGridCrsDefinition, targetUtmCode);

      // Verify results
      assertNotNull(earthGeometry, "Earth geometry should not be null");
      assertTrue(earthGeometry.getInnerGeometry() instanceof Point,
          "The geometry should be a Point");
      assertEquals(targetCrs, earthGeometry.getCrs(),
          "CRS should be the target CRS");

      // Verify the coordinates match expected values in UTM 11N
      Point point = (Point) earthGeometry.getInnerGeometry();

      // Calculate expected position in UTM 11N based on gridPoint position
      double expectedX = UTM_WEST_EASTING.doubleValue()
          + gridPoint.getCenterX().doubleValue() * CELL_SIZE.doubleValue();
      double expectedY = UTM_NORTH_NORTHING.doubleValue()
          - gridPoint.getCenterY().doubleValue() * CELL_SIZE.doubleValue();

      assertEquals(expectedX, point.getX(), 0.01,
          "X coordinate should match expected UTM 11N easting");
      assertEquals(expectedY, point.getY(), 0.01,
          "Y coordinate should match expected UTM 11N northing");
    }

    @Test
    @DisplayName("Converting a grid point with UTM input to WGS84")
    public void testPointConversionFromUtmToWgs84() {
      // Convert grid point from UTM 11N to WGS84
      EarthGeometry earthGeometry = EarthTransformer.gridToEarth(
          gridPoint, utmGridCrsDefinition, "EPSG:4326");

      // Verify results
      assertNotNull(earthGeometry, "Earth geometry should not be null");
      assertTrue(earthGeometry.getInnerGeometry() instanceof Point,
          "The geometry should be a Point");
      assertEquals(targetCrsWgs84, earthGeometry.getCrs(),
          "CRS should be WGS84");

      // We can only verify the point is within the expected bounding box
      Point point = (Point) earthGeometry.getInnerGeometry();
      assertTrue(point.getX() >= WEST_LON.doubleValue(), "Point should be east of west boundary");
      assertTrue(point.getX() <= EAST_LON.doubleValue(), "Point should be west of east boundary");
      assertTrue(point.getY() <= NORTH_LAT.doubleValue(), "Point should be south of north boundary");
      assertTrue(point.getY() >= SOUTH_LAT.doubleValue(), "Point should be north of south boundary");
    }

    @Test
    @DisplayName("Converting shapes with UTM input and output CRS")
    public void testShapeConversionWithUtmInputAndOutput() {
      // Convert grid circle to Earth geometry with same UTM CRS
      EarthGeometry earthGeometry = EarthTransformer.gridToEarth(
          gridCircle, utmGridCrsDefinition, "EPSG:32611");

      // Verify results
      assertNotNull(earthGeometry, "Earth geometry should not be null");
      assertTrue(earthGeometry.getInnerGeometry() instanceof Polygon,
          "The geometry should be a Polygon (circle approximation)");
      assertEquals(targetCrs, earthGeometry.getCrs(),
          "CRS should be the target CRS");

      // Verify the center coordinates are as expected
      Polygon polygon = (Polygon) earthGeometry.getInnerGeometry();
      Point centroid = polygon.getCentroid();

      double expectedX = UTM_WEST_EASTING.doubleValue()
          + gridCircle.getCenterX().doubleValue() * CELL_SIZE.doubleValue();
      double expectedY = UTM_NORTH_NORTHING.doubleValue()
          - gridCircle.getCenterY().doubleValue() * CELL_SIZE.doubleValue();

      assertEquals(expectedX, centroid.getX(), 1.0,
          "Circle center X coordinate should match expected value");
      assertEquals(expectedY, centroid.getY(), 1.0,
          "Circle center Y coordinate should match expected value");
    }

    @Test
    @DisplayName("Converting a grid square with UTM input using GridCrsManager")
    public void testSquareConversionWithRealizedCrs()
        throws FactoryException, IOException, TransformException {
      // Get a GridCrsManager based on UTM
      GridCrsManager utmGridCrs = EarthTransformer.getGridCrsManager(utmGridCrsDefinition);

      // Use the same UTM zone as target (UTM 11N)
      String targetUtmCode = "EPSG:32611";

      // Convert grid square to Earth geometry in UTM 11N
      EarthGeometry earthGeometry = EarthTransformer.gridToEarth(
          gridSquare, utmGridCrs, targetUtmCode);

      // Verify results
      assertNotNull(earthGeometry, "Earth geometry should not be null");
      assertTrue(earthGeometry.getInnerGeometry() instanceof Polygon,
          "The geometry should be a Polygon");
      assertEquals(targetCrs, earthGeometry.getCrs(),
          "CRS should be the target CRS");

      // Verify the center coordinates match expected values in UTM 11N
      Polygon polygon = (Polygon) earthGeometry.getInnerGeometry();
      Point centroid = polygon.getCentroid();

      // Calculate expected position based on grid coordinates
      double expectedX = UTM_WEST_EASTING.doubleValue()
          + gridSquare.getCenterX().doubleValue() * CELL_SIZE.doubleValue();
      double expectedY = UTM_NORTH_NORTHING.doubleValue()
          - gridSquare.getCenterY().doubleValue() * CELL_SIZE.doubleValue();

      assertEquals(expectedX, centroid.getX(), 1.0,
          "Square center X coordinate should match expected UTM 11N easting");
      assertEquals(expectedY, centroid.getY(), 1.0,
          "Square center Y coordinate should match expected UTM 11N northing");
    }
  }

  // Helper methods to create mock grid shapes
  private GridShape createMockGridPoint(BigDecimal x, BigDecimal y) {
    GridShape point = mock(GridShape.class);
    when(point.getGridShapeType()).thenReturn(GridShapeType.POINT);
    when(point.getCenterX()).thenReturn(x);
    when(point.getCenterY()).thenReturn(y);
    return point;
  }

  private GridShape createMockGridCircle(
        BigDecimal centerX, BigDecimal centerY, BigDecimal radius) {
    GridShape circle = mock(GridShape.class);
    when(circle.getGridShapeType()).thenReturn(GridShapeType.CIRCLE);
    when(circle.getCenterX()).thenReturn(centerX);
    when(circle.getCenterY()).thenReturn(centerY);
    when(circle.getWidth()).thenReturn(radius.multiply(new BigDecimal(2.0)));
    return circle;
  }

  private GridShape createMockGridSquare(BigDecimal centerX, BigDecimal centerY, BigDecimal width) {
    GridShape square = mock(GridShape.class);
    when(square.getGridShapeType()).thenReturn(GridShapeType.SQUARE);
    when(square.getCenterX()).thenReturn(centerX);
    when(square.getCenterY()).thenReturn(centerY);
    when(square.getWidth()).thenReturn(width);
    when(square.getHeight()).thenReturn(width);
    return square;
  }
}
