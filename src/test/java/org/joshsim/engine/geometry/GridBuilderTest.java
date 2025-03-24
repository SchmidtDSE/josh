package org.joshsim.engine.geometry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import org.joshsim.engine.entity.Patch;
import org.joshsim.engine.value.DecimalScalar;
import org.joshsim.engine.value.EngineValue;
import org.joshsim.engine.value.EngineValueWideningCaster;
import org.joshsim.engine.value.Units;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

class GridBuilderTest {

  // Joshua Tree National Park area (falls within UTM Zone 11N)
  private BigDecimal wgs84NorthLat;
  private BigDecimal wgs84WestLon;
  private BigDecimal wgs84SouthLat;
  private BigDecimal wgs84EastLon;
  private EngineValue cellWidth;
  
  // UTM 11N coordinates approximately matching Joshua Tree area
  private BigDecimal utmNorthY;
  private BigDecimal utmWestX;
  private BigDecimal utmSouthY;
  private BigDecimal utmEastX;
  
  @BeforeEach
  void setUp() {
    // WGS84 coordinates (geographic)
    wgs84NorthLat = new BigDecimal("34.0"); // Northern latitude
    wgs84WestLon = new BigDecimal("-116.0"); // Western longitude
    wgs84SouthLat = new BigDecimal("33.0"); // Southern latitude
    wgs84EastLon = new BigDecimal("-115.0"); // Eastern longitude
    
    // UTM 11N coordinates (projected)
    utmNorthY = new BigDecimal("3760000"); // North Y (meters)
    utmWestX = new BigDecimal("520000");   // West X (meters)
    utmSouthY = new BigDecimal("3650000"); // South Y (meters)
    utmEastX = new BigDecimal("580000");   // East X (meters)
    
    // Set a reasonable cell width (30 meters)
    cellWidth = new DecimalScalar(
        new EngineValueWideningCaster(),
        new BigDecimal("30"),
        new Units("m")
    );
  }
  
  @Test
  @DisplayName("Default constructor should initialize with WGS84")
  void defaultConstructorInitializesWithWgs84() {
    GridBuilder builder = new GridBuilder();
    CoordinateReferenceSystem crs = builder.getInputCoordinateReferenceSystem();
    assertNotNull(crs, "CRS should be initialized");
    assertTrue(crs instanceof org.opengis.referencing.crs.GeographicCRS, 
          "Default CRS should be geographic (WGS84)");
  }
  
  @Test
  @DisplayName("Builder should allow setting input and target CRS")
  void setCoordinateReferenceSystemParameters() throws FactoryException {
    GridBuilder builder = new GridBuilder();
    
    // Set UTM 11N as input CRS
    builder = builder.setInputCoordinateReferenceSystem("EPSG:32611");
    assertNotNull(builder.getInputCoordinateReferenceSystem(), "Input CRS should be set");
    
    // Set UTM 11N as target CRS
    builder = builder.setTargetCoordinateReferenceSystem("EPSG:32611");
    assertNotNull(builder.getTargetCoordinateReferenceSystem(), "Target CRS should be set");
  }
  
  @Nested
  @DisplayName("Parameter validation tests")
  class ParameterValidationTests {
    
    @Test
    @DisplayName("build() should require all parameters to be set")
    void buildRequiresAllParameters() {
      GridBuilder builder = new GridBuilder();
      
      // No parameters set
      assertThrows(IllegalStateException.class, () -> builder.build(),
            "Should throw when no parameters are set");
      
      // Only top-left set
      builder.setTopLeft(wgs84NorthLat, wgs84WestLon);
      assertThrows(IllegalStateException.class, () -> builder.build(),
            "Should throw when only top-left is set");
      
      // Top-left and bottom-right set, no cell width
      builder.setBottomRight(wgs84SouthLat, wgs84EastLon);
      assertThrows(IllegalStateException.class, () -> builder.build(),
            "Should throw when cell width is not set");
      
      // All parameters except target CRS
      builder.setCellWidth(cellWidth);
      assertThrows(IllegalStateException.class, () -> builder.build(),
            "Should throw when target CRS is not set");
    }
    
    @Test
    @DisplayName("build() should validate geographic coordinate relationships")
    void validateGeographicCoordinates() throws FactoryException {
      GridBuilder builder = new GridBuilder()
          .setTargetCoordinateReferenceSystem("EPSG:4326")
          .setCellWidth(cellWidth);
      
      // Swapped latitudes (south > north)
      builder.setTopLeft(wgs84SouthLat, wgs84WestLon)
          .setBottomRight(wgs84NorthLat, wgs84EastLon);
      
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
          () -> builder.build());
      assertTrue(exception.getMessage().contains("latitude"),
           "Should throw when latitudes are in wrong order");
      
      // Swapped longitudes (east < west)
      builder.setTopLeft(wgs84NorthLat, wgs84EastLon)
          .setBottomRight(wgs84SouthLat, wgs84WestLon);
      
      exception = assertThrows(IllegalArgumentException.class, 
        () -> builder.build());
      assertTrue(exception.getMessage().contains("longitude"),
           "Should throw when longitudes are in wrong order");
    }
    
