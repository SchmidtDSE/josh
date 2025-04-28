package org.joshsim.geo.geometry;

import org.apache.sis.util.Utilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Represents a square on Earth using JTS Polygon geometry.
 */
public class EarthSquare extends EarthShape {

  private final double width;

  /**
   * Constructs an Earth square with specified center and width.
   *
   * @param centerX X coordinate of the center
   * @param centerY Y coordinate of the center
   * @param width Width of the square sides in the units of the CRS
   * @param crs The coordinate reference system
   */
  public EarthSquare(double centerX, double centerY, double width, CoordinateReferenceSystem crs) {
    super(createSquarePolygon(centerX, centerY, width), crs);
    this.width = width;
  }

  /**
   * Constructs an Earth square from an existing polygon.
   *
   * @param polygon Square polygon
   * @param width Width of the square
   * @param crs The coordinate reference system
   */
  protected EarthSquare(Polygon polygon, double width, CoordinateReferenceSystem crs) {
    super(polygon, crs);
    this.width = width;
  }

  /**
   * Gets the width of this square.
   *
   * @return The width in units of the CRS
   */
  public double getWidth() {
    return width;
  }

  /**
   * Gets the underlying polygon representation of the square.
   *
   * @return The JTS Polygon representing this square
   */
  public Polygon getPolygon() {
    return (Polygon) innerGeometry;
  }

  @Override
  public EarthSquare asTargetCrs(CoordinateReferenceSystem targetCrs) throws FactoryException {
    // If same CRS, return self
    if (Utilities.equalsIgnoreMetadata(crs, targetCrs)) {
      return this;
    }

    // Use the EarthTransformer to handle the transformation
    EarthGeometry transformed = EarthTransformer.earthToEarth(this, targetCrs);
    Geometry transformedGeom = transformed.getInnerGeometry();

    // Note: After transformation, the shape might not be a perfect square anymore
    // We'll use the average width as the new width value, but this will be inaccurate
    // with large transformations where distortion might be more extreme.
    double avgWidth = computeAverageWidth(transformedGeom);

    return new EarthSquare((Polygon) transformedGeom, avgWidth, targetCrs);
  }

  /**
   * Creates a square polygon centered at the specified coordinates.
   *
   * @param centerX Center X coordinate
   * @param centerY Center Y coordinate
   * @param width Width of the square
   * @return A JTS Polygon representing the square
   */
  private static Polygon createSquarePolygon(double centerX, double centerY, double width) {
    double halfWidth = width / 2.0;

    Coordinate[] coords = new Coordinate[5];
    coords[0] = new Coordinate(centerX - halfWidth, centerY - halfWidth);
    coords[1] = new Coordinate(centerX + halfWidth, centerY - halfWidth);
    coords[2] = new Coordinate(centerX + halfWidth, centerY + halfWidth);
    coords[3] = new Coordinate(centerX - halfWidth, centerY + halfWidth);
    coords[4] = new Coordinate(centerX - halfWidth, centerY - halfWidth); // Close the ring

    LinearRing ring = JTS_GEOMETRY_FACTORY.createLinearRing(coords);
    return JTS_GEOMETRY_FACTORY.createPolygon(ring);
  }

  /**
   * Computes the average width of a geometry by measuring distances between opposite sides.
   *
   * @param geometry The geometry to measure
   * @return The average width
   */
  private double computeAverageWidth(Geometry geometry) {
    // For simplicity, we'll use the bounding box dimensions as an approximation
    return Math.sqrt(geometry.getArea());
  }

  @Override
  public String toString() {
    return String.format("EarthSquare[center=(%.6f, %.6f), width=%.6f, %s]",
        innerGeometry.getCentroid().getX(),
        innerGeometry.getCentroid().getY(),
        width,
        crs.getName().getCode());
  }
}