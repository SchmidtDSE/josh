package org.joshsim.geo.geometry;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.sis.referencing.CRS;
import org.apache.sis.util.Utilities;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.joshsim.engine.geometry.grid.GridShape;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * A utility class that centralizes all coordinate transformation logic.
 * Handles transformations between grid and Earth coordinates, and between different Earth CRS.
 */
public class EarthTransformer {
  private static final Map<String, CoordinateReferenceSystem> CRS_CACHE = new ConcurrentHashMap<>();

  // Cache GridCrsManagers by their definition hash
  private static final Map<Integer, GridCrsManager> GRID_CRS_MANAGER_CACHE =
      new ConcurrentHashMap<>();

  // Cache EarthGeometryFactories by combination of targetCRS and gridCrsManager
  private static final Map<String, EarthGeometryFactory> FACTORY_CACHE = new ConcurrentHashMap<>();
  
  // Cache MathTransforms for CRS pairs
  private static final Map<String, MathTransform> TRANSFORM_CACHE = new ConcurrentHashMap<>();

  // Private constructor to prevent instantiation
  private EarthTransformer() {
    throw new AssertionError("EarthTransformer is a utility class and should not be instantiated");
  }

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

      // Get or create a GridCrsManager from the definition
      GridCrsManager gridCrsManager = getGridCrsManager(definition);

      // Get or create the appropriate factory
      EarthGeometryFactory factory = getEarthGeometryFactory(targetCrs, gridCrsManager);

