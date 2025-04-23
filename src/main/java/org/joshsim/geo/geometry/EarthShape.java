package org.joshsim.geo.geometry;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.sis.util.Utilities;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.grid.GridShape;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.util.FactoryException;

/**
 * Abstract base class for all Earth-based geometries using JTS geometry with Apache SIS
 * for coordinate transforms.
 */
public abstract class EarthShape implements EngineGeometry {

  protected final Geometry innerGeometry;
  protected final CoordinateReferenceSystem crs;
  protected final Optional<Map<CoordinateReferenceSystem, MathTransform>> transformers;
  protected static final GeometryFactory JTS_GEOMETRY_FACTORY = new GeometryFactory();

  /**
   * Constructs an Earth shape with a provided JTS geometry and CRS.
   *
   * @param innerGeometry The JTS geometry
   * @param crs The coordinate reference system
   * @param transformers Optional pre-computed transformers to other CRS
   */
  protected EarthShape(
      Geometry innerGeometry,
      CoordinateReferenceSystem crs,
      Optional<Map<CoordinateReferenceSystem, MathTransform>> transformers) {
    this.innerGeometry = Objects.requireNonNull(innerGeometry, "Geometry cannot be null");
    this.crs = Objects.requireNonNull(crs, "Coordinate reference system cannot be null");
    this.transformers = transformers;
  }

  /**
   * Constructs an Earth shape with a provided JTS geometry and CRS.
   *
   * @param innerGeometry The JTS geometry
   * @param crs The coordinate reference system
   */
  protected EarthShape(Geometry innerGeometry, CoordinateReferenceSystem crs) {
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
   * @return A new EarthShape transformed to the target CRS
   * @throws FactoryException if the transformation fails
   */
  public abstract EarthShape asTargetCrs(CoordinateReferenceSystem targetCrs)
      throws FactoryException;

  /**
   * Checks if a point is contained within this geometry.
   *
   * @param locationX The X coordinate of the point
   * @param locationY The Y coordinate of the point
   * @return true if the point intersects this geometry, false otherwise
   */
  @Override
  public boolean intersects(BigDecimal locationX, BigDecimal locationY) {
    Point point = JTS_GEOMETRY_FACTORY.createPoint(
        new Coordinate(locationX.doubleValue(), locationY.doubleValue()));
    return innerGeometry.intersects(point);
  }

  @Override
  public boolean intersects(EngineGeometry other) {
    if (other instanceof EarthShape) {
      EarthShape otherShape = (EarthShape) other;
      // If CRS are different, transform to match
      if (!Utilities.equalsIgnoreMetadata(crs, otherShape.getCrs())) {
        EarthShape transformed;
        try {
          transformed = otherShape.asTargetCrs(crs);
        } catch (FactoryException e) {
          throw new RuntimeException("Failed to transform geometry", e);
        }
        return innerGeometry.intersects(transformed.getInnerGeometry());
      }
      return innerGeometry.intersects(otherShape.getInnerGeometry());
    } else {
      // If not EarthShape, use the grid representation
      return other.intersects(this);
    }
  }

  @Override
  public EarthPoint getCenter() {
    Coordinate center = innerGeometry.getCentroid().getCoordinate();
    return new EarthPoint(
        JTS_GEOMETRY_FACTORY.createPoint(center),
        crs,
        transformers
    );
  }

  @Override
  public EarthShape getOnEarth() {
    return this;
  }

  @Override
  public GridShape getOnGrid() {
    throw new UnsupportedOperationException(
        "Conversion from Earth to Grid space requires a mapper. Use GridToEarthMapper.");
  }

  @Override
  public BigDecimal getCenterX() {
    return BigDecimal.valueOf(innerGeometry.getCentroid().getX());
  }

  @Override
  public BigDecimal getCenterY() {
    return BigDecimal.valueOf(innerGeometry.getCentroid().getY());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof EarthShape)) {
      return false;
    }
    EarthShape other = (EarthShape) obj;
    return innerGeometry.equals(other.innerGeometry) 
        && Utilities.equalsIgnoreMetadata(crs, other.crs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(innerGeometry, crs.getName().getCode());
  }
}