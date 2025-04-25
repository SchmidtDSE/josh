package org.joshsim.geo.geometry;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import org.apache.sis.referencing.CRS;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Tests for {@link RealizedGridCrs}.
 *
 * <p>These tests verify that the RealizedGridCrs correctly handles coordinate transformations
 * between grid coordinates and arbitrary coordinate reference systems.
 */
class RealizedGridCrsTest {

  private GridCrsDefinition definition;
  private RealizedGridCrs realizedGridCrs;
  private static final double DELTA = 1e-10;

  @BeforeEach
  void setUp() throws FactoryException, IOException, TransformException {
    // Create a grid definition similar to the example in the prompt
    PatchBuilderExtents extents = new PatchBuilderExtents(
        new BigDecimal("-116.0"), // topLeftX (west longitude)
        new BigDecimal("35.0"),   // topLeftY (north latitude)
        new BigDecimal("-115.0"), // bottomRightX (east longitude)
        new BigDecimal("34.0"));  // bottomRightY (south latitude)

    definition = new GridCrsDefinition(
        "TestGrid",
        "EPSG:4326",
        extents,
        new BigDecimal("30"),
        "m");

    realizedGridCrs = new RealizedGridCrs(definition);
  }

  @Test
  void testGetGridCrsReturnsValidCrs() {
    CoordinateReferenceSystem gridCrs = realizedGridCrs.getGridCrs();

    assertNotNull(gridCrs);
    assertTrue(gridCrs.getName().getCode().contains("TestGrid"));
  }

  @Test
  void testGetBaseCrsReturnsSameCrsAsDefinition() throws FactoryException {
    CoordinateReferenceSystem expectedCrs = CRS.forCode(definition.getBaseCrsCode());
    CoordinateReferenceSystem actualCrs = realizedGridCrs.getBaseCrs();

    assertEquals(expectedCrs.getName().getCode(), actualCrs.getName().getCode());
  }

  @Test
  void testGetDefinitionReturnsOriginalDefinition() {
    GridCrsDefinition returnedDefinition = realizedGridCrs.getDefinition();

    assertEquals(definition.getName(), returnedDefinition.getName());
    assertEquals(definition.getBaseCrsCode(), returnedDefinition.getBaseCrsCode());
    assertEquals(definition.getCellSize(), returnedDefinition.getCellSize());
  }

  @Test
  void testGetCellSizeReturnsCellSizeFromDefinition() {
    double expectedCellSize = definition.getCellSize().doubleValue();
    double actualCellSize = realizedGridCrs.getCellSize();

    assertEquals(expectedCellSize, actualCellSize, DELTA);
  }

  @Test
  void testCreateGridToTargetCrsTransform() throws FactoryException {
    // Create a transform to UTM Zone 11N (common for the area in the test)
    CoordinateReferenceSystem targetCrs = CRS.forCode("EPSG:32611");
    MathTransform transform = realizedGridCrs.createGridToTargetCrsTransform(targetCrs);

    assertNotNull(transform);
    // The transform should have input and output dimensions of 2
    assertEquals(2, transform.getSourceDimensions());
    assertEquals(2, transform.getTargetDimensions());
  }

  @Test
  void testInvalidBaseCrsCodeThrowsException() {
    PatchBuilderExtents extents = new PatchBuilderExtents(
        new BigDecimal("-116.0"),
        new BigDecimal("35.0"),
        new BigDecimal("-115.0"),
        new BigDecimal("34.0"));

    GridCrsDefinition invalidDefinition = new GridCrsDefinition(
        "InvalidCrsTest",
        "INVALID:99999", // Invalid CRS code
        extents,
        new BigDecimal("30"),
        "m");

    // Should throw FactoryException because the CRS code is invalid
    assertThrows(FactoryException.class, () -> new RealizedGridCrs(invalidDefinition));
  }

  @Nested
  class CoordinateTransformTests {
    private CoordinateReferenceSystem utmCrs;
    private MathTransform gridToUtmTransform;

    @BeforeEach
    void setUpTransform() throws FactoryException {
      // UTM Zone 11N is appropriate for the test area
      utmCrs = CRS.forCode("EPSG:32611");
      gridToUtmTransform = realizedGridCrs.createGridToTargetCrsTransform(utmCrs);
    }

