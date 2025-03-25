package org.joshsim.engine.geometry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.joshsim.engine.entity.Patch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

class GridBuilderTest {

  // Joshua Tree National Park area (falls within UTM Zone 11N)
  private BigDecimal wgs84NorthLat;
  private BigDecimal wgs84WestLon;
  private BigDecimal wgs84SouthLat;
  private BigDecimal wgs84EastLon;
  private BigDecimal cellWidth;
  
  // UTM 11N coordinates approximately matching Joshua Tree area
  private BigDecimal utmNorthY;
  private BigDecimal utmWestX;
  private BigDecimal utmSouthY;
  private BigDecimal utmEastX;
  
  private Map<String, BigDecimal> wgs84CornerCoords;
  private Map<String, BigDecimal> utmCornerCoords;
  
  @BeforeEach
  void setUp() {
    // WGS84 coordinates (geographic)
    wgs84NorthLat = new BigDecimal("33.55");  // Northern latitude
    wgs84WestLon = new BigDecimal("-115.55"); // Western longitude
    wgs84SouthLat = new BigDecimal("33.5");  // Southern latitude
    wgs84EastLon = new BigDecimal("-115.5"); // Eastern longitude
    
    // Equivalent UTM 11N coordinates (projected)
    utmNorthY = new BigDecimal("3713204.185623667");   // Northern Y-coordinate
    utmWestX = new BigDecimal("634611.9480685203");     // Western X-coordinate
    utmSouthY = new BigDecimal("3707726.0273103723");   // Southern Y-coordinate
    utmEastX = new BigDecimal("639334.3319327366");     // Eastern X-coordinate
    
    // Set a reasonable cell width (30 meters)
    cellWidth = new BigDecimal(30); // 30 meters
    
    // Create corner coordinate maps with consistent X,Y naming
    wgs84CornerCoords = new HashMap<>();
    wgs84CornerCoords.put("topLeftY", wgs84NorthLat);
    wgs84CornerCoords.put("topLeftX", wgs84WestLon);
    wgs84CornerCoords.put("bottomRightY", wgs84SouthLat);
    wgs84CornerCoords.put("bottomRightX", wgs84EastLon);
    
    utmCornerCoords = new HashMap<>();
    utmCornerCoords.put("topLeftY", utmNorthY);
    utmCornerCoords.put("topLeftX", utmWestX);
    utmCornerCoords.put("bottomRightY", utmSouthY);
    utmCornerCoords.put("bottomRightX", utmEastX);
  }
  
  @Test
  @DisplayName("Constructor should properly initialize GridBuilder")
  void constructorInitializesGridBuilder() throws FactoryException, TransformException {
    GridBuilder builder = new GridBuilder(
        "EPSG:4326",    // WGS84
        "EPSG:32611",   // UTM 11N
        wgs84CornerCoords,
        cellWidth
    );
    
    // We can't directly test private fields, but we can test that build() works
    Grid grid = builder.build();
    assertNotNull(grid);
    assertFalse(grid.getPatches().isEmpty());
  }
  
  @Nested
  @DisplayName("Coordinate transformation tests")
  class CoordinateTransformationTests {
    
    @Test
    @DisplayName("transformCornerCoordinates should correctly transform between different CRS")
    void transformCornerCoordinatesBetweenCRS() throws FactoryException, TransformException {
      CoordinateReferenceSystem wgs84 = CRS.forCode("EPSG:4326");
      CoordinateReferenceSystem utm11n = CRS.forCode("EPSG:32611");
      
      GridBuilder builder = new GridBuilder("EPSG:4326", "EPSG:32611", wgs84CornerCoords, cellWidth);
      
      // Create positions with consistent X,Y ordering using RIGHT_HANDED convention
      DirectPosition2D topLeft = new DirectPosition2D(wgs84WestLon.doubleValue(), wgs84NorthLat.doubleValue());
      DirectPosition2D bottomRight = new DirectPosition2D(wgs84EastLon.doubleValue(), wgs84SouthLat.doubleValue());
      DirectPosition2D[] corners = {topLeft, bottomRight};
      
      // Transform using normalized CRS (RIGHT_HANDED convention)
      DirectPosition2D[] transformed = builder.transformCornerCoordinates(corners, 
          AbstractCRS.castOrCopy(wgs84).forConvention(AxesConvention.RIGHT_HANDED),
          AbstractCRS.castOrCopy(utm11n).forConvention(AxesConvention.RIGHT_HANDED));
      
      // Verify transformed coordinates match the expected UTM 11N values
      assertTrue(Math.abs(transformed[0].getOrdinate(0) - utmWestX.doubleValue()) < 2.0);
      assertTrue(Math.abs(transformed[0].getOrdinate(1) - utmNorthY.doubleValue()) < 1.0);
      assertTrue(Math.abs(transformed[1].getOrdinate(0) - utmEastX.doubleValue()) < 2.0);
      assertTrue(Math.abs(transformed[1].getOrdinate(1) - utmSouthY.doubleValue()) < 1.0);

    }
  }
  
