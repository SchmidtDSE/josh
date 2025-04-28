package org.joshsim.geo.geometry;

import java.math.BigDecimal;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.joshsim.engine.geometry.grid.GridShape;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Utility methods for transforming JTS geometries.
 */
public final class JtsTransformUtility {

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(
      new PrecisionModel(PrecisionModel.FLOATING)
  );

  private static final GeometricShapeFactory JTS_SHAPE_FACTORY =
      new GeometricShapeFactory(GEOMETRY_FACTORY);

  private static final int DEFAULT_CIRCLE_POINTS = 32;

  private JtsTransformUtility() {
    // Private constructor to prevent instantiation
  }

  /**
   * Creates a JTS Geometry object from a grid shape without transformation.
   *
   * @param gridShape The grid shape to convert
   * @return A JTS Geometry in grid coordinates
   */
  public static Geometry gridShapeToJts(GridShape gridShape) {
    switch (gridShape.getGridShapeType()) {
      case POINT:
        return createJtsPoint(
            gridShape.getCenterX().doubleValue(),
            gridShape.getCenterY().doubleValue()
        );

      case CIRCLE:
        return createJtsCircle(
            gridShape.getCenterX().doubleValue(),
            gridShape.getCenterY().doubleValue(),
            gridShape.getWidth().divide(new BigDecimal(2)).doubleValue()
        );

      case SQUARE:
        return createJtsRectangle(
            gridShape.getCenterX().doubleValue(),
            gridShape.getCenterY().doubleValue(),
            gridShape.getWidth().doubleValue(),
            gridShape.getWidth().doubleValue()
        );

      default:
        throw new UnsupportedOperationException(
            "Unsupported grid shape type: " + gridShape.getGridShapeType());
    }
  }

  // /**
  //  * Converts a grid EngineGeometry to JTS Geometry. This is necessary because, by design,
  //  * 'Grid Space' and the geometries within don't leverage any spatial libraries - they are
  //  * constructed from scratch and the description of how to convert them to 'Earth Space' is
  //  * contained within the `GridCrsDefinition` and the instantiated `GridCrsManager`.
  //  *
  //  * @param gridGeometry The grid geometry to convert
  //  * @return Equivalent JTS geometry
  //  */
  // public static Geometry gridGeometryToJts(EngineGeometry gridGeometry) {
  //   if (gridGeometry instanceof GridPoint) {
  //     GridPoint point = (GridPoint) gridGeometry;
  //     return createJtsPoint(
  //         point.getCenterX().doubleValue(),
  //         point.getCenterY().doubleValue()
  //     );
  //   } else if (gridGeometry instanceof GridCircle) {
  //     GridCircle circle = (GridCircle) gridGeometry;
  //     return createJtsCircle(
  //         circle.getCenterX().doubleValue(),
  //         circle.getCenterY().doubleValue(),
  //         circle.getWidth().divide(new BigDecimal(2)).doubleValue()
  //     );
  //   } else if (gridGeometry instanceof GridSquare) {
  //     GridSquare square = (GridSquare) gridGeometry;
  //     return createJtsRectangle(
  //         square.getCenterX().doubleValue(),
  //         square.getCenterY().doubleValue(),
  //         square.getWidth().doubleValue(),
  //         square.getWidth().doubleValue()
  //     );
  //   }

  //   throw new UnsupportedOperationException(
  //       "Cannot convert grid geometry type: " + gridGeometry.getClass().getSimpleName());
  // }

  /**
   * Creates a JTS Point from coordinates.
   *
   * @param x The x coordinate
   * @param y The y coordinate
   * @return A JTS Point
   */
  public static Point createJtsPoint(double x, double y) {
    return GEOMETRY_FACTORY.createPoint(new Coordinate(x, y));
  }

  /**
   * Creates a JTS Circle approximation (polygon).
   *
   * @param centerX The center x coordinate
   * @param centerY The center y coordinate
   * @param radius The circle radius
   * @return A JTS Polygon approximating a circle
   */
  public static Polygon createJtsCircle(double centerX, double centerY, double radius) {
    synchronized (JTS_SHAPE_FACTORY) {
      JTS_SHAPE_FACTORY.setCentre(new Coordinate(centerX, centerY));
      JTS_SHAPE_FACTORY.setWidth(radius * 2);
      JTS_SHAPE_FACTORY.setHeight(radius * 2);
      JTS_SHAPE_FACTORY.setNumPoints(DEFAULT_CIRCLE_POINTS);
      return (Polygon) JTS_SHAPE_FACTORY.createCircle();
    }
  }

