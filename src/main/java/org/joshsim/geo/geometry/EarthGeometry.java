package org.joshsim.geo.geometry;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.Utilities;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.EnginePoint;
import org.joshsim.engine.geometry.grid.GridShape;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * A geometric object using JTS geometry implementation with Apache SIS for coordinate transforms.
 */
public class EarthGeometry implements EngineGeometry {

  private final Geometry innerGeometry;
  private final CoordinateReferenceSystem crs;
  private final Optional<Map<CoordinateReferenceSystem, MathTransform>> transformers;
  private static final GeometryFactory JTS_GEOMETRY_FACTORY = new GeometryFactory();

  /**
   * Constructs a Geometry with a provided JTS geometry and CRS.
   *
   * @param innerGeometry The JTS geometry
   * @param crs The coordinate reference system
   * @param transformers Optional pre-computed transformers to other CRS
   */
  public EarthGeometry(
      Geometry innerGeometry,
      CoordinateReferenceSystem crs,
      Optional<Map<CoordinateReferenceSystem, MathTransform>> transformers) {
    this.innerGeometry = Objects.requireNonNull(innerGeometry, "Geometry cannot be null");
    this.crs = Objects.requireNonNull(crs, "Coordinate reference system cannot be null");
    this.transformers = transformers;
  }

  /**
   * Constructs a Geometry with a provided JTS geometry and CRS.
   *
   * @param innerGeometry The JTS geometry
   * @param crs The coordinate reference system
   */
  public EarthGeometry(Geometry innerGeometry, CoordinateReferenceSystem crs) {
    this(innerGeometry, crs, Optional.empty());
  }

  /**
   * Gets the JTS geometry.
   *
   * @return The inner JTS geometry
   */
  public Geometry getInnerGeometry() {
    return innerGeometry;
  }

  /**
   * Gets the coordinate reference system of this geometry.
   *
   * @return The coordinate reference system
   */
  public CoordinateReferenceSystem getCrs() {
    return crs;
  }

  /**
   * Transforms geometry to target CRS.
   *
   * @param targetCrs The target coordinate reference system
   * @return A new geometry transformed to the target CRS
   */
  public EarthGeometry asTargetCrs(CoordinateReferenceSystem targetCrs) {
    if (Utilities.equalsIgnoreMetadata(crs, targetCrs)) {
      return this;
    }

    try {
      MathTransform transform;
      if (transformers.isPresent() && transformers.get().containsKey(targetCrs)) {
        transform = transformers.get().get(targetCrs);
      } else {
        transform = CRS.findOperation(crs, targetCrs, null).getMathTransform();
      }

      Geometry transformedGeometry = transformGeometry(innerGeometry, transform);
      return new EarthGeometry(transformedGeometry, targetCrs);
    } catch (Exception e) {
      throw new RuntimeException("Failed to transform geometry to target CRS", e);
    }
  }

  /**
   * Checks if a point is contained within this geometry.
   *
   * @param locationX The X coordinate of the point
   * @param locationY The Y coordinate of the point
   * @return true if the point intersects this geometry, false otherwise
   */
  public boolean intersects(BigDecimal locationX, BigDecimal locationY) {
    Point point = JTS_GEOMETRY_FACTORY.createPoint(
        new Coordinate(locationX.doubleValue(), locationY.doubleValue()));
    return innerGeometry.intersects(point);
  }

  /**
   * Checks if this geometry intersects with another.
   *
   * @param other The other geometry to check for intersection
   * @return true if the geometries intersect, false otherwise
   */
  public boolean intersects(EarthGeometry other) {
    // Transform to same CRS if needed
    EarthShape otherEarth = other.getOnEarth();
    if (!Utilities.equalsIgnoreMetadata(crs, other.getCrs())) {
      try {
        otherEarth = otherEarth.asTargetCrs(crs);
      } catch (FactoryException e) {
        throw new RuntimeException("Failed to transform geometry to target CRS");
      }
    }
    return getInnerGeometry().intersects(otherEarth.getInnerGeometry());
  }

  @Override
  public boolean intersects(EngineGeometry other) {
    return intersects(other.getOnEarth());
  }

  @Override
  public GridShape getOnGrid() {
    throw new UnsupportedOperationException(
        "Conversion from Earth to PatchSet space reserved for future use."
    );
  }

  @Override
  public EarthShape getOnEarth() {
    throw new UnsupportedOperationException(
        "Conversion from PatchSet to Earth space reserved for future use."
    );
  }

  @Override
  public EngineGeometry getCenter() {
    Point centroid = getInnerGeometry().getCentroid();
    return new EarthPoint(
        centroid,
        crs,
        transformers
    );
  }

