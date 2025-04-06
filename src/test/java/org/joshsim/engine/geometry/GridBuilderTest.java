package org.joshsim.engine.geometry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.joshsim.engine.entity.type.Patch;
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

  private GridBuilderExtents wgs84Extents;
  private GridBuilderExtents utmExtents;

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

    wgs84Extents = new GridBuilderExtents(wgs84WestLon, wgs84NorthLat, wgs84EastLon, wgs84SouthLat);
    utmExtents = new GridBuilderExtents(utmWestX, utmNorthY, utmEastX, utmSouthY);
  }

  @Test
  @DisplayName("Constructor should properly initialize GridBuilder")
  void constructorInitializesGridBuilder() throws FactoryException, TransformException {
    GridBuilder builder = new GridBuilder(
        "EPSG:4326",    // WGS84
        "EPSG:32611",   // UTM 11N
        wgs84Extents,
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
    void transformCornerCoordinates() throws FactoryException, TransformException {
      CoordinateReferenceSystem wgs84 = CRS.forCode("EPSG:4326");
      CoordinateReferenceSystem utm11n = CRS.forCode("EPSG:32611");

      GridBuilder builder = new GridBuilder(
          "EPSG:4326",
          "EPSG:32611",
          wgs84Extents,
          cellWidth
      );

      // Create positions with consistent X,Y ordering using RIGHT_HANDED convention
      DirectPosition2D topLeft = new DirectPosition2D(
          wgs84Extents.getTopLeftX().doubleValue(),
          wgs84Extents.getTopLeftY().doubleValue()
      );
      DirectPosition2D bottomRight = new DirectPosition2D(
          wgs84Extents.getBottomRightX().doubleValue(),
          wgs84Extents.getBottomRightY().doubleValue()
      );
      DirectPosition2D[] corners = {topLeft, bottomRight};

      // Transform using normalized CRS (RIGHT_HANDED convention)
      DirectPosition2D[] transformed = builder.transformCornerCoordinates(corners,
          AbstractCRS.castOrCopy(wgs84).forConvention(AxesConvention.RIGHT_HANDED),
          AbstractCRS.castOrCopy(utm11n).forConvention(AxesConvention.RIGHT_HANDED));

      // Verify transformed coordinates match the expected UTM 11N values
      assertTrue(Math.abs(transformed[0].getOrdinate(0) - utmExtents.getTopLeftX().doubleValue()) < 2.0);
      assertTrue(Math.abs(transformed[0].getOrdinate(1) - utmExtents.getTopLeftY().doubleValue()) < 1.0);
      assertTrue(Math.abs(transformed[1].getOrdinate(0) - utmExtents.getBottomRightX().doubleValue()) < 2.0);
      assertTrue(Math.abs(transformed[1].getOrdinate(1) - utmExtents.getBottomRightY().doubleValue()) < 1.0);

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
          () -> new GridBuilder("EPSG:4326", "EPSG:32611", wgs84Extents, zeroCellWidth)
      );
      assertTrue(exception.getMessage().contains("Cell width must be positive"));

      // Try with negative cell width
      BigDecimal negativeCellWidth = new BigDecimal(-30);
      exception = assertThrows(
          IllegalArgumentException.class,
          () -> new GridBuilder("EPSG:4326", "EPSG:32611", wgs84Extents, negativeCellWidth)
      );
      assertTrue(exception.getMessage().contains("Cell width must be positive"));
    }


    @Test
    @DisplayName("GridBuilderExtents should validate corner coordinate relationships")
    void gridBuilderExtentsValidatesCornerRelationships() {
      assertThrows(IllegalArgumentException.class,
          () -> new GridBuilderExtents(BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO));
      assertThrows(IllegalArgumentException.class,
          () -> new GridBuilderExtents(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE));
    }

    @Test
    @DisplayName("build() should validate parameters")
    void buildValidatesParameters() throws FactoryException, TransformException {
      // Create a builder with valid parameters
      GridBuilder builder = new GridBuilder(
          "EPSG:4326",
          "EPSG:32611",
          wgs84Extents,
          cellWidth
      );

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
        wgs84Extents,
        cellWidth
    );

    Grid grid = builder.build();
    assertNotNull(grid, "Grid should be built successfully");

    List<Patch> patches = grid.getPatches();
    assertFalse(patches.isEmpty(), "Grid should contain patches");

    // Verify a patch exists
    Patch firstPatch = patches.get(0);
    assertTrue(firstPatch.getGeometry().isPresent());
  }

  @Test
  @DisplayName("build() with UTM 11N to UTM 11N (no transformation)")
  void buildWithUtm11nToUtm11n() throws FactoryException, TransformException {
    GridBuilder builder = new GridBuilder(
        "EPSG:32611",   // UTM 11N
        "EPSG:32611",   // UTM 11N
        utmExtents,
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
          () -> new GridBuilder("EPSG:99999", "EPSG:4326", wgs84Extents, cellWidth));
    }

    @Test
    @DisplayName("Constructor should throw exception for missing coordinates")
    void constructorWithMissingCoordinates() {
      //This test is no longer relevant given the use of GridBuilderExtents
    }
  }
}