package org.joshsim.engine.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.apache.sis.referencing.CRS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.shape.Circle;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.SpatialRelation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;


/**
 * Tests for the Geometry class, organized by shape type.
 */
public class GeometryTest {

  private SpatialContext ctx;
  private CoordinateReferenceSystem wgs84;
  private CoordinateReferenceSystem utm11n;

  /**
   * Create a GEO context for all tests.
   */
  @BeforeEach
  public void setUp() throws FactoryException {
    ctx = SpatialContext.GEO;
    wgs84 = CRS.forCode("EPSG:4326"); // WGS84
    utm11n = CRS.forCode("EPSG:32611"); // UTM Zone 11N
  }

  @Test
  public void testConstructor() {
    Shape mockShape = mock(Shape.class);
    SpatialContext mockContext = mock(SpatialContext.class);
    when(mockShape.getContext()).thenReturn(mockContext);

    Geometry geometry = new Geometry(mockShape, wgs84);

    assertNotNull(geometry, "Geometry should be initialized");
    assertEquals(mockShape, geometry.shape, "Shape should be set in constructor");
    assertEquals(
        mockContext,
        geometry.getSpatialContext(),
        "SpatialContext should come from the shape"
    );
    assertEquals(wgs84, geometry.getCrs(), "CRS should be set in constructor");
  }

  @Test
  public void testWithNullShape() {
    Geometry nullGeometry = new Geometry(null, wgs84);

    // Test getSpatialContext with null shape
    Exception exception = assertThrows(NullPointerException.class, () -> {
      nullGeometry.getSpatialContext();
    });

    // Test intersects with coordinates
    Exception exception1 = assertThrows(IllegalStateException.class, () -> {
      nullGeometry.intersects(BigDecimal.valueOf(10.0), BigDecimal.valueOf(20.0));
    });
    assertEquals("Shape not initialized", exception1.getMessage());

    // Test intersects with geometry
    Shape mockShape = mock(Shape.class);
    Geometry otherGeometry = new Geometry(mockShape, wgs84);
    Exception exception2 = assertThrows(IllegalStateException.class, () -> {
      nullGeometry.intersects(otherGeometry);
    });
    assertEquals("Shape not initialized", exception2.getMessage());
  }

  @Test
  public void testWithNullCrs() {
    Shape mockShape = mock(Shape.class);
    
    Exception exception = assertThrows(NullPointerException.class, () -> {
      new Geometry(mockShape, null);
    });
  }

  @Nested
  class PointGeometryTests {
    private Point point;
    private Geometry pointGeometry;

    @BeforeEach
    public void setUp() {
      point = ctx.getShapeFactory().pointXY(10.0, 20.0);
      pointGeometry = new Geometry(point, wgs84);
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
      Point samePoint = ctx.getShapeFactory().pointXY(10.0, 20.0);
      Geometry sameGeometry = new Geometry(samePoint, wgs84);
      assertTrue(pointGeometry.intersects(sameGeometry), "Identical points should intersect");
    }

    @Test
    public void testPointDistanceCalculation() {
      Point otherPoint = ctx.getShapeFactory().pointXY(11.0, 20.0);
      Geometry otherGeometry = new Geometry(otherPoint, wgs84);

      double expectedDistance = ctx.calcDistance(point, otherPoint);
      BigDecimal expected = new BigDecimal(expectedDistance);

      BigDecimal actual = pointGeometry.centerDistanceTo(otherGeometry);
      assertEquals(expected, actual, "Distance calculation should match spatial4j's result");
    }
  }

  @Nested
  class RectangleGeometryTests {
    private Rectangle rectangle;
    private Geometry rectangleGeometry;

    @BeforeEach
    public void setUp() {
      // Create rectangle from (10,20) to (12,22)
      rectangle = ctx.getShapeFactory().rect(10.0, 12.0, 20.0, 22.0);
      rectangleGeometry = new Geometry(rectangle, wgs84);
    }