    @Test
    @DisplayName("build() should validate projected coordinate relationships")
    void validateProjectedCoordinates() throws FactoryException {
      GridBuilder builder = new GridBuilder()
          .setInputCoordinateReferenceSystem("EPSG:32611")
          .setTargetCoordinateReferenceSystem("EPSG:32611")
          .setCellWidth(cellWidth);
        
      // Swapped Y coordinates (south > north)
      builder.setTopLeft(utmSouthY, utmWestX)
          .setBottomRight(utmNorthY, utmEastX);
      
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
          () -> builder.build());
      assertTrue(exception.getMessage().contains("Y-coordinate"),
           "Should throw when Y coordinates are in wrong order");
      
      // Swapped X coordinates (east < west)
      builder.setTopLeft(utmNorthY, utmEastX)
          .setBottomRight(utmSouthY, utmWestX);
      
      exception = assertThrows(IllegalArgumentException.class, 
        () -> builder.build());
      assertTrue(exception.getMessage().contains("X-coordinate"),
           "Should throw when X coordinates are in wrong order");
    }
    
    @Test
    @DisplayName("build() should reject negative or zero cell width")
    void validateCellWidth() throws FactoryException {
      GridBuilder builder = new GridBuilder()
          .setTopLeft(wgs84NorthLat, wgs84WestLon)
          .setBottomRight(wgs84SouthLat, wgs84EastLon)
          .setTargetCoordinateReferenceSystem("EPSG:4326");
      
      // Zero cell width
      EngineValue zeroCellWidth = new DecimalScalar(
          new EngineValueWideningCaster(), BigDecimal.ZERO, new Units("m"));
      builder.setCellWidth(zeroCellWidth);
      
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
          () -> builder.build());
      assertTrue(exception.getMessage().contains("Cell width must be positive"));
      
      // Negative cell width
      EngineValue negativeCellWidth = new DecimalScalar(
          new EngineValueWideningCaster(), new BigDecimal("-10"), new Units("m"));
      builder.setCellWidth(negativeCellWidth);
      
      exception = assertThrows(IllegalArgumentException.class, 
        () -> builder.build());
      assertTrue(exception.getMessage().contains("Cell width must be positive"));
    }
  }
  
  @Nested
  @DisplayName("Grid building with CRS transformations")
  class GridBuildingTests {
    
    @Test
    @DisplayName("build() with WGS84 to WGS84 (no transformation)")
    void buildWithWgs84ToWgs84() throws FactoryException {
      GridBuilder builder = new GridBuilder()
          .setTopLeft(wgs84NorthLat, wgs84WestLon)
          .setBottomRight(wgs84SouthLat, wgs84EastLon)
          .setCellWidth(cellWidth)
          .setTargetCoordinateReferenceSystem("EPSG:4326"); // WGS84
        
      Grid grid = builder.build();
      assertNotNull(grid, "Grid should be built successfully");
      
      List<Patch> patches = grid.getPatches();
      assertFalse(patches.isEmpty(), "Grid should contain patches");
      
      // Verify first patch has correct geometry
      Patch firstPatch = patches.get(0);
      Geometry geometry = firstPatch.getGeometry();
      assertNotNull(geometry, "Patch should have geometry");
      
      // Center should be within our grid bounds
      BigDecimal centerLat = geometry.getCenterLatitude();
      BigDecimal centerLon = geometry.getCenterLongitude();
      
      assertTrue(centerLat.compareTo(wgs84SouthLat) >= 0 && centerLat.compareTo(wgs84NorthLat) <= 0,
          "Center latitude should be within grid bounds");
      assertTrue(centerLon.compareTo(wgs84WestLon) >= 0 && centerLon.compareTo(wgs84EastLon) <= 0,
          "Center longitude should be within grid bounds");
    }
    
    @Test
    @DisplayName("build() with WGS84 to UTM 11N transformation")
    void buildWithWgs84ToUtm11n() throws FactoryException {
      GridBuilder builder = new GridBuilder()
          .setTopLeft(wgs84NorthLat, wgs84WestLon)
          .setBottomRight(wgs84SouthLat, wgs84EastLon)
          .setCellWidth(cellWidth)
          .setTargetCoordinateReferenceSystem("EPSG:32611"); // UTM 11N
      
      Grid grid = builder.build();
      assertNotNull(grid, "Grid should be built successfully");
      
      List<Patch> patches = grid.getPatches();
      assertFalse(patches.isEmpty(), "Grid should contain patches");
      
      // Verify first patch has correct geometry
      Patch firstPatch = patches.get(0);
      Geometry geometry = firstPatch.getGeometry();
      assertNotNull(geometry, "Patch should have geometry");
      
      // Since we're converting to UTM, the coordinates will be in meters
      // Check that patch centers are within a reasonable UTM 11N range for our area
      BigDecimal centerLat = geometry.getCenterLatitude();
      BigDecimal centerLon = geometry.getCenterLongitude();
      
      // These bounds are approximate UTM values for the Joshua Tree area
      assertTrue(centerLat.doubleValue() > 3600000 && centerLat.doubleValue() < 3800000,
          "Center northing should be in reasonable UTM 11N range");
      assertTrue(centerLon.doubleValue() > 500000 && centerLon.doubleValue() < 600000,
          "Center easting should be in reasonable UTM 11N range");
    }
    
    @Test
    @DisplayName("build() with UTM 11N to WGS84 transformation")
    void buildWithUtm11nToWgs84() throws FactoryException {
      GridBuilder builder = new GridBuilder()
          .setInputCoordinateReferenceSystem("EPSG:32611") // UTM 11N
          .setTopLeft(utmNorthY, utmWestX)
          .setBottomRight(utmSouthY, utmEastX)
          .setCellWidth(cellWidth)
          .setTargetCoordinateReferenceSystem("EPSG:4326"); // WGS84
        
      Grid grid = builder.build();
      assertNotNull(grid, "Grid should be built successfully");
      
      List<Patch> patches = grid.getPatches();
      assertFalse(patches.isEmpty(), "Grid should contain patches");
      
      // Verify first patch has correct geometry
      Patch firstPatch = patches.get(0);
      Geometry geometry = firstPatch.getGeometry();
      assertNotNull(geometry, "Patch should have geometry");
      
      // Since we're converting to WGS84, coordinates should be in degrees
      BigDecimal centerLat = geometry.getCenterLatitude();
      BigDecimal centerLon = geometry.getCenterLongitude();
      
      // Check that patch centers are within a reasonable WGS84 range for Joshua Tree area
      assertTrue(centerLat.doubleValue() > 32.0 && centerLat.doubleValue() < 35.0,
           "Center latitude should be in reasonable WGS84 range for Joshua Tree");
      assertTrue(centerLon.doubleValue() > -117.0 && centerLon.doubleValue() < -114.0,
          "Center longitude should be in reasonable WGS84 range for Joshua Tree");
    }
    
    @Test
    @DisplayName("build() with UTM 11N to UTM 11N (no transformation)")
    void buildWithUtm11nToUtm11n() throws FactoryException {
      GridBuilder builder = new GridBuilder()
          .setInputCoordinateReferenceSystem("EPSG:32611") // UTM 11N
          .setTargetCoordinateReferenceSystem("EPSG:32611") // UTM 11N
          .setTopLeft(utmNorthY, utmWestX)
          .setBottomRight(utmSouthY, utmEastX)
          .setCellWidth(cellWidth);
      
      Grid grid = builder.build();
      assertNotNull(grid, "Grid should be built successfully");
      
      List<Patch> patches = grid.getPatches();
      assertFalse(patches.isEmpty(), "Grid should contain patches");
      
      // Verify first patch has correct geometry
      Patch firstPatch = patches.get(0);
      Geometry geometry = firstPatch.getGeometry();
      assertNotNull(geometry, "Patch should have geometry");
      
      // Since we're staying in UTM, the coordinates will remain in meters
      BigDecimal centerLat = geometry.getCenterLatitude();
      BigDecimal centerLon = geometry.getCenterLongitude();
      
      // Center should be within our grid bounds
      assertTrue(centerLat.compareTo(utmSouthY) >= 0 && centerLat.compareTo(utmNorthY) <= 0,
          "Center northing should be within grid bounds");
      assertTrue(centerLon.compareTo(utmWestX) >= 0 && centerLon.compareTo(utmEastX) <= 0,
          "Center easting should be within grid bounds");
    }
  }
  
  @Nested
  @DisplayName("Error cases and edge conditions")
  class ErrorCasesTests {
    
    @Test
    @DisplayName("build() should throw exception for invalid CRS codes")
    void buildWithInvalidCrsCode() {
      GridBuilder builder = new GridBuilder();
      
      // Setting an invalid EPSG code
      assertThrows(FactoryException.class, 
          () -> builder.setTargetCoordinateReferenceSystem("EPSG:99999"));
    }
    
    @Test
    @DisplayName("build() should handle small cell width that creates many cells")
    void buildWithSmallCellWidth() throws FactoryException {
      // Use a smaller area to keep test efficient
      BigDecimal smallNorthLat = new BigDecimal("33.1");
      BigDecimal smallWestLon = new BigDecimal("-115.1");
      BigDecimal smallSouthLat = new BigDecimal("33.0");
      BigDecimal smallEastLon = new BigDecimal("-115.0");
      
      // Small cell width (0.001 degrees, approximately 100m)
      EngineValue smallCellWidth = new DecimalScalar(
          new EngineValueWideningCaster(), new BigDecimal("0.001"), new Units("degrees"));
      
      GridBuilder builder = new GridBuilder()
          .setTopLeft(smallNorthLat, smallWestLon)
          .setBottomRight(smallSouthLat, smallEastLon)
          .setCellWidth(smallCellWidth)
          .setTargetCoordinateReferenceSystem("EPSG:4326");
      
      Grid grid = builder.build();
      assertNotNull(grid, "Grid should be built successfully with small cell width");
      assertTrue(grid.getPatches().size() > 10, "Grid should have multiple patches");
    }
  }
}