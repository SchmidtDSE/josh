package org.joshsim.geo.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Tests for the EarthGeometry class, organized by EarthGeometry type.
 */
public class EarthGeometryTest {

  private GeometryFactory geometryFactory;
  private CoordinateReferenceSystem wgs84;
  private CoordinateReferenceSystem utm11n;

  private double[][] validUtm11nCoordinates;
  private Coordinate defaultValidCoordinate;
  private Point defaultValidPoint;
  private Polygon defaultValidPolygon;

  /**
   * Set up test environment.
   */
  @BeforeEach
  public void setUp() throws FactoryException {
    geometryFactory = new GeometryFactory();

    // Using Apache SIS for CRS definitions
    wgs84 = CommonCRS.WGS84.geographic();
    utm11n = CRS.forCode("EPSG:32611"); // UTM Zone 11N

    // Initialize valid coordinates for UTM Zone 11N (approximately -120° to -114° longitude)
    validUtm11nCoordinates = new double[][] {
        {-117.0, 34.0},   // Southern California
        {-118.2, 34.0},   // Los Angeles area
        {-116.5, 33.8},   // Palm Springs area
        {-119.8, 36.7},   // Central California
        {-115.0, 35.0}    // Mojave Desert area
    };

    // Set up a default valid coordinate/point for simple tests
    defaultValidCoordinate = new Coordinate(
        validUtm11nCoordinates[0][0], validUtm11nCoordinates[0][1]
    );
    defaultValidPoint = geometryFactory.createPoint(defaultValidCoordinate);

    // Create a valid rectangle that's within UTM Zone 11N
    Coordinate[] coords = new Coordinate[5];
    double lonWidth = 0.02;  // small lon/lat rectangle around the valid point
    double latHeight = 0.02;
    coords[0] = new Coordinate(
        defaultValidCoordinate.x - lonWidth, defaultValidCoordinate.y - latHeight
    );
    coords[1] = new Coordinate(
        defaultValidCoordinate.x + lonWidth,
        defaultValidCoordinate.y - latHeight
    );
    coords[2] = new Coordinate(
        defaultValidCoordinate.x + lonWidth,
        defaultValidCoordinate.y + latHeight
    );
    coords[3] = new Coordinate(
        defaultValidCoordinate.x - lonWidth,
        defaultValidCoordinate.y + latHeight
    );
    coords[4] = coords[0]; // close the ring
    defaultValidPolygon = geometryFactory.createPolygon(coords);
  }

  @Test
  public void testConstructor() {
    Point point = geometryFactory.createPoint(new Coordinate(10.0, 20.0));
    EarthGeometry geometry = new EarthGeometry(point, wgs84);

    assertNotNull(geometry, "EarthGeometry should be initialized");
    assertEquals(
        point,
        geometry.getInnerGeometry(),
        "Inner EarthGeometry should be set correctly"
    );
    assertEquals(wgs84, geometry.getCrs(), "CRS should be set correctly");
  }

  @Test
  public void testConstructorWithTransformers() {
    Point point = geometryFactory.createPoint(new Coordinate(10.0, 20.0));
    Optional<Map<CoordinateReferenceSystem, MathTransform>> transformers =
        Optional.of(new HashMap<>());
    EarthGeometry geometry = new EarthGeometry(point, wgs84, transformers);

    assertNotNull(geometry, "EarthGeometry should be initialized");
    assertEquals(
        point,
        geometry.getInnerGeometry(),
        "Inner EarthGeometry should be set correctly"
    );
    assertEquals(wgs84, geometry.getCrs(), "CRS should be set correctly");
  }

  @Test
  public void testWithNullGeometry() {
    Exception exception = assertThrows(NullPointerException.class, () -> {
      new EarthGeometry(null, wgs84);
    });
  }

  @Test
  public void testWithNullCrs() {
    Point point = geometryFactory.createPoint(new Coordinate(10.0, 20.0));
    Exception exception = assertThrows(NullPointerException.class, () -> {
      new EarthGeometry(point, null);
    });
  }

  @Nested
  class EnginePointGeometryTests {
    private Point point;
    private EarthGeometry pointGeometry;

    @BeforeEach
    public void setUp() {
      point = geometryFactory.createPoint(new Coordinate(10.0, 20.0));
      pointGeometry = new EarthGeometry(point, wgs84);
    }

    @Test
    public void testGetCenterCoordinates() {
      assertEquals(BigDecimal.valueOf(10.0), pointGeometry.getCenterX(),
          "Center X should match the point's X coordinate");
      assertEquals(BigDecimal.valueOf(20.0), pointGeometry.getCenterY(),
          "Center Y should match the point's Y coordinate");
      assertEquals(wgs84, pointGeometry.getCrs(), "CRS should be WGS84");
    }

