package org.joshsim.geo.geometry;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.apache.sis.referencing.CRS;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.joshsim.engine.geometry.grid.GridShape;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.FactoryException;

/**
 * A utility class for mapping grid-based geometries to Earth-based coordinate systems
 * using RealizedGridCrs.
 */
public class GridToEarthMapper {
  private static final Map<String, CoordinateReferenceSystem> CRS_CACHE = new HashMap<>();
  private static final Map<String, RealizedGridCrs> GRID_CRS_CACHE = new HashMap<>();

  /**
   * Converts a GridShape to EarthGeometry using RealizedGridCrs transformations.
   *
   * @param gridShape The grid shape to convert
   * @param targetCrsCode EPSG code for target coordinate system
   * @param gridOriginX X coordinate in target CRS of grid origin (top left)
   * @param gridOriginY Y coordinate in target CRS of grid origin (top left)
   * @param cellWidth Width of a grid cell in target CRS units
   * @return New EarthGeometry representation
   */
  public static EarthGeometry gridToEarth(
      GridShape gridShape,
      String targetCrsCode,
      BigDecimal gridOriginX,
      BigDecimal gridOriginY,
      BigDecimal cellWidth
  ) {
    try {
      // Get or create the target CRS
      CoordinateReferenceSystem targetCrs = getCrsFromCode(targetCrsCode);
      
      // Create or get a RealizedGridCrs for this configuration
      RealizedGridCrs gridCrs = getGridCrs(
          targetCrsCode, gridOriginX, gridOriginY, cellWidth);
      
      // Create geometry factory with the realized grid CRS
      EarthGeometryFactory factory = new EarthGeometryFactory(targetCrs, gridCrs);
      
      // Use the factory to create the appropriate Earth geometry
      return (EarthGeometry) factory.createFromGrid(gridShape);
      
    } catch (Exception e) {
      throw new RuntimeException("Failed to convert grid shape to Earth coordinates", e);
    }
  }

  /**
   * Gets or creates a RealizedGridCrs for the given parameters.
   *
   * @param crsCode The target CRS code
   * @param originX The grid origin X in target CRS
   * @param originY The grid origin Y in target CRS
   * @param cellWidth The cell width in target CRS units
   * @return A RealizedGridCrs for transformation
   */
  public static RealizedGridCrs getGridCrs(
      String crsCode, 
      BigDecimal originX, 
      BigDecimal originY, 
      BigDecimal cellWidth) throws FactoryException, IOException {
      
    // Create a cache key based on parameters
    String key = crsCode + ":" + originX + ":" + originY + ":" + cellWidth;
    
    // Check cache first
    if (GRID_CRS_CACHE.containsKey(key)) {
      return GRID_CRS_CACHE.get(key);
    }
    
    // Create extents for this grid (assuming a default grid size if not specified)
    PatchBuilderExtents extents = new PatchBuilderExtents(
        originX,                                  // topLeftX
        originY,                                  // topLeftY 
        originX.add(cellWidth.multiply(new BigDecimal(100))), // bottomRightX (arbitrary grid size)
        originY.add(cellWidth.multiply(new BigDecimal(100)))  // bottomRightY (arbitrary grid size)
    );
    
    // For simplicity, we'll use the units of the target CRS
    CoordinateReferenceSystem targetCrs = getCrsFromCode(crsCode);
    String crsUnits = targetCrs.getCoordinateSystem().getAxis(0).getUnit().toString();
    
    // Create grid CRS definition
    GridCrsDefinition definition = new GridCrsDefinition(
        "Grid_" + key.hashCode(),  // name  
        crsCode,                   // baseCrsCode
        extents,                   // extents
        cellWidth,                 // cellSize
        crsUnits,                  // cellSizeUnit (same as CRS)
        crsUnits                   // crsUnits
    );
    
    // Create the realized grid CRS
    RealizedGridCrs realizedGridCrs = new RealizedGridCrs(definition);
    GRID_CRS_CACHE.put(key, realizedGridCrs);
    
    return realizedGridCrs;
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