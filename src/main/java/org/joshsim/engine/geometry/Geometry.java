package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.sis.geometry.GeneralEnvelope;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * A geometric object using JTS geometry implementation.
 */
public class Geometry implements Spatial {

  protected org.locationtech.jts.geom.Geometry jtsGeometry;
  protected CoordinateReferenceSystem crs;
  protected Optional<Map<CoordinateReferenceSystem, MathTransform>> transformers;
  protected static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  /**
   * Constructs a Geometry with a provided JTS geometry and CRS.
   */
  public Geometry(
        org.locationtech.jts.geom.Geometry jtsGeometry, 
        CoordinateReferenceSystem crs,
        Optional<Map<CoordinateReferenceSystem, MathTransform>> transformers
  ) {
    this.jtsGeometry = Objects.requireNonNull(jtsGeometry, "Geometry cannot be null");
    this.crs = Objects.requireNonNull(crs, "Coordinate reference system cannot be null");
    this.transformers = transformers;
  }

  /**
   * Constructs a Geometry with a provided JTS geometry and CRS.
   */
  public Geometry(org.locationtech.jts.geom.Geometry jtsGeometry, CoordinateReferenceSystem crs) {
    this(jtsGeometry, crs, Optional.empty());
  }

  /**
   * Gets the JTS geometry.
   */
  public org.locationtech.jts.geom.Geometry getJtsGeometry() {
    return jtsGeometry;
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
  public Geometry asTargetCrs(CoordinateReferenceSystem targetCrs) {
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
      
      org.locationtech.jts.geom.Geometry transformedGeometry = JTS.transform(jtsGeometry, transform);
      return new Geometry(transformedGeometry, targetCrs);
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
    return jtsGeometry.intersects(point);
  }
  
  /**
   * Checks if this geometry intersects with another.
   */
  public boolean intersects(Geometry other) {
    // Ensure both geometries use the same CRS
    if (!CRS.equalsIgnoreMetadata(crs, other.getCrs())) {
      other = other.asTargetCrs(crs);
    }
    return jtsGeometry.intersects(other.jtsGeometry);
  }
  
  @Override
  public BigDecimal getCenterX() {
    return BigDecimal.valueOf(jtsGeometry.getCentroid().getX());
  }

  @Override
  public BigDecimal getCenterY() {
    return BigDecimal.valueOf(jtsGeometry.getCentroid().getY());
  }
  
  /**
   * Gets the envelope of this geometry as a GeneralEnvelope.
   */
  public GeneralEnvelope getEnvelope() {
    Envelope env = jtsGeometry.getEnvelopeInternal();
    GeneralEnvelope generalEnv = new GeneralEnvelope(2);
    generalEnv.setCoordinateReferenceSystem(crs);
    generalEnv.setRange(0, env.getMinX(), env.getMaxX());
    generalEnv.setRange(1, env.getMinY(), env.getMaxY());
    return generalEnv;
  }
}