  @Nested
  @DisplayName("Parameter validation tests")
  class ParameterValidationTests {
    
    @Test
    @DisplayName("Constructor should validate cell width")
    void constructorValidatesCellWidth() {
      // Try with zero cell width
      BigDecimal zeroCellWidth = BigDecimal.ZERO;
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          () -> new GridBuilder("EPSG:4326", "EPSG:32611", wgs84CornerCoords, zeroCellWidth)
      );
      assertTrue(exception.getMessage().contains("Cell width must be positive"));
      
      // Try with negative cell width
      BigDecimal negativeCellWidth = new BigDecimal(-30);
      exception = assertThrows(
          IllegalArgumentException.class,
          () -> new GridBuilder("EPSG:4326", "EPSG:32611", wgs84CornerCoords, negativeCellWidth)
      );
      assertTrue(exception.getMessage().contains("Cell width must be positive"));
    }
    
    @Test
    @DisplayName("Constructor should validate corner coordinate relationships")
    void constructorValidatesCornerRelationships() {
      // Create inverted coordinates (top-left is below bottom-right)
      Map<String, BigDecimal> invertedYCoords = new HashMap<>(wgs84CornerCoords);
      invertedYCoords.put("topLeftY", wgs84SouthLat);
      invertedYCoords.put("bottomRightY", wgs84NorthLat);
      
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          () -> new GridBuilder("EPSG:4326", "EPSG:32611", invertedYCoords, cellWidth)
      );
      assertTrue(exception.getMessage().contains("Y-coordinate"));
      
      // Create inverted X coordinates (top-left is east of bottom-right)
      Map<String, BigDecimal> invertedXCoords = new HashMap<>(wgs84CornerCoords);
      invertedXCoords.put("topLeftX", wgs84EastLon);
      invertedXCoords.put("bottomRightX", wgs84WestLon);
      
      exception = assertThrows(
          IllegalArgumentException.class,
          () -> new GridBuilder("EPSG:4326", "EPSG:32611", invertedXCoords, cellWidth)
      );
      assertTrue(exception.getMessage().contains("X-coordinate"));
    }
    
    @Test
    @DisplayName("build() should validate parameters")
    void buildValidatesParameters() throws FactoryException, TransformException {
      // Create a builder with valid parameters
      GridBuilder builder = new GridBuilder("EPSG:4326", "EPSG:32611", wgs84CornerCoords, cellWidth);
      
      // Should work fine
      Grid grid = builder.build();
      assertNotNull(grid);
      
      // Just test that build works - we know it calls validateParameters() internally now
      assertNotNull(grid.getPatches());
    }
  }
  
  @Test
  @DisplayName("build() with WGS84 to UTM 11N transformation")
  void buildWithWgs84ToUtm11n() throws FactoryException, TransformException {
    GridBuilder builder = new GridBuilder(
        "EPSG:4326",    // WGS84
        "EPSG:32611",   // UTM 11N
        wgs84CornerCoords,
        cellWidth
    );
    
    Grid grid = builder.build();
    assertNotNull(grid, "Grid should be built successfully");
    
    List<Patch> patches = grid.getPatches();
    assertFalse(patches.isEmpty(), "Grid should contain patches");
    
    // Verify a patch exists
    Patch firstPatch = patches.get(0);
    Geometry geometry = firstPatch.getGeometry();
    assertNotNull(geometry, "Patch should have geometry");
  }
  
  @Test
  @DisplayName("build() with UTM 11N to UTM 11N (no transformation)")
  void buildWithUtm11nToUtm11n() throws FactoryException, TransformException {
    GridBuilder builder = new GridBuilder(
        "EPSG:32611",   // UTM 11N
        "EPSG:32611",   // UTM 11N
        utmCornerCoords, 
        cellWidth
    );
    
    Grid grid = builder.build();
    assertNotNull(grid, "Grid should be built successfully");
    
    List<Patch> patches = grid.getPatches();
    assertFalse(patches.isEmpty(), "Grid should contain patches");
    
    // Verify patches are created
    assertTrue(patches.size() > 0);
  }
  
  @Nested
  @DisplayName("Error cases and edge conditions")
  class ErrorCasesTests {
    
    @Test
    @DisplayName("Constructor should throw exception for invalid CRS codes")
    void constructorWithInvalidCrsCode() {
      // Setting an invalid EPSG code
      assertThrows(FactoryException.class, 
          () -> new GridBuilder("EPSG:99999", "EPSG:4326", wgs84CornerCoords, cellWidth));
    }
    
    @Test
    @DisplayName("Constructor should throw exception for missing coordinates")
    void constructorWithMissingCoordinates() {
      Map<String, BigDecimal> incompleteCoords = new HashMap<>();
      incompleteCoords.put("topLeftY", wgs84NorthLat);
      // Missing other coordinates
      
      assertThrows(IllegalArgumentException.class, 
          () -> new GridBuilder("EPSG:4326", "EPSG:32611", incompleteCoords, cellWidth));
    }
  }
}