      // Use the factory to create the appropriate Earth geometry
      return (EarthGeometry) factory.createFromGrid(gridShape);

    } catch (Exception e) {
      throw new RuntimeException("Failed to convert grid shape to Earth coordinates", e);
    }
  }

  /**
   * Converts a GridShape to EarthGeometry using an existing GridCrsManager.
   *
   * @param gridShape The grid shape to convert
   * @param gridCrsManager The realized grid CRS
   * @param targetCrsCode EPSG code for target coordinate system
   * @return New EarthGeometry representation
   */
  public static EarthGeometry gridToEarth(
      GridShape gridShape,
      GridCrsManager gridCrsManager,
      String targetCrsCode
  ) {
    try {
      // Get or create the target CRS
      CoordinateReferenceSystem targetCrs = getCrsFromCode(targetCrsCode);

      // Get or create the appropriate factory
      EarthGeometryFactory factory = getEarthGeometryFactory(targetCrs, gridCrsManager);

      // Use the factory to create the appropriate Earth geometry
      return (EarthGeometry) factory.createFromGrid(gridShape);

    } catch (Exception e) {
      throw new RuntimeException("Failed to convert grid shape to Earth coordinates", e);
    }
  }

  /**
   * Transforms an EarthGeometry from its current CRS to a target CRS.
   *
   * @param sourceGeometry The source Earth geometry
   * @param targetCrsCode EPSG code for target coordinate system
   * @return New EarthGeometry in the target CRS
   */
  public static EarthGeometry earthToEarth(
      EarthShape sourceGeometry,
      String targetCrsCode
  ) {
    try {
      CoordinateReferenceSystem sourceCrs = sourceGeometry.getCrs();
      CoordinateReferenceSystem targetCrs = getCrsFromCode(targetCrsCode);
      
      // If same CRS, return the original geometry
      if (Utilities.equalsIgnoreMetadata(sourceCrs, targetCrs)) {
        return (EarthGeometry) sourceGeometry;
      }

      // Get or create the transform between CRSes
      MathTransform transform = getTransform(sourceCrs, targetCrs);
      
      // Transform the geometry
      Geometry transformedGeom = JtsTransformUtility.transform(
          sourceGeometry.getInnerGeometry(), transform);
      
      // Create new EarthGeometry with transformed geometry and target CRS
      return new EarthGeometry(transformedGeom, targetCrs);
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to transform Earth geometry to target CRS: " + targetCrsCode, e);
    }
  }
  
  /**
   * Transforms an EarthGeometry from its current CRS to another CRS.
   *
   * @param sourceGeometry The source Earth geometry
   * @param targetCrs The target coordinate reference system
   * @return New EarthGeometry in the target CRS
   */
  public static EarthGeometry earthToEarth(
      EarthShape sourceGeometry,
      CoordinateReferenceSystem targetCrs
  ) {
    try {
      CoordinateReferenceSystem sourceCrs = sourceGeometry.getCrs();
      
      // If same CRS, return the original geometry
      if (Utilities.equalsIgnoreMetadata(sourceCrs, targetCrs)) {
        return (EarthGeometry) sourceGeometry;
      }

      // Get or create the transform between CRSes
      MathTransform transform = getTransform(sourceCrs, targetCrs);
      
      // Transform the geometry
      Geometry transformedGeom = JtsTransformUtility.transform(
          sourceGeometry.getInnerGeometry(), transform);
      
      // Create new EarthGeometry with transformed geometry and target CRS
      return new EarthGeometry(transformedGeom, targetCrs);
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to transform Earth geometry to target CRS: " + targetCrs.getName().getCode(), e);
    }
  }
  
  /**
   * Gets or creates a MathTransform between two coordinate reference systems, with caching.
   *
   * @param sourceCrs The source coordinate reference system
   * @param targetCrs The target coordinate reference system
   * @return A MathTransform for converting between the two CRSes
   * @throws FactoryException if transform creation fails
   */
  public static MathTransform getTransform(
      CoordinateReferenceSystem sourceCrs,
      CoordinateReferenceSystem targetCrs
  ) throws FactoryException {
    // Create a unique cache key
    String cacheKey = sourceCrs.getName().getCode() + "-TO-" + targetCrs.getName().getCode();
    
    // Check cache first
    MathTransform transform = TRANSFORM_CACHE.get(cacheKey);
    if (transform == null) {
      // Create new transform and add to cache
      transform = CRS.findOperation(sourceCrs, targetCrs, null).getMathTransform();
      TRANSFORM_CACHE.put(cacheKey, transform);
    }
    
    return transform;
  }

  /**
   * Gets or creates a GridCrsManager from a GridCrsDefinition, with caching.
   *
   * @param definition The grid CRS definition
   * @return A GridCrsManager for transformation
   * @throws FactoryException if CRS factory fails
   * @throws IOException if IO operation fails
   * @throws TransformException if transformation fails
   */
  public static GridCrsManager getGridCrsManager(GridCrsDefinition definition)
      throws FactoryException, IOException, TransformException {
    // Use definition hashCode as a cache key
    int cacheKey = definition.hashCode();

    // Check cache first
    GridCrsManager manager = GRID_CRS_MANAGER_CACHE.get(cacheKey);
    if (manager == null) {
      // Create new manager and add to cache
      manager = new GridCrsManager(definition);
      GRID_CRS_MANAGER_CACHE.put(cacheKey, manager);
    }

    return manager;
  }

  /**
   * Gets or creates an EarthGeometryFactory for the given CRS and GridCrsManager, with caching.
   *
   * @param targetCrs The target coordinate reference system
   * @param gridCrsManager The grid CRS manager
   * @return A cached or new EarthGeometryFactory
   */
  private static EarthGeometryFactory getEarthGeometryFactory(
      CoordinateReferenceSystem targetCrs,
      GridCrsManager gridCrsManager) {

    // Create a unique cache key from both objects
    String cacheKey = targetCrs.getName().getCode() + "-" + gridCrsManager.hashCode();

    // Check cache first
    EarthGeometryFactory factory = FACTORY_CACHE.get(cacheKey);
    if (factory == null) {
      // Create new factory and add to cache
      factory = new EarthGeometryFactory(targetCrs, gridCrsManager);
      FACTORY_CACHE.put(cacheKey, factory);
    }

    return factory;
  }

  /**
   * Gets a coordinate reference system from the cache or creates a new one.
   */
  private static CoordinateReferenceSystem getCrsFromCode(String crsCode) {
    try {
      // Check cache first
      CoordinateReferenceSystem crs = CRS_CACHE.get(crsCode);
      if (crs == null) {
        // Create new CRS and add to cache
        crs = JtsTransformUtility.getRightHandedCrs(crsCode);
        CRS_CACHE.put(crsCode, crs);
      }
      return crs;
    } catch (Exception e) {
      throw new RuntimeException("Failed to create CRS from code: " + crsCode, e);
    }
  }
}