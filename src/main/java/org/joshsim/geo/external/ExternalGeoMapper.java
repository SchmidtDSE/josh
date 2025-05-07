package org.joshsim.geo.external;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.geometry.PatchSet;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Main utility class for mapping geospatial data to patches in a simulation.
 * Handles different data sources with a unified interface and supports
 * multiple interpolation strategies.
 */
public class ExternalGeoMapper {

  private final ExternalCoordinateTransformer coordinateTransformer;
  private final GeoInterpolationStrategy interpolationStrategy;
  private final String dimensionX;
  private final String dimensionY;
  private final String timeDimension;
  private final String crsCode;
  private boolean useParallelProcessing = false;

  /**
   * Constructs an ExternalGeospatialMapper with the specified components.
   *
   * @param coordinateTransformer Coordinate transformer for spatial conversions
   * @param interpolationStrategy Strategy for interpolating values from data to patches
   */
  public ExternalGeoMapper(
      ExternalCoordinateTransformer coordinateTransformer,
      GeoInterpolationStrategy interpolationStrategy) {
    this(coordinateTransformer, interpolationStrategy, null, null, null, null);
  }

  /**
   * Constructs an ExternalGeospatialMapper with the specified components and dimension settings.
   *
   * @param coordinateTransformer Coordinate transformer for spatial conversions
   * @param interpolationStrategy Strategy for interpolating values from data to patches
   * @param dimensionX The name of the X dimension (can be null for auto-detection)
   * @param dimensionY The name of the Y dimension (can be null for auto-detection)
   * @param timeDimension The name of the time dimension (can be null)
   * @param crsCode The coordinate reference system code (can be null)
   */
  public ExternalGeoMapper(
      ExternalCoordinateTransformer coordinateTransformer,
      GeoInterpolationStrategy interpolationStrategy,
      String dimensionX,
      String dimensionY,
      String timeDimension,
      String crsCode) {
    this.coordinateTransformer = coordinateTransformer;
    this.interpolationStrategy = interpolationStrategy;
    this.dimensionX = dimensionX;
    this.dimensionY = dimensionY;
    this.timeDimension = timeDimension;
    this.crsCode = crsCode;
  }

  /**
   * Enable or disable parallel processing.
   *
   * @param useParallelProcessing true to enable parallel processing, false to disable
   */
  public void setUseParallelProcessing(boolean useParallelProcessing) {
    this.useParallelProcessing = useParallelProcessing;
  }

  /**
   * Maps geospatial data to patches based on spatial location for a specified time range.
   *
   * @param dataFilePath Path to the data file
   * @param variableNames List of variable names to extract
   * @param patchSet Set of patches to map data to
   * @param minTimestep Minimum time step to process (inclusive, 0-based)
   * @param maxTimestep Maximum time step to process (inclusive, or -1 for all available)
   * @return Nested map of variable name to time step to patch key to value
   * @throws IOException If there's an error reading the data file
   */
  public Map<String, Map<Integer, Map<GeoKey, EngineValue>>> mapDataToPatchValues(
      String dataFilePath,
      List<String> variableNames,
      PatchSet patchSet,
      int minTimestep,
      int maxTimestep
  ) throws IOException {

    Map<String, Map<Integer, Map<GeoKey, EngineValue>>> result = new HashMap<>();

    // Get appropriate reader for this file
    try (ExternalDataReader reader = ExternalDataReaderFactory.createReader(
          dataFilePath
      )) {
      // Open data source
      reader.open(dataFilePath);
      reader.setDimensions(dimensionX, dimensionY, Optional.ofNullable(timeDimension));
      if (crsCode != null) {
        reader.setCrsCode(crsCode);
      }

      // If no variable names provided, get all available variables
      List<String> actualVariables = variableNames.isEmpty()
          ? reader.getVariableNames() : variableNames;

      // Get spatial dimensions of the data source
      ExternalSpatialDimensions dimensions = reader.getSpatialDimensions();

      // Get time dimension size
      int availableTimeSteps = reader.getTimeDimensionSize().orElse(1);

      // Determine actual time range to process
      int actualMinTimestep = Math.max(0, minTimestep);
      int actualMaxTimestep = (maxTimestep < 0) ? availableTimeSteps - 1
          : Math.min(maxTimestep, availableTimeSteps - 1);

      // Process each requested variable
      for (String varName : actualVariables) {
        // Create nested maps for this variable
        Map<Integer, Map<GeoKey, EngineValue>> timeStepMaps = new HashMap<>();
        result.put(varName, timeStepMaps);

        // Process each time step in the requested range
        for (int t = actualMinTimestep; t <= actualMaxTimestep; t++) {
          // Create a map for this time step by collecting the stream
          Map<GeoKey, EngineValue> patchValueMap;
          try (Stream<Map.Entry<GeoKey, EngineValue>> stream = streamVariableTimeStepToPatches(
              reader, dataFilePath, varName, t, dimensions, patchSet)) {
            patchValueMap = stream.collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
          }
          timeStepMaps.put(t, patchValueMap);
        }
      }
    } catch (Exception e) {
      throw new IOException("Failed to map data to patches: " + e.getMessage(), e);
    }

    return result;
  }

