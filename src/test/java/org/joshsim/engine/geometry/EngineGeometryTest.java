package org.joshsim.engine.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Tests for the EngineGeometry class, organized by EngineGeometry type.
 */
public class EngineGeometryTest {

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
    wgs84 = CRS.decode("EPSG:4326", true); // WGS84, lefthanded (lon first)
    utm11n = CRS.decode("EPSG:32611"); // UTM Zone 11N

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
    EngineGeometry geometry = new EngineGeometry(point, wgs84);

    assertNotNull(geometry, "EngineGeometry should be initialized");
    assertEquals(
        point,
        geometry.getInnerGeometry(),
        "Inner EngineGeometry should be set correctly"
    );
    assertEquals(wgs84, geometry.getCrs(), "CRS should be set correctly");
  }

  @Test
  public void testConstructorWithTransformers() {
    Point point = geometryFactory.createPoint(new Coordinate(10.0, 20.0));
    Optional<Map<CoordinateReferenceSystem, MathTransform>> transformers =
        Optional.of(new HashMap<>());
    EngineGeometry geometry = new EngineGeometry(point, wgs84, transformers);

    assertNotNull(geometry, "EngineGeometry should be initialized");
    assertEquals(
        point,
        geometry.getInnerGeometry(),
        "Inner EngineGeometry should be set correctly"
    );
    assertEquals(wgs84, geometry.getCrs(), "CRS should be set correctly");
  }

  @Test
  public void testWithNullGeometry() {
    Exception exception = assertThrows(NullPointerException.class, () -> {
      new EngineGeometry(null, wgs84);
    });
  }

  @Test
  public void testWithNullCrs() {
    Point point = geometryFactory.createPoint(new Coordinate(10.0, 20.0));
    Exception exception = assertThrows(NullPointerException.class, () -> {
      new EngineGeometry(point, null);
    });
  }

  @Nested
  class PointGeometryTests {
    private Point point;
    private EngineGeometry pointGeometry;

    @BeforeEach
    public void setUp() {
      point = geometryFactory.createPoint(new Coordinate(10.0, 20.0));
      pointGeometry = new EngineGeometry(point, wgs84);
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
      EngineGeometry sameGeometry = new EngineGeometry(samePoint, wgs84);
      assertTrue(pointGeometry.intersects(sameGeometry), "Identical points should intersect");
    }
  }

  @Nested
  class PolygonGeometryTests {
    private Polygon rectangle;
    private EngineGeometry polygonGeometry;

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
      polygonGeometry = new EngineGeometry(rectangle, wgs84);
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
      EngineGeometry insideGeometry = new EngineGeometry(insidePoint, wgs84);
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
      EngineGeometry overlappingGeometry = new EngineGeometry(overlappingPoly, wgs84);
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
      EngineGeometry nonOverlappingGeometry = new EngineGeometry(nonOverlappingPoly, wgs84);
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
      EngineGeometry hull = polygonGeometry.getConvexHull();

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
      EngineGeometry geometry = new EngineGeometry(defaultValidPoint, wgs84);

      EngineGeometry transformed = geometry.asTargetCrs(utm11n);

      assertNotNull(transformed, "Transformed EngineGeometry should not be null");
      assertEquals(utm11n, transformed.getCrs(),
          "Transformed EngineGeometry should have target CRS");
      assertFalse(defaultValidPoint.equals(transformed.getInnerGeometry()),
          "Transformed point should have different coordinates");
    }

    @Test
    public void testAsTargetCrsWithSameCrs() {
      Point point = geometryFactory.createPoint(new Coordinate(10.0, 20.0));
      EngineGeometry geometry = new EngineGeometry(point, wgs84);

      EngineGeometry transformed = geometry.asTargetCrs(wgs84);

      // Should return the same instance when target CRS is the same
      assertEquals(geometry, transformed, "Should return same instance when CRS is unchanged");
    }

    @Test
    public void testIntersectionWithDifferentCrs() throws FactoryException, TransformException {
      EngineGeometry wgs84Geometry = new EngineGeometry(defaultValidPoint, wgs84);

      // Create point in UTM11N that corresponds to same location
      MathTransform transform = CRS.findMathTransform(wgs84, utm11n, true);
      Geometry utmPoint = JTS.transform(defaultValidPoint, transform);
      EngineGeometry utmGeometry = new EngineGeometry(utmPoint, utm11n);

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

      EngineGeometry geom1 = new EngineGeometry(point1, wgs84);
      EngineGeometry geom2 = new EngineGeometry(point2, wgs84);

      // Get convex hull of the two points
      EngineGeometry hull = geom1.getConvexHull(geom2);

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
      EngineGeometry wgs84Geometry = new EngineGeometry(wgs84Point, wgs84);

      // Create a point in UTM11N
      Point utmPoint = geometryFactory.createPoint(new Coordinate(500000.0, 4000000.0));
      EngineGeometry utmGeometry = new EngineGeometry(utmPoint, utm11n);

      // Get convex hull - should transform to same CRS first
      EngineGeometry hull = wgs84Geometry.getConvexHull(utmGeometry);

      assertNotNull(hull, "Convex hull should not be null");
      assertEquals(wgs84, hull.getCrs(), "Convex hull should use the first geometry's CRS");
    }
  }
}
