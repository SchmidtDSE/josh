package org.joshsim.geo.geometry;

import org.apache.sis.util.Utilities;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
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

  /**
   * Transforms point to target CRS using EarthTransformer.
   *
   * @param targetCrs The target coordinate reference system
   * @return A new EarthPoint transformed to the target CRS
   * @throws FactoryException if the transformation fails
   */
  @Override
  public EarthPoint asTargetCrs(CoordinateReferenceSystem targetCrs) throws FactoryException {
    // If same CRS, return self
    if (Utilities.equalsIgnoreMetadata(crs, targetCrs)) {
      return this;
    }

    // Use the EarthTransformer to handle the transformation
    EarthGeometry transformed = EarthTransformer.earthToEarth(this, targetCrs);

    // Since we know this is a point, cast to ensure we return an EarthPoint
    return new EarthPoint((Point) transformed.getInnerGeometry(), targetCrs);
  }

  @Override
  public String toString() {
    return String.format("EarthPoint[%.6f, %.6f, %s]",
        getPoint().getX(), getPoint().getY(), crs.getName().getCode());
  }
}
