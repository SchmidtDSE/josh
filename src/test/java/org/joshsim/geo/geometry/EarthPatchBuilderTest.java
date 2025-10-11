package org.joshsim.geo.geometry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.entity.type.EntityType;
import org.joshsim.engine.entity.type.Patch;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.PatchBuilder;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;


class EarthPatchBuilderTest {

  // Coordinate Reference Systems
  private String wgs84;
  private String utm11n;

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

  private PatchBuilderExtents wgs84Extents;
  private PatchBuilderExtents utm11nExtents;

  @BeforeEach
  void setUp() throws FactoryException {
    // Initialize Coordinate Reference Systems
    wgs84 = "EPSG:4326"; // WGS84, lefthanded (lon first)
    utm11n = "EPSG:32611"; // UTM Zone 11N

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

    // PatchSet Builder extents in X, Y order
    wgs84Extents = new PatchBuilderExtents(
        wgs84WestLon,
        wgs84NorthLat,
        wgs84EastLon,
        wgs84SouthLat
    );

    // UTM 11N extents in X, Y order
    utm11nExtents = new PatchBuilderExtents(
        utmWestX,
        utmNorthY,
        utmEastX,
        utmSouthY
    );
  }

  @Test
  @DisplayName("Constructor should properly initialize PatchBuilder")
  void constructorInitializesGridBuilder() throws FactoryException, TransformException {
    PatchBuilder builder = new EarthPatchBuilder(
        wgs84,
        utm11n,
        wgs84Extents,
        cellWidth,
        prototype
    );

    // We can't directly test private fields, but we can test that build() works
    PatchSet grid = builder.build();
    assertNotNull(grid);
    assertFalse(grid.getPatches().isEmpty());
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
          () -> new EarthPatchBuilder(wgs84, utm11n, wgs84Extents, zeroCellWidth, prototype)
      );
      assertTrue(exception.getMessage().contains("Cell width must be positive"));

      // Try with negative cell width
      BigDecimal negativeCellWidth = new BigDecimal(-30);
      exception = assertThrows(
          IllegalArgumentException.class,
          () -> new EarthPatchBuilder(wgs84, utm11n, wgs84Extents, negativeCellWidth, prototype)
      );
      assertTrue(exception.getMessage().contains("Cell width must be positive"));
    }

    @Test
    @DisplayName("Constructor should validate corner coordinate relationships")
    void constructorValidatesCornerRelationships() throws TransformException {

      // Create inverted coordinates (top-left is below bottom-right)
      IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
        PatchBuilderExtents extents = new PatchBuilderExtents(
            wgs84WestLon,
            wgs84SouthLat, // Inverted Y (North/South)
            wgs84EastLon,
            wgs84NorthLat
        );
        EarthPatchBuilder builder = new EarthPatchBuilder(
            wgs84,
            utm11n,
            extents,
            BigDecimal.ONE,
            prototype
        );
      });
      assertTrue(exception.getMessage().contains("Y-coordinate"));

      exception = assertThrows(IllegalArgumentException.class, () -> {
        PatchBuilderExtents extents = new PatchBuilderExtents(
            wgs84EastLon, // Inverted X (East/West)
            wgs84NorthLat,
            wgs84WestLon,
            wgs84SouthLat
        );

        EarthPatchBuilder builder = new EarthPatchBuilder(
            wgs84,
            utm11n,
            extents,
            BigDecimal.ONE,
            prototype
        );
      });
      assertTrue(exception.getMessage().contains("X-coordinate"));
    }

    @Test
    @DisplayName("build() should validate parameters")
    void buildValidatesParameters() throws FactoryException, TransformException {
      // Create a builder with valid parameters
      PatchBuilder builder = new EarthPatchBuilder(
          wgs84,
          utm11n,
          wgs84Extents,
          cellWidth,
          prototype
      );

      // Should work fine
      PatchSet grid = builder.build();
      assertNotNull(grid);
      assertNotNull(grid.getPatches());
    }
  }

  @Test
  @DisplayName("build() with WGS84 to UTM 11N transformation")
  void buildWithWgs84ToUtm11n() throws FactoryException, TransformException {
    PatchBuilder builder = new EarthPatchBuilder(
        wgs84,
        utm11n,
        wgs84Extents,
        cellWidth,
        prototype
    );

    PatchSet grid = builder.build();
    assertNotNull(grid, "PatchSet should be built successfully");

    List<MutableEntity> patches = grid.getPatches();
    assertFalse(patches.isEmpty(), "PatchSet should contain patches");

    // Verify a patch exists
    MutableEntity firstPatch = patches.get(0);
    assertTrue(firstPatch.getGeometry().isPresent());
  }

  @Nested
  @DisplayName("Error cases and edge conditions")
  class ErrorCasesTests {

    @Test
    @DisplayName("Constructor should throw exception for null CRS")
    void constructorWithNullCrs() {
      assertThrows(IllegalArgumentException.class,
          () -> new EarthPatchBuilder(null, utm11n, wgs84Extents, cellWidth, prototype));

      assertThrows(IllegalArgumentException.class,
          () -> new EarthPatchBuilder(wgs84, null, wgs84Extents, cellWidth, prototype));
    }

    @Test
    @DisplayName("Constructor should throw exception for missing coordinates")
    void constructorWithMissingCoordinates() throws TransformException, FactoryException {
      PatchBuilder builder = new EarthPatchBuilder(
          utm11n,
          utm11n,
          utm11nExtents,
          cellWidth,
          prototype
      );

      // Note: PatchBuilderExtents constructor will throw an exception for null values,
      // so we need to verify that at the PatchBuilderExtents constructor level
      assertThrows(IllegalArgumentException.class, () -> {
        PatchBuilderExtents extents = new PatchBuilderExtents(
            null,
            wgs84NorthLat,
            wgs84EastLon,
            wgs84SouthLat
        );
      });

      assertThrows(IllegalArgumentException.class, () -> {
        PatchBuilderExtents extents = new PatchBuilderExtents(
            wgs84WestLon,
            null,
            wgs84EastLon,
            wgs84SouthLat
        );

      });
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
      return new Patch(
          parent, "test", new HashMap<>(), new HashMap<>(), java.util.Collections.emptyMap());
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
