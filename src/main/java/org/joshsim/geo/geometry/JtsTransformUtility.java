package org.joshsim.geo.geometry;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Utility methods for transforming JTS geometries.
 */
public final class JtsTransformUtility {

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(
      new PrecisionModel(PrecisionModel.FLOATING)
  );

  private JtsTransformUtility() {
    // Private constructor to prevent instantiation
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
}
