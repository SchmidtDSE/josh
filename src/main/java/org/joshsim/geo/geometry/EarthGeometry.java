package org.joshsim.geo.geometry;

import java.math.BigDecimal;
import java.util.Objects;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.util.Utilities;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.grid.GridShape;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 * A geometric object using JTS geometry implementation with Apache SIS for coordinate transforms.
 */
public class EarthGeometry extends EarthShape {

  private final Geometry innerGeometry;
  private final CoordinateReferenceSystem crs;
  private static final GeometryFactory JTS_GEOMETRY_FACTORY = new GeometryFactory();

  /**
   * Constructs a Geometry with a provided JTS geometry and CRS.
   *
   * @param innerGeometry The JTS geometry
   * @param crs The coordinate reference system
   */
  public EarthGeometry(Geometry innerGeometry, CoordinateReferenceSystem crs) {
    super(innerGeometry, crs);
    this.innerGeometry = Objects.requireNonNull(innerGeometry, "Geometry cannot be null");
    this.crs = Objects.requireNonNull(crs, "Coordinate reference system cannot be null");
  }

  /**
   * Gets the JTS geometry.
   *
   * @return The inner JTS geometry
   */
  @Override
  public Geometry getInnerGeometry() {
    return innerGeometry;
  }

  /**
   * Gets the coordinate reference system of this geometry.
   *
   * @return The coordinate reference system
   */
  @Override
  public CoordinateReferenceSystem getCrs() {
    return crs;
  }

  /**
   * Transforms geometry to target CRS using EarthTransformer.
   *
   * @param targetCrs The target coordinate reference system
   * @return A new EarthGeometry transformed to the target CRS
   * @throws FactoryException if the transformation fails
   */
  @Override
  public EarthGeometry asTargetCrs(CoordinateReferenceSystem targetCrs) throws FactoryException {
    return EarthTransformer.earthToEarth(this, targetCrs);
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
    if (!Utilities.equalsIgnoreMetadata(crs, other.getCrs())) {
      EarthGeometry transformed = EarthTransformer.earthToEarth(other, crs);
      return getInnerGeometry().intersects(transformed.getInnerGeometry());
    }
    return getInnerGeometry().intersects(other.getInnerGeometry());
  }

  @Override
  public boolean intersects(EngineGeometry other) {
    return intersects(other.getOnEarth());
  }

  @Override
  public GridShape getOnGrid() {
    throw new UnsupportedOperationException(
        "EarthGeometry does not support conversion to GridShape");
  }

  @Override
  public EarthShape getOnEarth() {
    return this;
  }

  @Override
  public EarthPoint getCenter() {
    Point centroid = getInnerGeometry().getCentroid();
    return new EarthPoint(centroid, crs);
  }

  /**
   * Computes the convex hull of this geometry and another geometry.
   * Ensures both geometries are in the same CRS before computation.
   *
   * @param other The other geometry to compute the convex hull with
   * @return A new EarthGeometry representing the convex hull
   * @throws FactoryException if the transformation fails
   */
  public EarthGeometry getConvexHull(EarthGeometry other) throws FactoryException {
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
}