    @Test
    public void testGetCenterCoordinates() {
      assertEquals(BigDecimal.valueOf(11.0), rectangleGeometry.getCenterX(),
          "Center X should be the middle of min and max");
      assertEquals(BigDecimal.valueOf(21.0), rectangleGeometry.getCenterY(),
          "Center Y should be the middle of min and max");
      assertEquals(wgs84, rectangleGeometry.getCrs(), "CRS should be WGS84");
    }

    @Test
    public void testRectangleContainsPoint() {
      // Point inside rectangle
      assertTrue(rectangleGeometry.intersects(
          BigDecimal.valueOf(11.0),
          BigDecimal.valueOf(21.0)
      ), "Rectangle should contain point inside its bounds");

      // Point on rectangle boundary
      assertTrue(rectangleGeometry.intersects(
          BigDecimal.valueOf(10.0),
          BigDecimal.valueOf(20.0)
      ), "Rectangle should contain point on its boundary");

      // Point outside rectangle
      assertFalse(rectangleGeometry.intersects(
          BigDecimal.valueOf(40.0),
          BigDecimal.valueOf(30.0)
      ), "Rectangle should not contain point outside its bounds");
    }

    @Test
    public void testRectangleIntersectsWithGeometry() {
      // Point inside rectangle
      Point insidePoint = ctx.getShapeFactory().pointXY(11.0, 21.0);
      Geometry insideGeometry = new Geometry(insidePoint, wgs84);
      assertTrue(rectangleGeometry.intersects(insideGeometry),
          "Rectangle should intersect with point inside it");

      // Another rectangle that overlaps
      Rectangle overlappingRect = ctx.getShapeFactory().rect(11.0, 13.0, 21.0, 23.0);
      Geometry overlappingGeometry = new Geometry(overlappingRect, wgs84);
      assertTrue(rectangleGeometry.intersects(overlappingGeometry),
          "Rectangle should intersect with overlapping rectangle");

      // Non-overlapping rectangle
      Rectangle nonOverlappingRect = ctx.getShapeFactory().rect(15.0, 16.0, 25.0, 26.0);
      Geometry nonOverlappingGeometry = new Geometry(nonOverlappingRect, wgs84);
      assertFalse(rectangleGeometry.intersects(nonOverlappingGeometry),
          "Rectangle should not intersect with non-overlapping rectangle");
    }
  }

  @Nested
  class CircleGeometryTests {
    private Circle circle;
    private Geometry circleGeometry;

    @BeforeEach
    public void setUp() {
      // Create circle at (10,20) with radius of 1 degree
      circle = ctx.getShapeFactory().circle(10.0, 20.0, 1.0);
      circleGeometry = new Geometry(circle, wgs84);
    }

    @Test
    public void testGetCenterCoordinates() {
      assertEquals(BigDecimal.valueOf(10.0), circleGeometry.getCenterX(),
          "Center X should match the circle's center X");
      assertEquals(BigDecimal.valueOf(20.0), circleGeometry.getCenterY(),
          "Center Y should match the circle's center Y");
      assertEquals(wgs84, circleGeometry.getCrs(), "CRS should be WGS84");
    }

    @Test
    public void testCircleContainsPoint() {
      // Point at center of circle
      assertTrue(circleGeometry.intersects(
          BigDecimal.valueOf(10.0),
          BigDecimal.valueOf(20.0)
      ), "Circle should contain its center point");

      // Point inside circle
      assertTrue(circleGeometry.intersects(
          BigDecimal.valueOf(10.5),
          BigDecimal.valueOf(20.5)
      ), "Circle should contain point inside its radius");

      // Point outside circle
      assertFalse(circleGeometry.intersects(
          BigDecimal.valueOf(15.0),
          BigDecimal.valueOf(25.0)
      ), "Circle should not contain point outside its radius");
    }

