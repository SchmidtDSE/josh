package org.joshsim.geo.geometry;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.sis.referencing.CRS;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.joshsim.engine.geometry.grid.GridShape;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * A utility class for mapping grid-based geometries to Earth-based coordinate systems
 * using RealizedGridCrs.
 */
public class GridToEarthMapper {
  private static final Map<String, CoordinateReferenceSystem> CRS_CACHE = 
      new ConcurrentHashMap<>();
  private static final Map<String, RealizedGridCrs> GRID_CRS_CACHE = 
      new ConcurrentHashMap<>();

  /**
   * Converts a GridShape to EarthGeometry using a GridCrsDefinition.
   *
   * @param gridShape The grid shape to convert
   * @param definition The grid CRS definition
   * @param targetCrsCode EPSG code for target coordinate system
   * @return New EarthGeometry representation
   */
  public static EarthGeometry gridToEarth(
      GridShape gridShape,
      GridCrsDefinition definition,
      String targetCrsCode
  ) {
    try {
      // Get or create the target CRS
      CoordinateReferenceSystem targetCrs = getCrsFromCode(targetCrsCode);
      
      // Get or create a RealizedGridCrs from the definition
      RealizedGridCrs gridCrs = getRealizedGridCrs(definition);
      
      // Create geometry factory with the realized grid CRS
      EarthGeometryFactory factory = new EarthGeometryFactory(targetCrs, gridCrs);
      
      // Use the factory to create the appropriate Earth geometry
      return (EarthGeometry) factory.createFromGrid(gridShape);
      
    } catch (Exception e) {
      throw new RuntimeException("Failed to convert grid shape to Earth coordinates", e);
    }
  }
  
  /**
   * Converts a GridShape to EarthGeometry using an existing RealizedGridCrs.
   *
   * @param gridShape The grid shape to convert
   * @param realizedGridCrs The realized grid CRS
   * @param targetCrsCode EPSG code for target coordinate system
   * @return New EarthGeometry representation
   */
  public static EarthGeometry gridToEarth(
      GridShape gridShape,
      RealizedGridCrs realizedGridCrs,
      String targetCrsCode
  ) {
    try {
      // Get or create the target CRS
      CoordinateReferenceSystem targetCrs = getCrsFromCode(targetCrsCode);
      
      // Create geometry factory with the realized grid CRS
      EarthGeometryFactory factory = new EarthGeometryFactory(targetCrs, realizedGridCrs);
      
      // Use the factory to create the appropriate Earth geometry
      return (EarthGeometry) factory.createFromGrid(gridShape);
      
    } catch (Exception e) {
      throw new RuntimeException("Failed to convert grid shape to Earth coordinates", e);
    }
  }

  /**
   * Gets or creates a RealizedGridCrs from a GridCrsDefinition.
   *
   * @param definition The grid CRS definition
   * @return A RealizedGridCrs for transformation
   * @throws TransformException if transformation fails
   */
  public static RealizedGridCrs getRealizedGridCrs(GridCrsDefinition definition) 
      throws FactoryException, IOException, TransformException {
    // Create a cache key based on the definition
    String key = definition.toString();
    
    // Check cache first
    if (GRID_CRS_CACHE.containsKey(key)) {
      return GRID_CRS_CACHE.get(key);
    }
    
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