    @Test
    public void testPointIntersectsWithItself() {
      assertTrue(pointGeometry.intersects(
          BigDecimal.valueOf(10.0),
          BigDecimal.valueOf(20.0)
      ), "Point should intersect with itself");
    }

    @Test
    public void testPointDoesNotIntersectWithDistantPoint() {
      assertFalse(pointGeometry.intersects(
          BigDecimal.valueOf(40.0),
          BigDecimal.valueOf(30.0)
      ), "Point should not intersect with distant point");
    }

    @Test
    public void testPointIntersectsWithIdenticalPoint() {
      Point samePoint = geometryFactory.createPoint(new Coordinate(10.0, 20.0));
      EarthGeometry sameGeometry = new EarthGeometry(samePoint, wgs84);
      assertTrue(pointGeometry.intersects(sameGeometry), "Identical points should intersect");
    }
  }

  @Nested
  class PolygonGeometryTests {
    private Polygon rectangle;
    private EarthGeometry polygonGeometry;

    @BeforeEach
    public void setUp() {
      // Create a rectangular polygon from (10,20) to (12,22)
      Coordinate[] coords = new Coordinate[] {
          new Coordinate(10.0, 20.0),
          new Coordinate(12.0, 20.0),
          new Coordinate(12.0, 22.0),
          new Coordinate(10.0, 22.0),
          new Coordinate(10.0, 20.0)  // Close the polygon
      };
      rectangle = geometryFactory.createPolygon(coords);
      polygonGeometry = new EarthGeometry(rectangle, wgs84);
    }

    @Test
    public void testGetCenterCoordinates() {
      assertEquals(BigDecimal.valueOf(11.0), polygonGeometry.getCenterX(),
          "Center X should be the middle of min and max");
      assertEquals(BigDecimal.valueOf(21.0), polygonGeometry.getCenterY(),
          "Center Y should be the middle of min and max");
      assertEquals(wgs84, polygonGeometry.getCrs(), "CRS should be WGS84");
    }

    @Test
    public void testPolygonContainsPoint() {
      // Point inside polygon
      assertTrue(polygonGeometry.intersects(
          BigDecimal.valueOf(11.0),
          BigDecimal.valueOf(21.0)
      ), "Polygon should contain point inside its bounds");

      // Point on polygon boundary
      assertTrue(polygonGeometry.intersects(
          BigDecimal.valueOf(10.0),
          BigDecimal.valueOf(20.0)
      ), "Polygon should contain point on its boundary");

      // Point outside polygon
      assertFalse(polygonGeometry.intersects(
          BigDecimal.valueOf(40.0),
          BigDecimal.valueOf(30.0)
      ), "Polygon should not contain point outside its bounds");
    }

    @Test
    public void testPolygonIntersectsWithGeometry() {
      // Point inside polygon
      Point insidePoint = geometryFactory.createPoint(new Coordinate(11.0, 21.0));
      EarthGeometry insideGeometry = new EarthGeometry(insidePoint, wgs84);
      assertTrue(polygonGeometry.intersects(insideGeometry),
          "Polygon should intersect with point inside it");

      // Another overlapping polygon
      Coordinate[] coords = new Coordinate[] {
          new Coordinate(11.0, 21.0),
          new Coordinate(13.0, 21.0),
          new Coordinate(13.0, 23.0),
          new Coordinate(11.0, 23.0),
          new Coordinate(11.0, 21.0)  // Close the polygon
      };
      Polygon overlappingPoly = geometryFactory.createPolygon(coords);
      EarthGeometry overlappingGeometry = new EarthGeometry(overlappingPoly, wgs84);
      assertTrue(polygonGeometry.intersects(overlappingGeometry),
          "Polygon should intersect with overlapping polygon");

      // Non-overlapping polygon
      Coordinate[] coords2 = new Coordinate[] {
          new Coordinate(15.0, 25.0),
          new Coordinate(16.0, 25.0),
          new Coordinate(16.0, 26.0),
          new Coordinate(15.0, 26.0),
          new Coordinate(15.0, 25.0)  // Close the polygon
      };
      Polygon nonOverlappingPoly = geometryFactory.createPolygon(coords2);
      EarthGeometry nonOverlappingGeometry = new EarthGeometry(nonOverlappingPoly, wgs84);
      assertFalse(polygonGeometry.intersects(nonOverlappingGeometry),
          "Polygon should not intersect with non-overlapping polygon");
    }

