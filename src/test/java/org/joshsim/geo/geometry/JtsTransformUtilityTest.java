package org.joshsim.geo.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.apache.sis.referencing.CRS;
import org.joshsim.engine.geometry.grid.GridShape;
import org.joshsim.engine.geometry.grid.GridShapeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Unit tests for {@link JtsTransformUtility}.
 */
public class JtsTransformUtilityTest {

  @Mock private GridShape mockPointShape;
  @Mock private GridShape mockCircleShape;
  @Mock private GridShape mockSquareShape;
  @Mock private GridShape mockUnsupportedShape;
  @Mock private MathTransform mockTransform;

  private GeometryFactory geometryFactory;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));

    // Configure mock point shape
    when(mockPointShape.getGridShapeType()).thenReturn(GridShapeType.POINT);
    when(mockPointShape.getCenterX()).thenReturn(new BigDecimal("10.0"));
    when(mockPointShape.getCenterY()).thenReturn(new BigDecimal("20.0"));

    // Configure mock circle shape
    when(mockCircleShape.getGridShapeType()).thenReturn(GridShapeType.CIRCLE);
    when(mockCircleShape.getCenterX()).thenReturn(new BigDecimal("15.0"));
    when(mockCircleShape.getCenterY()).thenReturn(new BigDecimal("25.0"));
    when(mockCircleShape.getWidth()).thenReturn(new BigDecimal("10.0"));

    // Configure mock square shape
    when(mockSquareShape.getGridShapeType()).thenReturn(GridShapeType.SQUARE);
    when(mockSquareShape.getCenterX()).thenReturn(new BigDecimal("30.0"));
    when(mockSquareShape.getCenterY()).thenReturn(new BigDecimal("40.0"));
    when(mockSquareShape.getWidth()).thenReturn(new BigDecimal("20.0"));

    // Configure unsupported shape
    when(mockUnsupportedShape.getGridShapeType()).thenReturn(null);

    // Configure the mock MathTransform to add 100 to x and 200 to y
    doAnswer(invocation -> {
      double[] source = invocation.getArgument(0);
      int sourceOffset = invocation.getArgument(1);
      double[] target = invocation.getArgument(2);
      int targetOffset = invocation.getArgument(3);
      int numPoints = invocation.getArgument(4);
      
      for (int i = 0; i < numPoints; i++) {
        target[targetOffset + 2 * i] = source[sourceOffset + 2 * i] + 100;
        target[targetOffset + 2 * i + 1] = source[sourceOffset + 2 * i + 1] + 200;
      }
      return null;
    }).when(mockTransform).transform(any(double[].class), anyInt(), any(double[].class), 
        anyInt(), anyInt()
    );
  }

  @Test
  public void gridShapeToJts_point_returnsCorrectPoint() {
    Geometry result = JtsTransformUtility.gridShapeToJts(mockPointShape);
    
    assertTrue(result instanceof Point);
    Point point = (Point) result;
    assertEquals(10.0, point.getX(), 0.001);
    assertEquals(20.0, point.getY(), 0.001);
  }

  @Test
  public void gridShapeToJts_circle_returnsPolygon() {
    Geometry result = JtsTransformUtility.gridShapeToJts(mockCircleShape);
    
    assertTrue(result instanceof Polygon);
    Polygon polygon = (Polygon) result;
    assertTrue(polygon.isValid());
    
    // Check center point is approximately correct
    Coordinate centroid = polygon.getCentroid().getCoordinate();
    assertEquals(15.0, centroid.x, 0.1);
    assertEquals(25.0, centroid.y, 0.1);
  }

  @Test
  public void gridShapeToJts_square_returnsPolygon() {
    Geometry result = JtsTransformUtility.gridShapeToJts(mockSquareShape);
    
    assertTrue(result instanceof Polygon);
    Polygon polygon = (Polygon) result;
    assertTrue(polygon.isValid());
    
    // Check center point is approximately correct
    Coordinate centroid = polygon.getCentroid().getCoordinate();
    assertEquals(30.0, centroid.x, 0.1);
    assertEquals(40.0, centroid.y, 0.1);
  }

  @Test
  public void createJtsPoint_returnsPointWithCorrectCoordinates() {
    Point point = JtsTransformUtility.createJtsPoint(42.0, 84.0);
    
    assertNotNull(point);
    assertEquals(42.0, point.getX(), 0.001);
    assertEquals(84.0, point.getY(), 0.001);
  }

  @Test
  public void createJtsCircle_returnsValidPolygon() {
    double centerX = 100.0;
    double centerY = 200.0;
    double radius = 50.0;
    
    Polygon circle = JtsTransformUtility.createJtsCircle(centerX, centerY, radius);
    
    assertNotNull(circle);
    assertTrue(circle.isValid());
    
    // Check center point is approximately correct
    Coordinate centroid = circle.getCentroid().getCoordinate();
    assertEquals(centerX, centroid.x, 0.1);
    assertEquals(centerY, centroid.y, 0.1);
  }

  @Test
  public void createJtsRectangle_returnsValidPolygon() {
    double centerX = 200.0;
    double centerY = 300.0;
    double width = 100.0;
    double height = 50.0;
    
    Polygon rectangle = JtsTransformUtility.createJtsRectangle(centerX, centerY, width, height);
    
    assertNotNull(rectangle);
    assertTrue(rectangle.isValid());
    
    // Check center point is approximately correct
    Coordinate centroid = rectangle.getCentroid().getCoordinate();
    assertEquals(centerX, centroid.x, 0.1);
    assertEquals(centerY, centroid.y, 0.1);
  }

  @Test
  public void transform_point_transformsCorrectly() throws TransformException {
    Point point = geometryFactory.createPoint(new Coordinate(0.0, 0.0));
    
    Geometry result = JtsTransformUtility.transform(point, mockTransform);
    
    assertTrue(result instanceof Point);
    Point transformedPoint = (Point) result;
    assertEquals(100.0, transformedPoint.getX(), 0.001);
    assertEquals(200.0, transformedPoint.getY(), 0.001);
  }

  @Test
  public void transform_lineString_transformsCorrectly() throws TransformException {
    Coordinate[] coords = new Coordinate[] {
        new Coordinate(0.0, 0.0),
        new Coordinate(10.0, 10.0)
    };
    LineString line = geometryFactory.createLineString(coords);
    
    Geometry result = JtsTransformUtility.transform(line, mockTransform);
    
    assertTrue(result instanceof LineString);
    LineString transformedLine = (LineString) result;
    assertEquals(100.0, transformedLine.getCoordinateN(0).x, 0.001);
    assertEquals(200.0, transformedLine.getCoordinateN(0).y, 0.001);
    assertEquals(110.0, transformedLine.getCoordinateN(1).x, 0.001);
    assertEquals(210.0, transformedLine.getCoordinateN(1).y, 0.001);
  }

  @Test
  public void transform_polygon_transformsCorrectly() throws TransformException {
    // Create a simple polygon (square)
    Coordinate[] coords = new Coordinate[] {
        new Coordinate(0.0, 0.0),
        new Coordinate(10.0, 0.0),
        new Coordinate(10.0, 10.0),
        new Coordinate(0.0, 10.0),
        new Coordinate(0.0, 0.0) // Closing point
    };
    LinearRing ring = geometryFactory.createLinearRing(coords);
    Polygon poly = geometryFactory.createPolygon(ring);
    
    Geometry result = JtsTransformUtility.transform(poly, mockTransform);
    
    assertTrue(result instanceof Polygon);
    Polygon transformedPoly = (Polygon) result;
    
    // Check exterior ring coordinates
    LinearRing exterior = transformedPoly.getExteriorRing();
    assertEquals(5, exterior.getNumPoints());
    assertEquals(100.0, exterior.getCoordinateN(0).x, 0.001);
    assertEquals(200.0, exterior.getCoordinateN(0).y, 0.001);
    assertEquals(110.0, exterior.getCoordinateN(1).x, 0.001);
    assertEquals(200.0, exterior.getCoordinateN(1).y, 0.001);
  }
  
  @Test
  public void transform_polygonWithHole_transformsCorrectly() throws TransformException {
    // Create exterior ring
    Coordinate[] exteriorCoords = new Coordinate[] {
        new Coordinate(0.0, 0.0),
        new Coordinate(20.0, 0.0),
        new Coordinate(20.0, 20.0),
        new Coordinate(0.0, 20.0),
        new Coordinate(0.0, 0.0) // Closing point
    };
    LinearRing exterior = geometryFactory.createLinearRing(exteriorCoords);
    
    // Create interior ring (hole)
    Coordinate[] interiorCoords = new Coordinate[] {
        new Coordinate(5.0, 5.0),
        new Coordinate(15.0, 5.0),
        new Coordinate(15.0, 15.0),
        new Coordinate(5.0, 15.0),
        new Coordinate(5.0, 5.0) // Closing point
    };
    LinearRing interior = geometryFactory.createLinearRing(interiorCoords);
    
    // Create polygon with hole
    Polygon poly = geometryFactory.createPolygon(exterior, new LinearRing[] { interior });
    
    Geometry result = JtsTransformUtility.transform(poly, mockTransform);
    
    assertTrue(result instanceof Polygon);
    Polygon transformedPoly = (Polygon) result;
    assertEquals(1, transformedPoly.getNumInteriorRing());
    
    // Check one coordinate from hole to verify it was transformed
    LinearRing transformedHole = transformedPoly.getInteriorRingN(0);
    assertEquals(105.0, transformedHole.getCoordinateN(0).x, 0.001);
    assertEquals(205.0, transformedHole.getCoordinateN(0).y, 0.001);
  }

  @Test
  public void getRightHandedCrs_validCrsCode_returnsValidCrs() throws FactoryException {
    CoordinateReferenceSystem crs = JtsTransformUtility.getRightHandedCrs("EPSG:4326");
    
    assertNotNull(crs);
    assertEquals("WGS 84", crs.getName().toString());
  }

  @Test
  public void getRightHandedCrs_invalidCrsCode_throwsException() {
    assertThrows(FactoryException.class, () -> {
      JtsTransformUtility.getRightHandedCrs("INVALID:CODE");
    });
  }

  @Test
  public void getRightHandedCrs_fromExistingCrs_returnsRightHandedCrs() throws FactoryException {
    CoordinateReferenceSystem originalCrs = CRS.forCode("EPSG:4326");
    CoordinateReferenceSystem rightHandedCrs = JtsTransformUtility.getRightHandedCrs(originalCrs);
    
    assertNotNull(rightHandedCrs);
  }
}