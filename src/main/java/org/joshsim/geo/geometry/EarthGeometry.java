package org.joshsim.geo.geometry;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.EnginePoint;
import org.joshsim.engine.geometry.shape.GridShape;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * A geometric object using JTS geometry implementation.
 */
public class EarthGeometry implements EngineGeometry {

  protected Geometry innerGeometry;
  protected CoordinateReferenceSystem crs;
  protected Optional<Map<CoordinateReferenceSystem, MathTransform>> transformers;
  protected static final GeometryFactory JTS_GEOMETRY_FACTORY = new GeometryFactory();

  /**
   * Constructs a Geometry with a provided JTS geometry and CRS.
   */
  public EarthGeometry(
      Geometry innerGeometry,
      CoordinateReferenceSystem crs,
      Optional<Map<CoordinateReferenceSystem, MathTransform>> transformers
  ) {
    this.innerGeometry = Objects.requireNonNull(innerGeometry, "Geometry cannot be null");
    this.crs = Objects.requireNonNull(crs, "Coordinate reference system cannot be null");
    this.transformers = transformers;
  }

  /**
   * Constructs a Geometry with a provided JTS geometry and CRS.
   */
  public EarthGeometry(Geometry innerGeometry, CoordinateReferenceSystem crs) {
    this(innerGeometry, crs, Optional.empty());
  }

  /**
   * Gets the JTS geometry.
   */
  public Geometry getInnerGeometry() {
    return innerGeometry;
  }

  /**
   * Gets the coordinate reference system of this geometry.
   */
  public CoordinateReferenceSystem getCrs() {
    return crs;
  }

  /**
   * Transforms geometry to target CRS.
   */
  public EarthGeometry asTargetCrs(CoordinateReferenceSystem targetCrs) {
    if (CRS.equalsIgnoreMetadata(crs, targetCrs)) {
      return this;
    }

    try {
      MathTransform transform;
      if (transformers.isPresent() && transformers.get().containsKey(targetCrs)) {
        transform = transformers.get().get(targetCrs);
      } else {
        transform = CRS.findMathTransform(crs, targetCrs, true);
      }

      Geometry transformedGeometry = JTS.transform(
          innerGeometry,
          transform
      );
      return new EarthGeometry(transformedGeometry, targetCrs);
    } catch (Exception e) {
      throw new RuntimeException("Failed to transform geometry to target CRS", e);
    }
  }

  /**
   * Checks if a point is contained within this geometry.
   */
  public boolean intersects(BigDecimal locationX, BigDecimal locationY) {
    Point point = JTS_GEOMETRY_FACTORY.createPoint(
        new Coordinate(locationX.doubleValue(), locationY.doubleValue())
    );
    return innerGeometry.intersects(point);
  }

  @Override
  public EarthGeometry getOnEarth() {
    return this;
  }

  @Override
  public GridShape getOnGrid() {
    throw new UnsupportedOperationException(
        "Conversion from Earth to Grid space reserved for future use."
    );
  }

  @Override
  public EnginePoint getCenter() {
    return null;
  }

  /**
   * Checks if this geometry intersects with another.
   */
  public boolean intersects(EngineGeometry other) {
    // TODO: Remove cast
    EarthGeometry otherEarth = other.getOnEarth();

    // Ensure both geometries use the same CRS
    if (!CRS.equalsIgnoreMetadata(crs, otherEarth.getCrs())) {
      otherEarth = otherEarth.asTargetCrs(crs);
    }
    return getInnerGeometry().intersects(otherEarth.getInnerGeometry());
  }

  /**
   * Computes the convex hull of this geometry and another geometry.
   * Ensures both geometries are in the same CRS before computation.
   *
   * @param other The other geometry to compute the convex hull with.
   * @return A new EarthGeometry representing the convex hull.
   */
  public EarthGeometry getConvexHull(EarthGeometry other) {
    // Ensure both geometries use the same CRS
    if (!CRS.equalsIgnoreMetadata(crs, other.getCrs())) {
      other = other.asTargetCrs(crs);
    }
    Geometry convexHull = getInnerGeometry().union(other.getInnerGeometry()).convexHull();
    return new EarthGeometry(convexHull, crs);
  }

  /**
   * Computes the convex hull of this geometry.
   *
   * @return A new EarthGeometry representing the convex hull.
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
   * Gets the envelope of this geometry as a GeneralEnvelope.
   */
  public ReferencedEnvelope getEnvelope() {
    Envelope env = getInnerGeometry().getEnvelopeInternal();
    return new ReferencedEnvelope(env, crs);
  }

  @Override
  public boolean equals(Object o) {
    // Check if the object is the exact same instance
    if (this == o) {
      return true;
    }

    // Check if the object is null or not of the same class
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    // Delegate to the type-specific equals method
    return equals((EarthGeometry) o);
  }

  /**
   * Type-specific equality check.
   *
   * @param other The other EarthGeometry to compare with
   * @return true if the geometries are equal, false otherwise
   */
  public boolean equals(EarthGeometry other) {
    // Compare innerGeometry using equals
    if (!innerGeometry.equals(other.innerGeometry)) {
      return false;
    }
    // Compare CRS using CRS.equalsIgnoreMetadata
    return CRS.equalsIgnoreMetadata(crs, other.crs);
  }

  @Override
  public int hashCode() {
    int result = innerGeometry.hashCode();
    // Use a simple approach for CRS hashCode
    result = 31 * result + crs.toString().hashCode();
    return result;
  }

  @Override
  public String toString() {
    return String.format(
        "EarthGeometry at %s, %s with crs of %s.",
        getCenterX().toString(),
        getCenterY().toString(),
        crs
    );
  }

}