    @Test
    public void testGetEnvelope() {
      var envelope = polygonGeometry.getEnvelope();

      assertEquals(10.0, envelope.getMinX(), 0.000001);
      assertEquals(12.0, envelope.getMaxX(), 0.000001);
      assertEquals(20.0, envelope.getMinY(), 0.000001);
      assertEquals(22.0, envelope.getMaxY(), 0.000001);
      assertEquals(wgs84, envelope.getCoordinateReferenceSystem(), "CRS should be WGS84");
    }

    @Test
    public void testGetConvexHull() {
      EarthGeometry hull = polygonGeometry.getConvexHull();

      assertNotNull(hull, "Convex hull should not be null");
      assertEquals(wgs84, hull.getCrs(), "Convex hull should have same CRS");
      // Rectangle is already convex
      assertTrue(hull.getInnerGeometry().equals(rectangle),
          "Convex hull of rectangle should be the same rectangle");
    }
  }

  @Nested
  class CrsTransformationTests {
    @Test
    public void testAsTargetCrs() throws FactoryException {
      EarthGeometry geometry = new EarthGeometry(defaultValidPoint, wgs84);

      EarthGeometry transformed = geometry.asTargetCrs(utm11n);

      assertNotNull(transformed, "Transformed EarthGeometry should not be null");
      assertEquals(utm11n, transformed.getCrs(),
          "Transformed EarthGeometry should have target CRS");
      assertFalse(defaultValidPoint.equals(transformed.getInnerGeometry()),
          "Transformed point should have different coordinates");
    }

    @Test
    public void testAsTargetCrsWithSameCrs() {
      Point point = geometryFactory.createPoint(new Coordinate(10.0, 20.0));
      EarthGeometry geometry = new EarthGeometry(point, wgs84);

      EarthGeometry transformed = geometry.asTargetCrs(wgs84);

      // Should return the same instance when target CRS is the same
      assertEquals(geometry, transformed, "Should return same instance when CRS is unchanged");
    }

    @Test
    public void testIntersectionWithDifferentCrs() throws FactoryException, TransformException {
      EarthGeometry wgs84Geometry = new EarthGeometry(defaultValidPoint, wgs84);

      // Create point in UTM11N that corresponds to same location
      MathTransform transform = CRS.findOperation(wgs84, utm11n, null).getMathTransform();
      DirectPosition2D srcPt = new DirectPosition2D(
          defaultValidCoordinate.x, defaultValidCoordinate.y);
      DirectPosition2D dstPt = new DirectPosition2D();
      transform.transform(srcPt, dstPt);

      Point utmPoint = geometryFactory.createPoint(new Coordinate(dstPt.getX(), dstPt.getY()));
      EarthGeometry utmGeometry = new EarthGeometry(utmPoint, utm11n);

      // They should intersect despite different CRS
      assertTrue(wgs84Geometry.intersects(utmGeometry),
          "Points at same location should intersect despite different CRS");
    }
  }

  @Nested
  class ConvexHullTests {
    @Test
    public void testGetConvexHullWithOtherGeometry() {
      // Create two points
      Point point1 = geometryFactory.createPoint(new Coordinate(10.0, 20.0));
      Point point2 = geometryFactory.createPoint(new Coordinate(30.0, 40.0));

      EarthGeometry geom1 = new EarthGeometry(point1, wgs84);
      EarthGeometry geom2 = new EarthGeometry(point2, wgs84);

      // Get convex hull of the two points
      EarthGeometry hull = geom1.getConvexHull(geom2);

      assertNotNull(hull, "Convex hull should not be null");
      assertEquals(wgs84, hull.getCrs(), "Convex hull should have same CRS");

      // The convex hull of two points should be a line
      assertEquals(2, hull.getInnerGeometry().getCoordinates().length,
          "Convex hull of two points should be a line with 2 coordinates");
    }

    @Test
    public void testGetConvexHullWithOtherGeometryDifferentCrs() throws FactoryException {
      // Create a point in WGS84
      Point wgs84Point = geometryFactory.createPoint(new Coordinate(10.0, 20.0));
      EarthGeometry wgs84Geometry = new EarthGeometry(wgs84Point, wgs84);

      // Create a point in UTM11N
      Point utmPoint = geometryFactory.createPoint(new Coordinate(500000.0, 4000000.0));
      EarthGeometry utmGeometry = new EarthGeometry(utmPoint, utm11n);

      // Get convex hull - should transform to same CRS first
      EarthGeometry hull = wgs84Geometry.getConvexHull(utmGeometry);

      assertNotNull(hull, "Convex hull should not be null");
      assertEquals(wgs84, hull.getCrs(), "Convex hull should use the first geometry's CRS");
    }

