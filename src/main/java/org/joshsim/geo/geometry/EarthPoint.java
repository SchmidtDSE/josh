package org.joshsim.geo.geometry;

import java.util.Map;
import java.util.Optional;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.Utilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Represents a point on Earth using JTS Point geometry.
 */
public class EarthPoint extends EarthShape {

  /**
   * Constructs an Earth point with a provided JTS point and CRS.
   *
   * @param point The JTS point
   * @param crs The coordinate reference system
   * @param transformers Optional pre-computed transformers to other CRS
   */
  public EarthPoint(
      Point point, 
      CoordinateReferenceSystem crs,
      Optional<Map<CoordinateReferenceSystem, MathTransform>> transformers) {
    super(point, crs, transformers);
  }

  /**
   * Constructs an Earth point with a provided JTS point and CRS.
   *
   * @param point The JTS point
   * @param crs The coordinate reference system
   */
  public EarthPoint(Point point, CoordinateReferenceSystem crs) {
    super(point, crs);
  }

  /**
   * Gets the underlying JTS Point.
   *
   * @return The JTS Point geometry
   */
  public Point getPoint() {
    return (Point) innerGeometry;
  }

  @Override
  public EarthPoint asTargetCrs(CoordinateReferenceSystem targetCrs) throws FactoryException {
    // If same CRS, return self
    if (Utilities.equalsIgnoreMetadata(crs, targetCrs)) {
      return this;
    }

    try {
      MathTransform transform;
      // Check if we already have a transformer
      if (transformers.isPresent() && transformers.get().containsKey(targetCrs)) {
        transform = transformers.get().get(targetCrs);
      } else {
        transform = CRS.findOperation(crs, targetCrs, null).getMathTransform();
      }

      // Use the utility for transformation
      Geometry transformedGeom = JtsTransformUtility.transform(innerGeometry, transform);
      
      // Cast to Point since we know it's a point
      Point transformedPoint = (Point) transformedGeom;
      
      return new EarthPoint(transformedPoint, targetCrs, transformers);
    } catch (TransformException e) {
      throw new RuntimeException("Failed to transform point to target CRS", e);
    }
  }

  @Override
  public String toString() {
    return String.format("EarthPoint[%.6f, %.6f, %s]", 
        getPoint().getX(), getPoint().getY(), crs.getName().getCode());
  }
}