  /**
   * Computes the convex hull of this geometry and another geometry.
   * Ensures both geometries are in the same CRS before computation.
   *
   * @param other The other geometry to compute the convex hull with
   * @return A new EarthGeometry representing the convex hull
   */
  public EarthGeometry getConvexHull(EarthGeometry other) {
    // Ensure both geometries use the same CRS
    if (!Utilities.equalsIgnoreMetadata(crs, other.getCrs())) {
      other = other.asTargetCrs(crs);
    }
    Geometry convexHull = getInnerGeometry().union(other.getInnerGeometry()).convexHull();
    return new EarthGeometry(convexHull, crs);
  }

  /**
   * Computes the convex hull of this geometry.
   *
   * @return A new EarthGeometry representing the convex hull
   */
  public EarthGeometry getConvexHull() {
    Geometry convexHull = getInnerGeometry().convexHull();
    return new EarthGeometry(convexHull, crs);
  }

  @Override
  public BigDecimal getCenterX() {
    return BigDecimal.valueOf(getInnerGeometry().getCentroid().getX());
  }

  @Override
  public BigDecimal getCenterY() {
    return BigDecimal.valueOf(getInnerGeometry().getCentroid().getY());
  }

  /**
   * Gets the envelope of this geometry.
   *
   * @return The envelope of this geometry with its CRS
   */
  public Envelope2D getEnvelope() {
    org.locationtech.jts.geom.Envelope env = getInnerGeometry().getEnvelopeInternal();
    return new Envelope2D(crs, env.getMinX(), env.getMinY(),
        env.getWidth(), env.getHeight());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    return equals((EarthGeometry) o);
  }

  /**
   * Type-specific equality check.
   *
   * @param other The other EarthGeometry to compare with
   * @return true if the geometries are equal, false otherwise
   */
  public boolean equals(EarthGeometry other) {
    if (!innerGeometry.equals(other.innerGeometry)) {
      return false;
    }
    return Utilities.equalsIgnoreMetadata(crs, other.getCrs());
  }

  @Override
  public int hashCode() {
    int result = innerGeometry.hashCode();
    result = 31 * result + crs.toString().hashCode();
    return result;
  }

  @Override
  public String toString() {
    return String.format(
        "EarthGeometry at %s, %s with crs of %s.",
        getCenterX().toString(),
        getCenterY().toString(),
        crs);
  }

  private Geometry transformGeometry(Geometry geometry, MathTransform transform)
      throws TransformException {
    if (geometry instanceof Point) {
      return transformPoint((Point) geometry, transform);
    }

    // For other geometry types, transform each coordinate
    Coordinate[] coords = geometry.getCoordinates();
    Coordinate[] transformedCoords = new Coordinate[coords.length];

    for (int i = 0; i < coords.length; i++) {
      double[] src = new double[] {coords[i].x, coords[i].y};
      double[] dest = new double[2];
      transform.transform(src, 0, dest, 0, 1);

      // Apply precision to each coordinate
      Coordinate transformedCoord = new Coordinate(dest[0], dest[1]);
      JTS_GEOMETRY_FACTORY.getPrecisionModel().makePrecise(transformedCoord);
      transformedCoords[i] = transformedCoord;
    }

    // Use switch statement for different geometry types
    GeometryFactory factory = JTS_GEOMETRY_FACTORY;

    switch (geometry.getClass().getSimpleName()) {
      case "LineString":
        return factory.createLineString(transformedCoords);
      case "Polygon":
        return factory.createPolygon(transformedCoords);
      case "LinearRing":
        return factory.createLinearRing(transformedCoords);
      case "MultiPoint":
        Point[] points = new Point[transformedCoords.length];
        for (int i = 0; i < transformedCoords.length; i++) {
          points[i] = factory.createPoint(transformedCoords[i]);
        }
        return factory.createMultiPoint(points);
      default:
        throw new TransformException(
            "Geometry type not supported for transformation: " + geometry.getClass());
    }
  }

  /**
   * Transforms a JTS Point using an Apache SIS MathTransform.
   *
   * @param point The point to transform
   * @param transform The transform to apply
   * @return The transformed point
   * @throws TransformException If transformation fails
   */
  private Point transformPoint(Point point, MathTransform transform) throws TransformException {
    double[] srcPt = new double[] {point.getX(), point.getY()};
    double[] dstPt = new double[2];
    transform.transform(srcPt, 0, dstPt, 0, 1);
    return JTS_GEOMETRY_FACTORY.createPoint(new Coordinate(dstPt[0], dstPt[1]));
  }

}