    @Test
    void testTransformGridOriginToUtm() throws TransformException {
      // Grid origin (0,0) should map to top-left of extents in UTM
      double[] gridPoint = {0.0, 0.0};
      double[] utmPoint = new double[2];

      gridToUtmTransform.transform(gridPoint, 0, utmPoint, 0, 1);

      // Approximate UTM coordinates for -116,35 are around 241000,3876000
      // The exact values will depend on the projection details
      assertTrue(utmPoint[0] > 230000 && utmPoint[0] < 250000);
      assertTrue(utmPoint[1] > 3860000 && utmPoint[1] < 3890000);
    }

    @Test
    void testTransformGridPointToUtm() throws TransformException {
      // Grid point (10,10) - 10 cells east and south from origin
      double[] gridPoint = {10.0, 10.0};
      double[] utmPoint = new double[2];

      gridToUtmTransform.transform(gridPoint, 0, utmPoint, 0, 1);

      // Should be 300 meters (10 cells * 30m) east and south from origin
      double[] originPoint = new double[2];
      gridToUtmTransform.transform(new double[]{0.0, 0.0}, 0, originPoint, 0, 1);

      assertEquals(300.0, utmPoint[0] - originPoint[0], 0.1);
      // Y decreases as we go south in UTM
      assertEquals(-300.0, utmPoint[1] - originPoint[1], 0.1);
    }
  }

  @Nested
  class RoundTripTests {

    @Test
    void testGridToGeoAndBackMaintainsPosition() throws FactoryException, TransformException {
      // Create direct transform from grid to geo and back
      CoordinateReferenceSystem geoCrs = CRS.forCode("EPSG:4326");
      MathTransform gridToGeo = realizedGridCrs.createGridToTargetCrsTransform(geoCrs);
      MathTransform geoToGrid = gridToGeo.inverse();

      // Test with a grid point (5.5, 8.75)
      double[] gridPoint = {5.5, 8.75};
      double[] geoPoint = new double[2];
      double[] roundTrip = new double[2];

      // Transform grid to geo
      gridToGeo.transform(gridPoint, 0, geoPoint, 0, 1);
      // Transform back to grid
      geoToGrid.transform(geoPoint, 0, roundTrip, 0, 1);

      // Should get back the same point within a small tolerance
      assertArrayEquals(gridPoint, roundTrip, DELTA);
    }

    @Test
    void testGridToUtmAndBackMaintainsPosition() throws FactoryException, TransformException {
      // Create transform from grid to UTM and back
      CoordinateReferenceSystem utmCrs = CRS.forCode("EPSG:32611");
      MathTransform gridToUtm = realizedGridCrs.createGridToTargetCrsTransform(utmCrs);
      MathTransform utmToGrid = gridToUtm.inverse();

      // Test with various grid points to ensure consistent behavior
      double[][] testPoints = {
        {0.0, 0.0},    // Origin
        {10.0, 10.0},  // Middle point
        {33.3, 33.3}   // Arbitrary point
      };

      for (double[] gridPoint : testPoints) {
        double[] utmPoint = new double[2];
        double[] roundTrip = new double[2];

        // Transform grid to UTM
        gridToUtm.transform(gridPoint, 0, utmPoint, 0, 1);
        // Transform back to grid
        utmToGrid.transform(utmPoint, 0, roundTrip, 0, 1);

        // Should get back the same point within a small tolerance
        assertArrayEquals(gridPoint, roundTrip, DELTA);
      }
    }
  }

  @Nested
  class CellSizeTests {

    @Test
    void testCellSizeMaintainedInTransformation() throws FactoryException, TransformException {
      // Create transform to UTM
      CoordinateReferenceSystem utmCrs = CRS.forCode("EPSG:32611");
      MathTransform gridToUtm = realizedGridCrs.createGridToTargetCrsTransform(utmCrs);

      // Get UTM coordinates for grid cells (0,0) and (1,0)
      double[] gridOrigin = {0.0, 0.0};
      double[] gridEast = {1.0, 0.0};

      double[] utmOrigin = new double[2];
      double[] utmEast = new double[2];

      gridToUtm.transform(gridOrigin, 0, utmOrigin, 0, 1);
      gridToUtm.transform(gridEast, 0, utmEast, 0, 1);

      // The distance should be approximately the cell size (30m)
      double distance = Math.sqrt(
          Math.pow(utmEast[0] - utmOrigin[0], 2)
          + Math.pow(utmEast[1] - utmOrigin[1], 2));

      assertEquals(30.0, distance, 0.1);
    }
  }
}
