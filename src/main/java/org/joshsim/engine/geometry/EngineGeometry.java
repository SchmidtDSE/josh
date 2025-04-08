package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * A geometric object using JTS geometry implementation.
 */
public class EngineGeometry implements Spatial {

  protected Geometry innerGeometry;
  protected CoordinateReferenceSystem crs;
  protected Optional<Map<CoordinateReferenceSystem, MathTransform>> transformers;
  protected static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  /**
   * Constructs a Geometry with a provided JTS geometry and CRS.
   */
  public EngineGeometry(
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
  public EngineGeometry(Geometry innerGeometry, CoordinateReferenceSystem crs) {
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
  public EngineGeometry asTargetCrs(CoordinateReferenceSystem targetCrs) {
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
      return new EngineGeometry(transformedGeometry, targetCrs);
    } catch (Exception e) {
      throw new RuntimeException("Failed to transform geometry to target CRS", e);
    }
  }

  /**
   * Checks if a point is contained within this geometry.
   */
  public boolean intersects(BigDecimal locationX, BigDecimal locationY) {
    Point point = GEOMETRY_FACTORY.createPoint(
        new Coordinate(locationX.doubleValue(), locationY.doubleValue())
    );
    return innerGeometry.intersects(point);
  }

  /**
   * Checks if this geometry intersects with another.
   */
  public boolean intersects(EngineGeometry other) {
    // Ensure both geometries use the same CRS
    if (!CRS.equalsIgnoreMetadata(crs, other.getCrs())) {
      other = other.asTargetCrs(crs);
    }
    return getInnerGeometry().intersects(other.getInnerGeometry());
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
}
