/**
 * Structures describing geometric shapes and their properties.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.geo.geometry;

import java.math.BigDecimal;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.util.GeometricShapeFactory;


/**
 * Factory methods for creating geometric shapes using JTS geometry.
 */
public class EarthGeometryFactory implements EngineGeometryFactory {
  private static final GeometryFactory JTS_GEOMETRY_FACTORY =
      new GeometryFactory();
  private static final double SQUARE_TOLERANCE = 0.01;

  private final CoordinateReferenceSystem crs;

  /**
   * Create a new factory for the given coordinate reference system.
   *
   * @param crs The coodinate reference system to use in constructing these geometries.
   */
  public EarthGeometryFactory(CoordinateReferenceSystem crs) {
    this.crs = crs;
  }

  @Override
  public EngineGeometry createSquare(
      BigDecimal centerX, BigDecimal centerY, BigDecimal width
  ) {
    double halfWidth = width.doubleValue() / 2.0;
    double centerValX = centerX.doubleValue();
    double centerValY = centerY.doubleValue();

    double minX = centerValX - halfWidth;
    double maxX = centerValX + halfWidth;
    double minY = centerValY - halfWidth;
    double maxY = centerValY + halfWidth;

    Coordinate[] coords = new Coordinate[5];
    coords[0] = new Coordinate(minX, minY);
    coords[1] = new Coordinate(maxX, minY);
    coords[2] = new Coordinate(maxX, maxY);
    coords[3] = new Coordinate(minX, maxY);
    coords[4] = new Coordinate(minX, minY); // Close the ring

    LinearRing ring = JTS_GEOMETRY_FACTORY.createLinearRing(coords);
    Polygon square = JTS_GEOMETRY_FACTORY.createPolygon(ring, null);

    return new EarthGeometry(square, crs);
  }

  @Override
  public EngineGeometry createSquare(
      BigDecimal topLeftX,
      BigDecimal topLeftY,
      BigDecimal bottomRightX,
      BigDecimal bottomRightY
  ) {
    double minX = topLeftX.doubleValue();
    double maxX = bottomRightX.doubleValue();
    double maxY = topLeftY.doubleValue();
    double minY = bottomRightY.doubleValue();

    // Check if it's a square (approximately, within reasonable precision)
    double width = Math.abs(maxX - minX);
    double height = Math.abs(maxY - minY);
    if ((1 - (width / height)) > SQUARE_TOLERANCE) {
      return null;
    }

    Coordinate[] coords = new Coordinate[5];
    coords[0] = new Coordinate(minX, minY);
    coords[1] = new Coordinate(maxX, minY);
    coords[2] = new Coordinate(maxX, maxY);
    coords[3] = new Coordinate(minX, maxY);
    coords[4] = new Coordinate(minX, minY); // Close the ring

    LinearRing ring = JTS_GEOMETRY_FACTORY.createLinearRing(coords);
    Polygon square = JTS_GEOMETRY_FACTORY.createPolygon(ring, null);

    return new EarthGeometry(square, crs);
  }

  @Override
  public EngineGeometry createCircle(
      BigDecimal centerX, BigDecimal centerY, BigDecimal radius
  ) {
    double radiusVal = radius.doubleValue();
    double centerValX = centerX.doubleValue();
    double centerValY = centerY.doubleValue();

    GeometricShapeFactory shapeFactory = new GeometricShapeFactory(JTS_GEOMETRY_FACTORY);
    shapeFactory.setNumPoints(64); // Smooth circle approximation
    shapeFactory.setCentre(new Coordinate(centerValX, centerValY));
    shapeFactory.setSize(radiusVal * 2);
    Polygon circle = shapeFactory.createCircle();

    return new EarthGeometry(circle, crs);
  }

  @Override
  public EngineGeometry createCircle(
      BigDecimal point1X,
      BigDecimal point1Y,
      BigDecimal point2X,
      BigDecimal point2Y
  ) {
    double pointValX = point1X.doubleValue();
    double pointValY = point1Y.doubleValue();
    double centerValX = point2X.doubleValue();
    double centerValY = point2Y.doubleValue();

    // Calculate radius using distance between center and point
    double radius = Math.sqrt(
        Math.pow(pointValX - centerValX, 2) + Math.pow(pointValY - centerValY, 2)
    );

    GeometricShapeFactory shapeFactory = new GeometricShapeFactory(JTS_GEOMETRY_FACTORY);
    shapeFactory.setNumPoints(64); // Smooth circle approximation
    shapeFactory.setCentre(new Coordinate(centerValX, centerValY));
    shapeFactory.setSize(radius * 2);
    Polygon circle = shapeFactory.createCircle();

    return new EarthGeometry(circle, crs);
  }

  @Override
  public EngineGeometry createPoint(
      BigDecimal x,
      BigDecimal y
  ) {
    Point point = JTS_GEOMETRY_FACTORY.createPoint(
        new Coordinate(x.doubleValue(), y.doubleValue())
    );
    return new EarthGeometry(point, crs);
  }

  @Override
  public String toString() {
    return "EarthGeometryFactory with crs of " + crs + ".";
  }

}