    @Test
    public void testCircleIntersectsWithGeometry() {
      // Point inside circle
      Point insidePoint = ctx.getShapeFactory().pointXY(10.5, 20.5);
      Geometry insideGeometry = new Geometry(insidePoint, wgs84);
      assertTrue(circleGeometry.intersects(insideGeometry),
          "Circle should intersect with point inside it");

      // Another circle that overlaps
      Circle overlappingCircle = ctx.getShapeFactory().circle(10.5, 20.5, 1.0);
      Geometry overlappingGeometry = new Geometry(overlappingCircle, wgs84);
      assertTrue(circleGeometry.intersects(overlappingGeometry),
          "Circle should intersect with overlapping circle");

      // Non-overlapping circle
      Circle nonOverlappingCircle = ctx.getShapeFactory().circle(15.0, 25.0, 1.0);
      Geometry nonOverlappingGeometry = new Geometry(nonOverlappingCircle, wgs84);
      assertFalse(circleGeometry.intersects(nonOverlappingGeometry),
          "Circle should not intersect with non-overlapping circle");
    }
  }

  @Nested
  class MockedShapeTests {
    @Test
    public void testWithMockedObjects() {
      // Setup mocked objects
      Shape mockedShape = mock(Shape.class);
      Point mockedCenter = mock(Point.class);
      when(mockedShape.getCenter()).thenReturn(mockedCenter);
      when(mockedCenter.getX()).thenReturn(15.5);
      when(mockedCenter.getY()).thenReturn(25.5);

      // Create test geometry with mocked shape
      Geometry mockedGeometry = new Geometry(mockedShape, wgs84);

      // Test center coordinates
      assertEquals(BigDecimal.valueOf(15.5), mockedGeometry.getCenterX());
      assertEquals(BigDecimal.valueOf(25.5), mockedGeometry.getCenterY());
      assertEquals(wgs84, mockedGeometry.getCrs(), "CRS should be WGS84");

      // Verify shape.getCenter() was called
      verify(mockedShape, times(2)).getCenter();
    }

    @Test
    public void testIntersectsWithMockedRelation() {
      // Setup mocked objects for testing intersection logic
      Shape mockedShape1 = mock(Shape.class);
      Shape mockedShape2 = mock(Shape.class);
      SpatialRelation mockedRelation = mock(SpatialRelation.class);

      when(mockedShape1.relate(any(Shape.class))).thenReturn(mockedRelation);
      when(mockedRelation.intersects()).thenReturn(true);

      Geometry geom1 = new Geometry(mockedShape1, wgs84);
      Geometry geom2 = new Geometry(mockedShape2, wgs84);

      assertTrue(geom1.intersects(geom2));
      verify(mockedShape1).relate(mockedShape2);
      verify(mockedRelation).intersects();
    }
    
    @Test
    public void testWithDifferentCrs() {
      Shape mockedShape1 = mock(Shape.class);
      Shape mockedShape2 = mock(Shape.class);
      
      Geometry geom1 = new Geometry(mockedShape1, wgs84);
      Geometry geom2 = new Geometry(mockedShape2, utm11n);
      
      assertEquals(wgs84, geom1.getCrs(), "First geometry should have WGS84 CRS");
      assertEquals(utm11n, geom2.getCrs(), "Second geometry should have UTM11N CRS");
    }
  }
  
  @Nested
  class EnvelopeTests {
    @Test
    public void testGetEnvelope() {
      Rectangle rect = ctx.getShapeFactory().rect(10.0, 12.0, 20.0, 22.0);
      Geometry rectGeometry = new Geometry(rect, wgs84);
      
      var envelope = rectGeometry.getEnvelope();
      
      assertEquals(10.0, envelope.getLower(0), 0.000001);
      assertEquals(12.0, envelope.getUpper(0), 0.000001);
      assertEquals(20.0, envelope.getLower(1), 0.000001);
      assertEquals(22.0, envelope.getUpper(1), 0.000001);
    }
  }
}