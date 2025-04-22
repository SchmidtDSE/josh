package org.joshsim.geo.geometry;

import java.util.Map;
import java.util.Optional;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.Utilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Represents a circle on Earth using a JTS Polygon approximation.
 */
public class EarthCircle extends EarthShape {
  
  private final double radius;

  /**
   * Constructs an Earth circle with specified center and radius.
   *
   * @param centerX X coordinate of the center
   * @param centerY Y coordinate of the center
   * @param radius Radius of the circle in the units of the CRS
   * @param crs The coordinate reference system
   */
  public EarthCircle(double centerX, double centerY, double radius, CoordinateReferenceSystem crs) {
    super(createCirclePolygon(centerX, centerY, radius), crs);
    this.radius = radius;
  }

  /**
   * Constructs an Earth circle from an existing polygon (should be circle-like).
   *
   * @param polygon Circle polygon approximation
   * @param radius Radius of the circle
   * @param crs The coordinate reference system
   * @param transformers Optional pre-computed transformers to other CRS
   */
  protected EarthCircle(
      Polygon polygon, 
      double radius, 
      CoordinateReferenceSystem crs,
      Optional<Map<CoordinateReferenceSystem, MathTransform>> transformers) {
    super(polygon, crs, transformers);
    this.radius = radius;
  }

  /**
   * Gets the radius of this circle.
   *
   * @return The radius in units of the CRS
   */
  public double getRadius() {
    return radius;
  }

  /**
   * Gets the underlying polygon representation of the circle.
   *
   * @return The JTS Polygon approximating this circle
   */
  public Polygon getPolygon() {
    return (Polygon) innerGeometry;
  }

  @Override
  public EarthCircle asTargetCrs(CoordinateReferenceSystem targetCrs) {
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

      Geometry transformedGeom = JtsTransformUtility.transform(innerGeometry, transform);
      
      // Since circle shape may be distorted after transformation,
      // we approximate the transformed shape as a circle with the same area
      double area = transformedGeom.getArea();
      double estimatedRadius = Math.sqrt(area / Math.PI);
      
      return new EarthCircle(
          (Polygon) transformedGeom, estimatedRadius, targetCrs, transformers);
    } catch (FactoryException | TransformException e) {
      throw new RuntimeException("Failed to transform circle to target CRS", e);
    }
  }

  private static Polygon createCirclePolygon(double centerX, double centerY, double radius) {
    GeometricShapeFactory shapeFactory = new GeometricShapeFactory(JTS_GEOMETRY_FACTORY);
    shapeFactory.setCentre(new Coordinate(centerX, centerY));
    shapeFactory.setWidth(radius * 2);
    shapeFactory.setHeight(radius * 2);
    shapeFactory.setNumPoints(32); // Number of vertices - higher for smoother circles
    return shapeFactory.createCircle();
  }

  @Override
  public String toString() {
    Coordinate center = innerGeometry.getCentroid().getCoordinate();
    return String.format("EarthCircle[center=(%.6f, %.6f), radius=%.6f, %s]", 
        center.x, center.y, radius, crs.getName().getCode());
  }
}