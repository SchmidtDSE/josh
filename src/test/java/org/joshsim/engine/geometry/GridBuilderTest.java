package org.joshsim.engine.geometry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.GeneralPosition;
import org.geotools.referencing.CRS;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.entity.type.Patch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GridBuilderTest {

  // Coordinate Reference Systems
  private CoordinateReferenceSystem wgs84;
  private CoordinateReferenceSystem utm11n;

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
  private EntityPrototype prototype;

  private GridBuilderExtents wgs84Extents;
  private GridBuilderExtents utm11nExtents;

  @BeforeEach
  void setUp() throws FactoryException {
    // Initialize Coordinate Reference Systems
    wgs84 = CRS.decode("EPSG:4326", true); // WGS84, lefthanded (lon first)
    utm11n = CRS.decode("EPSG:32611"); // UTM Zone 11N

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

    // Create a Test Prototype for the patches
    prototype = new TestPatchEntityPrototype();

    // Set a reasonable cell width (30 meters)
    cellWidth = new BigDecimal(30); // 30 meters

    // Grid Builder extents in X, Y order
    wgs84Extents = new GridBuilderExtents(
        wgs84WestLon,
        wgs84NorthLat,
        wgs84EastLon,
        wgs84SouthLat
    );

    // UTM 11N extents in X, Y order
    utm11nExtents = new GridBuilderExtents(
        utmWestX,
        utmNorthY,
        utmEastX,
        utmSouthY
    );
  }

  @Test
  @DisplayName("Constructor should properly initialize GridBuilder")
  void constructorInitializesGridBuilder() throws FactoryException, TransformException {
    GridBuilder builder = new GridBuilder(
        wgs84,
        utm11n,
        wgs84Extents,
        cellWidth,
        prototype
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
      GridBuilder builder = new GridBuilder(
          wgs84,
          utm11n,
          wgs84Extents,
          cellWidth,
          prototype
      );

      // Create positions with consistent X,Y ordering
      GeneralPosition topLeft = new GeneralPosition(
          wgs84WestLon.doubleValue(),
          wgs84NorthLat.doubleValue()
      );
      topLeft.setCoordinateReferenceSystem(wgs84);

      GeneralPosition bottomRight = new GeneralPosition(
          wgs84EastLon.doubleValue(),
          wgs84SouthLat.doubleValue()
      );
      bottomRight.setCoordinateReferenceSystem(wgs84);

      GeneralPosition[] corners = {topLeft, bottomRight};

      // Transform using CRS
      GeneralPosition[] transformed = builder.transformCornerCoordinates(corners, wgs84, utm11n);

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
          () -> new GridBuilder(wgs84, utm11n, wgs84Extents, zeroCellWidth, prototype)
      );
      assertTrue(exception.getMessage().contains("Cell width must be positive"));

      // Try with negative cell width
      BigDecimal negativeCellWidth = new BigDecimal(-30);
      exception = assertThrows(
          IllegalArgumentException.class,
          () -> new GridBuilder(wgs84, utm11n, wgs84Extents, negativeCellWidth, prototype)
      );
      assertTrue(exception.getMessage().contains("Cell width must be positive"));
    }

    @Test
    @DisplayName("Constructor should validate corner coordinate relationships")
    void constructorValidatesCornerRelationships() {
      // Create inverted coordinates (top-left is below bottom-right)
      IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
              () -> new GridBuilderExtents(
              wgs84WestLon,
              wgs84SouthLat, // Inverted Y (North/South)
              wgs84EastLon,
              wgs84NorthLat
          )
      );
      assertTrue(exception.getMessage().contains("Y-coordinate"));

      exception = assertThrows(
          IllegalArgumentException.class,
              () -> new GridBuilderExtents(
              wgs84EastLon, // Inverted X (East/West)
              wgs84NorthLat,
              wgs84WestLon,
              wgs84SouthLat
          )
      );
      assertTrue(exception.getMessage().contains("X-coordinate"));
    }

    @Test
    @DisplayName("build() should validate parameters")
    void buildValidatesParameters() throws FactoryException, TransformException {
      // Create a builder with valid parameters
      GridBuilder builder = new GridBuilder(
          wgs84,
          utm11n,
          wgs84Extents,
          cellWidth,
          prototype
      );

      // Should work fine
      Grid grid = builder.build();
      assertNotNull(grid);
      assertNotNull(grid.getPatches());
    }
  }

  @Test
  @DisplayName("build() with WGS84 to UTM 11N transformation")
  void buildWithWgs84ToUtm11n() throws FactoryException, TransformException {
    GridBuilder builder = new GridBuilder(
        wgs84,
        utm11n,
        wgs84Extents,
        cellWidth,
        prototype
    );

    Grid grid = builder.build();
    assertNotNull(grid, "Grid should be built successfully");

    List<MutableEntity> patches = grid.getPatches();
    assertFalse(patches.isEmpty(), "Grid should contain patches");

    // Verify a patch exists
    MutableEntity firstPatch = patches.get(0);
    assertTrue(firstPatch.getGeometry().isPresent());
  }

  @Test
  @DisplayName("build() with UTM 11N to UTM 11N (no transformation)")
  void buildWithUtm11nToUtm11n() throws FactoryException, TransformException {
    GridBuilder builder = new GridBuilder(
        utm11n,
        utm11n,
        utm11nExtents,
        cellWidth,
        prototype
    );

    Grid grid = builder.build();
    assertNotNull(grid, "Grid should be built successfully");

    List<MutableEntity> patches = grid.getPatches();
    assertFalse(patches.isEmpty(), "Grid should contain patches");

    // Verify patches are created
    assertTrue(patches.size() > 0);
  }

  @Nested
  @DisplayName("Error cases and edge conditions")
  class ErrorCasesTests {

    @Test
    @DisplayName("Constructor should throw exception for null CRS")
    void constructorWithNullCrs() {
      assertThrows(IllegalArgumentException.class,
          () -> new GridBuilder(null, utm11n, wgs84Extents, cellWidth, prototype));

      assertThrows(IllegalArgumentException.class,
          () -> new GridBuilder(wgs84, null, wgs84Extents, cellWidth, prototype));
    }

    @Test
    @DisplayName("Constructor should throw exception for missing coordinates")
    void constructorWithMissingCoordinates() {
      // Note: GridBuilderExtents constructor will throw an exception for null values,
      // so we need to verify that at the GridBuilderExtents constructor level
      assertThrows(IllegalArgumentException.class,
          () -> new GridBuilderExtents(null, wgs84NorthLat, wgs84EastLon, wgs84SouthLat));

      assertThrows(IllegalArgumentException.class,
          () -> new GridBuilderExtents(wgs84WestLon, null, wgs84EastLon, wgs84SouthLat));
    }
  }

  class TestPatchEntityPrototype implements EntityPrototype {

    @Override
    public String getIdentifier() {
      return "Test";
    }

    @Override
    public EntityType getEntityType() {
      return EntityType.PATCH;
    }

    @Override
    public MutableEntity build() {
      throw new RuntimeException("Requires use of spatial.");
    }

    @Override
    public MutableEntity buildSpatial(Entity parent) {
      throw new RuntimeException("Requires use of spatial.");
    }

    @Override
    public MutableEntity buildSpatial(EngineGeometry parent) {
      return new Patch(parent, "test", new HashMap<>(), new HashMap<>());
    }

    @Override
    public boolean requiresParent() {
      return false;
    }

    @Override
    public boolean requiresGeometry() {
      return true;
    }
  }
}
