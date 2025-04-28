package org.joshsim.geo.external;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.PatchSet;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;
import org.joshsim.geo.external.GeoInterpolationStrategyFactory.InterpolationMethod;

/**
 * Main utility class for mapping geospatial data to patches in a simulation.
 * Handles different data sources with a unified interface and supports
 * multiple interpolation strategies.
 */
public class ExternalGeoMapper {
  
  private final ExternalCoordinateTransformer coordinateTransformer;
  private final GeoInterpolationStrategy interpolationStrategy;
  
  /**
   * Constructs an ExternalGeospatialMapper with the specified components.
   *
   * @param coordinateTransformer Coordinate transformer for spatial conversions
   * @param interpolationStrategy Strategy for interpolating values from data to patches
   */
  public ExternalGeoMapper(
      ExternalCoordinateTransformer coordinateTransformer,
      GeoInterpolationStrategy interpolationStrategy) {
    this.coordinateTransformer = coordinateTransformer;
    this.interpolationStrategy = interpolationStrategy;
  }
  
  /**
   * Maps geospatial data to patches based on spatial location.
   *
   * @param dataFilePath Path to the data file
   * @param variableNames List of variable names to extract
   * @param patchSet Set of patches to map data to
   * @param timeSteps Number of time steps to process (or -1 for all)
   * @return Nested map of variable name to time step to patch key to value
   * @throws IOException If there's an error reading the data file
   */
  public Map<String, Map<Integer, Map<GeoKey, EngineValue>>> mapDataToPatchValues(
      String dataFilePath,
      List<String> variableNames,
      PatchSet patchSet,
      int timeSteps) throws IOException {
    
    Map<String, Map<Integer, Map<GeoKey, EngineValue>>> result = new HashMap<>();
    
    // Get appropriate reader for this file
    try (ExternalDataReader reader = ExternalDataReaderFactory.createReader(dataFilePath)) {
      // Open data source
      reader.open(dataFilePath);
      
      // If no variable names provided, get all available variables
      List<String> actualVariables = variableNames.isEmpty() ? 
          reader.getVariableNames() : variableNames;
      
      // Get spatial dimensions of the data source
      ExternalSpatialDimensions dimensions = reader.getSpatialDimensions();
      
      // Get time dimension size
      int actualTimeSteps = reader.getTimeDimensionSize().orElse(1);
      if (timeSteps > 0) {
        actualTimeSteps = Math.min(timeSteps, actualTimeSteps);
      }
      
      // Process each requested variable
      for (String varName : actualVariables) {
        // Create nested maps for this variable
        Map<Integer, Map<GeoKey, EngineValue>> timeStepMaps = new HashMap<>();
        result.put(varName, timeStepMaps);
        
        // Process each time step
        for (int t = 0; t < actualTimeSteps; t++) {
          // Create a map for this time step
          Map<GeoKey, EngineValue> patchValueMap = mapVariableTimeStepToPatches(
              reader, varName, t, dimensions, patchSet);
          timeStepMaps.put(t, patchValueMap);
        }
      }
    } catch (Exception e) {
      throw new IOException("Failed to map data to patches: " + e.getMessage(), e);
    }
    
    return result;
  }
  
  /**
   * Maps a specific variable at a specific time step to patch values.
   *
   * @param reader The data source reader
   * @param variableName The variable to extract
   * @param timeStep The time step to process
   * @param dimensions The spatial dimensions information
   * @param patchSet The set of patches
   * @return Map of patch keys to values
   * @throws Exception If there's an error mapping the data
   */
  private Map<GeoKey, EngineValue> mapVariableTimeStepToPatches(
      ExternalDataReader reader,
      String variableName,
      int timeStep,
      ExternalSpatialDimensions dimensions,
      PatchSet patchSet) throws Exception {
    
    Map<GeoKey, EngineValue> patchValueMap = new ConcurrentHashMap<>();
    
    // Process each patch (could be parallelized)
    for (MutableEntity patch : patchSet.getPatches()) {
      Optional<EngineValue> valueOpt = interpolationStrategy.interpolateValue(
          patch,
          variableName,
          timeStep,
          patchSet.getGridCrsDefinition(),
          coordinateTransformer,
          reader,
          dimensions);
      
      // Store in the map if value exists
      if (valueOpt.isPresent()) {
        GeoKey key = interpolationStrategy.getGeoKey(patch);
        patchValueMap.put(key, valueOpt.get());
      }
    }
    
    return patchValueMap;
  }
}