  /**
   * Maps geospatial data to patches based on spatial location for a specific time step.
   *
   * @param dataFilePath Path to the data file
   * @param variableNames List of variable names to extract
   * @param patchSet Set of patches to map data to
   * @param timestep The specific time step to process (0-based)
   * @return Nested map of variable name to time step to patch key to value
   * @throws IOException If there's an error reading the data file
   */
  public Map<String, Map<Integer, Map<GeoKey, EngineValue>>> mapDataToPatchValues(
      String dataFilePath,
      List<String> variableNames,
      PatchSet patchSet,
      int timestep
  ) throws IOException {
    // Simply delegate to the range-based method with the same timestep as both min and max
    return mapDataToPatchValues(dataFilePath, variableNames, patchSet, timestep, timestep);
  }

  /**
   * Returns a stream of patch key-value entries for a specific variable and time step.
   * The stream must be closed when done to release resources.
   *
   * @param sharedReader The shared data reader for sequential processing
   * @param dataFilePath The path to the data file for creating thread-local readers
   * @param variableName The variable to extract
   * @param timeStep The time step to process
   * @param dimensions The spatial dimensions information
   * @param patchSet The set of patches
   * @return Stream of patch key to value entries
   */
  public Stream<Map.Entry<GeoKey, EngineValue>> streamVariableTimeStepToPatches(
      ExternalDataReader sharedReader,
      String dataFilePath,
      String variableName,
      int timeStep,
      ExternalSpatialDimensions dimensions,
      PatchSet patchSet) {
    
    var patchStream = useParallelProcessing
        ? patchSet.getPatches().parallelStream()
        : patchSet.getPatches().stream();
    
    return patchStream.flatMap(patch -> {
      ExternalDataReader effectiveReader = sharedReader;
      
      try {
        // Create a thread-local reader only for parallel processing
        ExternalDataReader threadLocalReader = null;
        if (useParallelProcessing) {
          threadLocalReader = ExternalDataReaderFactory.createReader(dataFilePath);
          threadLocalReader.open(dataFilePath);
          threadLocalReader.setDimensions(
                dimensionX, dimensionY, Optional.ofNullable(timeDimension));
          if (crsCode != null) {
            threadLocalReader.setCrsCode(crsCode);
          }
          effectiveReader = threadLocalReader;
        }
        
        try {
          Optional<EngineValue> valueOpt = interpolationStrategy.interpolateValue(
              patch,
              variableName,
              timeStep,
              patchSet.getGridCrsDefinition(),
              coordinateTransformer,
              effectiveReader,
              dimensions
          );

          // Return entry if value exists, otherwise empty stream
          if (valueOpt.isPresent() && patch.getKey().isPresent()) {
            GeoKey key = patch.getKey().get();
            return Stream.of(Map.entry(key, valueOpt.get()));
          }
          return Stream.empty();
          
        } finally {
          // Close the thread-local reader if we created one
          if (threadLocalReader != null) {
            try {
              threadLocalReader.close();
            } catch (Exception e) {
              // Log or wrap this exception
            }
          }
        }
      } catch (Exception e) {
        throw new RuntimeException("Error interpolating value for patch: " + patch, e);
      }
    });
  }

  /**
   * Returns a stream of patch values for a specific variable and time step.
   * This creates and manages its own data reader.
   *
   * @param dataFilePath Path to the data file
   * @param variableName The variable to extract
   * @param timeStep The time step to process
   * @param patchSet Set of patches to map data to
   * @return Stream of patch key to value entries that must be closed when done
   * @throws IOException If there's an error reading the data file
   */
  public Stream<Map.Entry<GeoKey, EngineValue>> streamVariableTimeStepToPatches(
      String dataFilePath,
      String variableName,
      int timeStep,
      PatchSet patchSet) throws IOException {

    // Create and configure reader
    ExternalDataReader reader = ExternalDataReaderFactory.createReader(dataFilePath);
    reader.open(dataFilePath);
    reader.setDimensions(dimensionX, dimensionY, Optional.ofNullable(timeDimension));
    if (crsCode != null) {
      reader.setCrsCode(crsCode);
    }
    
    ExternalSpatialDimensions dimensions = reader.getSpatialDimensions();
    
    // Create a stream that will close the reader when it's done
    return streamVariableTimeStepToPatches(
          reader, dataFilePath, variableName, timeStep, dimensions, patchSet)
        .onClose(() -> {
          try {
            reader.close();
          } catch (Exception e) {
            throw new RuntimeException("Failed to close data reader", e);
          }
        });
  }
}