package org.joshsim.geo.geometry;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.apache.sis.referencing.CRS;
import org.joshsim.engine.geometry.grid.GridShape;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A utility class for mapping grid-based geometries to Earth-based coordinate systems. This
 * allows the engine side to understand how to interpret grid shapes without needing to import
 * spatial libraries.
 */
public class GridToEarthMapper {
  private static final Map<String, CoordinateReferenceSystem> CRS_CACHE = new HashMap<>();

  /**
   * Converts a GridShape to EarthGeometry using the appropriate factory methods.
   *
   * @param gridShape The grid shape to convert
   * @param targetCrsCode EPSG code for target coordinate system
   * @param gridOriginX X coordinate in target CRS of grid origin
   * @param gridOriginY Y coordinate in target CRS of grid origin
   * @param cellWidth Width of a grid cell in target CRS units
   * @return New EarthGeometry representation
   */
  public static EarthGeometry gridToEarth(
      GridShape gridShape,
      String targetCrsCode,
      BigDecimal gridOriginX,
      BigDecimal gridOriginY,
      BigDecimal cellWidth
  ){

    // Get or create the target CRS
    CoordinateReferenceSystem targetCrs = getCrsFromCode(targetCrsCode);

    // Create geometry factory for the target CRS
    EarthGeometryFactory factory = new EarthGeometryFactory(targetCrs);

    // Delegate to appropriate factory method based on shape type
    switch (gridShape.getGridShapeType()) {
      case POINT:
        return (EarthGeometry) factory.createPointFromGrid(
          gridShape, gridOriginX, gridOriginY, cellWidth);

      case CIRCLE:
        return (EarthGeometry) factory.createCircleFromGrid(
          gridShape, gridOriginX, gridOriginY, cellWidth);

      case SQUARE:
        return (EarthGeometry) factory.createRectangleFromGrid(
          gridShape, gridOriginX, gridOriginY, cellWidth);

      default:
        throw new UnsupportedOperationException(
          "Shape type not supported: " + gridShape.getGridShapeType()
        );
    }
  }

  /**
   * Gets a coordinate reference system from the cache or creates a new one.
   */
  private static CoordinateReferenceSystem getCrsFromCode(String crsCode) {
    try {
      // Check cache first
      if (CRS_CACHE.containsKey(crsCode)) {
        return CRS_CACHE.get(crsCode);
      }

      // Create new CRS and add to cache
      CoordinateReferenceSystem crs = CRS.forCode(crsCode);
      CRS_CACHE.put(crsCode, crs);
      return crs;

    } catch (Exception e) {
      throw new RuntimeException("Failed to create CRS from code: " + crsCode, e);
    }
  }
}
