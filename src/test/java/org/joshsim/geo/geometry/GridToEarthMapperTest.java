package org.joshsim.geo.geometry;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.math.BigDecimal;

import org.apache.sis.referencing.CRS;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.joshsim.engine.geometry.grid.GridShape;
import org.joshsim.engine.geometry.grid.GridShapeType;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

import static org.mockito.Mockito.*;

/**
 * Tests for GridToEarthMapper that verify the correct conversion between
 * grid coordinates and Earth-based coordinate reference systems.
 */
public class GridToEarthMapperTest {

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
      targetCrsWgs84 = CRS.forCode("EPSG:4326");
    } catch (FactoryException e) {
      fail("Failed to create target CRS: " + e.getMessage());
    }

    // Create mock grid shapes for testing
    gridPoint = createMockGridPoint(BigDecimal.valueOf(0), BigDecimal.valueOf(0));
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
    public void testPointConversion() {
      // Convert grid point to Earth geometry
      EarthGeometry earthGeometry = GridToEarthMapper.gridToEarth(
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
      assertEquals(expectedX, point.getX(), 0.01, "X coordinate should match expected value");
      assertEquals(expectedY, point.getY(), 0.01, "Y coordinate should match expected value");
    }
    
    @Test
    @DisplayName("Converting a grid point to Earth geometry (WGS84)")
    public void testPointConversionWgs84() {
      // Convert grid point to Earth geometry
      EarthGeometry earthGeometry = GridToEarthMapper.gridToEarth(
          gridPoint, gridCrsDefinition, "EPSG:4326");

      // Verify results
      assertNotNull(earthGeometry, "Earth geometry should not be null");
      assertTrue(earthGeometry.getInnerGeometry() instanceof Point, 
          "The geometry should be a Point");
      assertEquals(targetCrsWgs84, earthGeometry.getCrs(), 
          "CRS should be the target CRS");

      // Verify the coordinates are as expected - translating grid coordinates (in indices)
      // to meters (according to cell size)
      Point point = (Point) earthGeometry.getInnerGeometry();
      double expectedX = WEST_LON.doubleValue()
          + gridPoint.getCenterX().doubleValue() * CELL_SIZE.doubleValue();
      double expectedY = NORTH_LAT.doubleValue()
          - gridPoint.getCenterY().doubleValue() * CELL_SIZE.doubleValue();

      // Check the coordinates are within a small tolerance
      assertEquals(expectedX, point.getX(), 0.01, "X coordinate should match expected value");
      assertEquals(expectedY, point.getY(), 0.01, "Y coordinate should match expected value");
    }

    @Test
    @DisplayName("Converting a grid circle to Earth geometry")
    public void testCircleConversion() {
      // Convert grid circle to Earth geometry
      EarthGeometry earthGeometry = GridToEarthMapper.gridToEarth(
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
          1.0,
          "Circle center X coordinate should match expected value"
      );
      assertEquals(
          expectedY,
          centroid.getY(),
          1.0,
          "Circle center Y coordinate should match expected value"
      );
    }
    
    @Test
    @DisplayName("Converting a grid square to Earth geometry")
    public void testSquareConversion() {
      // Convert grid square to Earth geometry
      EarthGeometry earthGeometry = GridToEarthMapper.gridToEarth(
          gridSquare, gridCrsDefinition, TARGET_CRS_CODE);

      // Verify results
      assertNotNull(earthGeometry, "Earth geometry should not be null");
      assertTrue(earthGeometry.getInnerGeometry() instanceof Polygon, 
          "The geometry should be a Polygon");
      assertEquals(TARGET_CRS_CODE, earthGeometry.getCrs().getName().getCode(), 
          "CRS should be the target CRS");
          
      // Verify the center coordinates are as expected
      Polygon polygon = (Polygon) earthGeometry.getInnerGeometry();
      Point centroid = polygon.getCentroid();
      
      double expectedX = WEST_EASTING.doubleValue()
          + gridSquare.getCenterX().doubleValue() * CELL_SIZE.doubleValue();
      double expectedY = NORTH_NORTHING.doubleValue()
          - gridSquare.getCenterY().doubleValue() * CELL_SIZE.doubleValue();
          
      assertEquals(expectedX, centroid.getX(), 1.0, "Square center X coordinate should match expected value");
      assertEquals(expectedY, centroid.getY(), 1.0, "Square center Y coordinate should match expected value");
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
      GridCrsManager gridCrs = GridToEarthMapper.getGridCrsManager(gridCrsDefinition);
      
      // Convert grid point to Earth geometry
      EarthGeometry earthGeometry = GridToEarthMapper.gridToEarth(
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

      assertEquals(expectedX, point.getX(), 0.01, "X coordinate should match expected value");
      assertEquals(expectedY, point.getY(), 0.01, "Y coordinate should match expected value");
    }
    
    @Test
    @DisplayName("Converting shapes with different target CRS")
    public void testConversionWithDifferentTargetCrs() 
        throws FactoryException, IOException, TransformException {
      // Use UTM Zone 11N (appropriate for the test area)
      String utmCrsCode = "EPSG:32611";
      GridCrsManager gridCrs = GridToEarthMapper.getGridCrsManager(gridCrsDefinition);
      
      // Convert grid circle to Earth geometry with UTM CRS
      EarthGeometry earthGeometry = GridToEarthMapper.gridToEarth(
          gridCircle, gridCrs, utmCrsCode);

      // Verify results
      assertNotNull(earthGeometry, "Earth geometry should not be null");
      assertEquals(utmCrsCode, earthGeometry.getCrs().getName().getCode(), 
          "CRS should be the UTM CRS");
          
      // Verify the center coordinates are as expected
      Polygon polygon = (Polygon) earthGeometry.getInnerGeometry();
      Point centroid = polygon.getCentroid();
      
      double expectedX = WEST_EASTING.doubleValue()
          + gridCircle.getCenterX().doubleValue() * CELL_SIZE.doubleValue();
      double expectedY = NORTH_NORTHING.doubleValue()
          - gridCircle.getCenterY().doubleValue() * CELL_SIZE.doubleValue();
          
      assertEquals(expectedX, centroid.getX(), 1.0, "Circle center X coordinate should match expected value");
      assertEquals(expectedY, centroid.getY(), 1.0, "Circle center Y coordinate should match expected value");
    }
  }

  @Test
  @DisplayName("GridCrsManager caching mechanism works")
  public void testGridCrsManagerCaching() 
      throws FactoryException, IOException, TransformException {
    // Get a GridCrsManager
    GridCrsManager gridCrs1 = GridToEarthMapper.getGridCrsManager(gridCrsDefinition);
    
    // Get another GridCrsManager with the same definition
    GridCrsManager gridCrs2 = GridToEarthMapper.getGridCrsManager(gridCrsDefinition);
    
    // They should be the same instance due to caching
    assertSame(gridCrs1, gridCrs2, 
        "Same GridCrsDefinition should return the same cached GridCrsManager instance");
  }

  @Test
  @DisplayName("Error handling for invalid CRS code")
  public void testInvalidCrsCodeHandling() {
    // Try to convert with an invalid CRS code
    Exception exception = assertThrows(RuntimeException.class, () -> {
      GridToEarthMapper.gridToEarth(gridPoint, gridCrsDefinition, "INVALID_CRS_CODE");
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
      EarthGeometry earthGeometry = GridToEarthMapper.gridToEarth(
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
    @DisplayName("Converting shapes with UTM input and output CRS")
    public void testShapeConversionWithUtmInputAndOutput() {
      // Convert grid circle to Earth geometry with same UTM CRS
      EarthGeometry earthGeometry = GridToEarthMapper.gridToEarth(
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
          
      assertEquals(expectedX, centroid.getX(), 1.0, "Circle center X coordinate should match expected value");
      assertEquals(expectedY, centroid.getY(), 1.0, "Circle center Y coordinate should match expected value");
    }
    
    @Test
    @DisplayName("Converting a grid square with UTM input using GridCrsManager")
    public void testSquareConversionWithRealizedCrs() 
        throws FactoryException, IOException, TransformException {
      // Get a GridCrsManager based on UTM
      GridCrsManager utmGridCrs = GridToEarthMapper.getGridCrsManager(utmGridCrsDefinition);
      
      // Use the same UTM zone as target (UTM 11N)
      String targetUtmCode = "EPSG:32611";
      
      // Convert grid square to Earth geometry in UTM 11N
      EarthGeometry earthGeometry = GridToEarthMapper.gridToEarth(
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

  private GridShape createMockGridCircle(BigDecimal centerX, BigDecimal centerY, BigDecimal radius) {
    GridShape circle = mock(GridShape.class);
    when(circle.getGridShapeType()).thenReturn(GridShapeType.CIRCLE);
    when(circle.getCenterX()).thenReturn(centerX);
    when(circle.getCenterY()).thenReturn(centerY);
    return circle;
  }

  private GridShape createMockGridSquare(BigDecimal centerX, BigDecimal centerY, BigDecimal width) {
    GridShape square = mock(GridShape.class);
    when(square.getGridShapeType()).thenReturn(GridShapeType.SQUARE);
    when(square.getCenterX()).thenReturn(centerX);
    when(square.getCenterY()).thenReturn(centerY);
    when(square.getWidth()).thenReturn(width);
    return square;
  }
}