  /**
   * Creates a JTS Rectangle.
   *
   * @param centerX The center x coordinate
   * @param centerY The center y coordinate
   * @param width The rectangle width
   * @param height The rectangle height
   * @return A JTS Polygon rectangle
   */
  public static Polygon createJtsRectangle(
        double centerX, double centerY, double width, double height) {
    synchronized (JTS_SHAPE_FACTORY) {
      JTS_SHAPE_FACTORY.setCentre(new Coordinate(centerX, centerY));
      JTS_SHAPE_FACTORY.setWidth(width);
      JTS_SHAPE_FACTORY.setHeight(height);
      return (Polygon) JTS_SHAPE_FACTORY.createRectangle();
    }
  }

  /**
   * Transforms a JTS geometry using the specified math transform, by
   * converting the geometry's coordinates to the new coordinate system.
   *
   * @param geometry The geometry to transform
   * @param transform The transform to apply
   * @return The transformed geometry
   * @throws TransformException If the transformation fails
   */
  public static Geometry transform(Geometry geometry, MathTransform transform)
      throws TransformException {
    // Special case for Point geometries
    if (geometry.getGeometryType().equals("Point")) {
      Coordinate coord = geometry.getCoordinate();
      double[] srcPt = new double[] {coord.x, coord.y};
      double[] dstPt = new double[2];
      transform.transform(srcPt, 0, dstPt, 0, 1);
      return GEOMETRY_FACTORY.createPoint(new Coordinate(dstPt[0], dstPt[1]));
    }

    // Handle complex geometries
    Coordinate[] transformedCoords = transformCoordinates(geometry.getCoordinates(), transform);

    // Create appropriate geometry type based on original
    if (geometry instanceof Polygon) {
      Polygon poly = (Polygon) geometry;

      // Transform exterior ring
      LinearRing shell = GEOMETRY_FACTORY.createLinearRing(
          transformCoordinates(poly.getExteriorRing().getCoordinates(), transform));

      // Transform interior rings (holes)
      LinearRing[] holes = new LinearRing[poly.getNumInteriorRing()];
      for (int i = 0; i < poly.getNumInteriorRing(); i++) {
        holes[i] = GEOMETRY_FACTORY.createLinearRing(
            transformCoordinates(poly.getInteriorRingN(i).getCoordinates(), transform));
      }

      return GEOMETRY_FACTORY.createPolygon(shell, holes);
    } else if (geometry instanceof LinearRing) {
      return GEOMETRY_FACTORY.createLinearRing(transformedCoords);
    } else if (geometry instanceof LineString) {
      return GEOMETRY_FACTORY.createLineString(transformedCoords);
    }

    // Default fallback
    return GEOMETRY_FACTORY.createGeometry(geometry);
  }

  /**
   * Helper method to transform an array of coordinates using the provided transform.
   *
   * @param coords The coordinates to transform
   * @param transform The transform to apply
   * @return The transformed coordinates
   * @throws TransformException If the transformation fails
   */
  private static Coordinate[] transformCoordinates(Coordinate[] coords, MathTransform transform)
      throws TransformException {
    Coordinate[] transformedCoords = new Coordinate[coords.length];

    for (int i = 0; i < coords.length; i++) {
      double[] srcPt = new double[] {coords[i].x, coords[i].y};
      double[] dstPt = new double[2];
      transform.transform(srcPt, 0, dstPt, 0, 1);
      transformedCoords[i] = new Coordinate(dstPt[0], dstPt[1]);
    }

    return transformedCoords;
  }

  /**
   * Creates a right-handed coordinate reference system from the given CRS code. Note that, if a
   * CoordinateReferenceSystem is already right-handed, this method will return an instantiation
   * of the same CRS one would expect from the code passed in.
   *
   * @param crsCode The CRS code
   * @return The right-handed CRS
   * @throws FactoryException If the CRS cannot be created
   */
  public static CoordinateReferenceSystem getRightHandedCrs(String crsCode)
      throws FactoryException {

    CoordinateReferenceSystem unsafeCrs = CRS.forCode(crsCode);
    CoordinateReferenceSystem rightHandedCrs =
        AbstractCRS.castOrCopy(unsafeCrs).forConvention(AxesConvention.RIGHT_HANDED);

    if (rightHandedCrs == null) {
      throw new FactoryException("Failed to create right-handed CRS for code: " + crsCode);
    }

    return rightHandedCrs;
  }

  /**
   * Creates a right-handed coordinate reference system from a given. Note that, if a
   * CoordinateReferenceSystem is already right-handed, this method will return the same CRS.
   *
   * @param crs The CRS
   * @return The right-handed CRS
   * @throws FactoryException If the CRS cannot be created
   */
  public static CoordinateReferenceSystem getRightHandedCrs(
      CoordinateReferenceSystem crs) throws FactoryException {

    CoordinateReferenceSystem rightHandedCrs =
        AbstractCRS.castOrCopy(crs).forConvention(AxesConvention.RIGHT_HANDED);

    if (rightHandedCrs == null) {
      throw new FactoryException("Failed to create right-handed CRS for code: " + crs);
    }

    return rightHandedCrs;
  }

}