    @Test
    public void testGetConvexHullWithMultipleGeometries() {
      // Create multiple points forming a polygon
      Point point1 = geometryFactory.createPoint(new Coordinate(10.0, 10.0));
      Point point2 = geometryFactory.createPoint(new Coordinate(10.0, 20.0));
      Point point3 = geometryFactory.createPoint(new Coordinate(20.0, 20.0));
      Point point4 = geometryFactory.createPoint(new Coordinate(20.0, 10.0));

      EarthGeometry geom1 = new EarthGeometry(point1, wgs84);
      EarthGeometry geom2 = new EarthGeometry(point2, wgs84);
      EarthGeometry geom3 = new EarthGeometry(point3, wgs84);
      EarthGeometry geom4 = new EarthGeometry(point4, wgs84);

      // Get convex hull of all points
      EarthGeometry hull = geom1
          .getConvexHull(geom2)
          .getConvexHull(geom3)
          .getConvexHull(geom4);

      assertNotNull(hull, "Convex hull should not be null");
      assertEquals(5, hull.getInnerGeometry().getCoordinates().length,
          "Convex hull should form a closed poly with 5 coordinates (first last are the same)");
    }
  }

  @Nested
  class GridCrsConversionTests {
    @Test
    public void testGridCrsCreation() throws FactoryException {
      // Define extents
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

      assertNotNull(definition, "Grid CRS definition should be created");
      assertEquals("TestGrid", definition.getName(), "Grid name should match");
      assertEquals("EPSG:4326", definition.getBaseCrsCode(), "Base CRS code should match");
      assertEquals(cellSize, definition.getCellSize(), "Cell size should match");
    }

    @Test
    public void testGridToCrsCoordinatesConversion() {
      // Define extents based on WGS84
      BigDecimal topLeftX = new BigDecimal("-116.0");
      BigDecimal topLeftY = new BigDecimal("35.0");
      BigDecimal bottomRightX = new BigDecimal("-115.0");
      BigDecimal bottomRightY = new BigDecimal("34.0");
      BigDecimal cellSize = new BigDecimal("0.001"); // Small cell size for WGS84

      // Create grid CRS definition
      GridCrsDefinition definition = new GridCrsDefinition(
          "TestGrid",
          "EPSG:4326",
          new PatchBuilderExtents(topLeftX, topLeftY, bottomRightX, bottomRightY),
          cellSize,
          "degrees");

      // Test converting from grid to CRS coordinates
      BigDecimal gridX = BigDecimal.ZERO;
      BigDecimal gridY = BigDecimal.ZERO;

      BigDecimal[] crsCoords = definition.gridToCrsCoordinates(gridX, gridY);

      // Should return top-left corner - using compareTo for BigDecimal comparison
      assertTrue(topLeftX.compareTo(crsCoords[0].setScale(4, RoundingMode.HALF_UP)) == 0, 
          "X coordinate should match top-left");
      assertTrue(topLeftY.compareTo(crsCoords[1].setScale(4, RoundingMode.HALF_UP)) == 0,
          "Y coordinate should match top-left");
    }

    @Test
    public void testCrsToGridCoordinatesConversion() {
      // Define extents based on WGS84
      BigDecimal topLeftX = new BigDecimal("-116.0");
      BigDecimal topLeftY = new BigDecimal("35.0");
      BigDecimal bottomRightX = new BigDecimal("-115.0");
      BigDecimal bottomRightY = new BigDecimal("34.0");
      BigDecimal cellSize = new BigDecimal("0.001"); // Small cell size for WGS84

      // Create grid CRS definition
      GridCrsDefinition definition = new GridCrsDefinition(
          "TestGrid",
          "EPSG:4326",
          new PatchBuilderExtents(topLeftX, topLeftY, bottomRightX, bottomRightY),
          cellSize,
          "degrees");

      // Test converting from CRS to grid coordinates
      BigDecimal[] gridCoords = definition.crsToGridCoordinates(topLeftX, topLeftY);

      // Should return origin cell (0,0) - using compareTo with appropriate scale
      assertTrue(BigDecimal.ZERO.compareTo(gridCoords[0].setScale(4, RoundingMode.HALF_UP)) == 0, 
          "Grid X should be 0");
      assertTrue(BigDecimal.ZERO.compareTo(gridCoords[1].setScale(4, RoundingMode.HALF_UP)) == 0,
          "Grid Y should be 0");
    }
  }
}
