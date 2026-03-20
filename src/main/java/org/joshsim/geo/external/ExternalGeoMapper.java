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
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Main utility class for mapping geospatial data to patches in a simulation.
 * Handles different data sources with a unified interface and supports
 * multiple interpolation strategies.
 */
public class ExternalGeoMapper {

  private final EngineValueFactory valueFactory;
  private final ExternalCoordinateTransformer coordinateTransformer;
  private final GeoInterpolationStrategy interpolationStrategy;
  private final String dimensionX;
  private final String dimensionY;
  private final String timeDimension;
  private final String crsCode;
  private final Optional<Long> forcedTimestep;
  private boolean useParallelProcessing = false;

  /**
   * Constructs an ExternalGeospatialMapper with the specified components and dimension settings.
   *
   * @param valueFactory Value factory for creating EngineValue objects within this mapper.
   * @param coordinateTransformer Coordinate transformer for spatial conversions
   * @param interpolationStrategy Strategy for interpolating values from data to patches
   * @param dimensionX The name of the X dimension (can be null for auto-detection)
   * @param dimensionY The name of the Y dimension (can be null for auto-detection)
   * @param timeDimension The name of the time dimension (can be null)
   * @param crsCode The coordinate reference system code (can be null)
   */
  public ExternalGeoMapper(
      EngineValueFactory valueFactory,
      ExternalCoordinateTransformer coordinateTransformer,
      GeoInterpolationStrategy interpolationStrategy,
      String dimensionX,
      String dimensionY,
      String timeDimension,
      String crsCode
  ) {
    this.valueFactory = valueFactory;
    this.coordinateTransformer = coordinateTransformer;
    this.interpolationStrategy = interpolationStrategy;
    this.dimensionX = dimensionX;
    this.dimensionY = dimensionY;
    this.timeDimension = timeDimension;
    this.crsCode = crsCode;
    this.forcedTimestep = Optional.empty();
  }

  /**
   * Constructs an ExternalGeospatialMapper with the specified components and dimension settings.
   *
   * @param valueFactory Value factory for creating EngineValue objects within this mapper.
   * @param coordinateTransformer Coordinate transformer for spatial conversions
   * @param interpolationStrategy Strategy for interpolating values from data to patches
   * @param dimensionX The name of the X dimension (can be null for auto-detection)
   * @param dimensionY The name of the Y dimension (can be null for auto-detection)
   * @param timeDimension The name of the time dimension (can be null)
   * @param crsCode The coordinate reference system code (can be null)
   * @param forcedTimestep If provided, all values read will be assumed to have this timestep
   */
  public ExternalGeoMapper(
      EngineValueFactory valueFactory,
      ExternalCoordinateTransformer coordinateTransformer,
      GeoInterpolationStrategy interpolationStrategy,
      String dimensionX,
      String dimensionY,
      String timeDimension,
      String crsCode,
      Optional<Long> forcedTimestep
  ) {
    this.valueFactory = valueFactory;
    this.coordinateTransformer = coordinateTransformer;
    this.interpolationStrategy = interpolationStrategy;
    this.dimensionX = dimensionX;
    this.dimensionY = dimensionY;
    this.timeDimension = timeDimension;
    this.crsCode = crsCode;
    this.forcedTimestep = forcedTimestep;
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

    // Get metadata from a temporary reader
    List<String> actualVariables;
    int actualMinTimestep;
    int actualMaxTimestep;

    try (ExternalDataReader reader = ExternalDataReaderFactory.createReader(
          valueFactory, dataFilePath)) {
      reader.open(dataFilePath);
      reader.setDimensions(dimensionX, dimensionY, Optional.ofNullable(timeDimension));
      if (crsCode != null) {
        reader.setCrsCode(crsCode);
      }

      actualVariables = variableNames.isEmpty() ? reader.getVariableNames() : variableNames;
      int availableTimeSteps = reader.getTimeDimensionSize().orElse(1);
      actualMinTimestep = Math.max(0, minTimestep);
      actualMaxTimestep = (maxTimestep < 0) ? availableTimeSteps - 1
          : Math.min(maxTimestep, availableTimeSteps - 1);

      if (forcedTimestep.isPresent()) {
        boolean matches = forcedTimestep.get() == actualMinTimestep
            && forcedTimestep.get() == actualMaxTimestep;
        if (!matches) {
          throw new IllegalArgumentException(
              "If forcing timestep, min and max must equal that step."
          );
        }
      }
    } catch (Exception e) {
      if (e instanceof IOException) {
        throw (IOException) e;
      }
      throw new IOException("Failed to read metadata from data file", e);
    }

    // Process each variable and timestep
    for (String varName : actualVariables) {
      Map<Integer, Map<GeoKey, EngineValue>> timeStepMaps = new HashMap<>();
      result.put(varName, timeStepMaps);

      for (int t = actualMinTimestep; t <= actualMaxTimestep; t++) {
        try (Stream<Map.Entry<GeoKey, EngineValue>> stream = streamVariableTimeStepToPatches(
            dataFilePath, varName, t, patchSet)) {
          Map<GeoKey, EngineValue> patchValueMap = stream.collect(Collectors.toMap(
              Map.Entry::getKey,
              Map.Entry::getValue
          ));
          timeStepMaps.put(t, patchValueMap);
        }
      }
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
    return mapDataToPatchValues(dataFilePath, variableNames, patchSet, timestep, timestep);
  }

  /**
   * Returns a stream of patch values for a specific variable and time step.
   * The stream must be closed when done to release resources.
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

    if (useParallelProcessing) {
      return streamPatchesParallel(dataFilePath, variableName, timeStep, patchSet);
    } else {
      return streamPatchesSequential(dataFilePath, variableName, timeStep, patchSet);
    }
  }

  private Stream<Map.Entry<GeoKey, EngineValue>> streamPatchesSequential(
      String dataFilePath,
      String variableName,
      int timeStep,
      PatchSet patchSet) throws IOException {

    ExternalDataReader reader = ExternalDataReaderFactory.createReader(valueFactory, dataFilePath);
    reader.open(dataFilePath);
    reader.setDimensions(dimensionX, dimensionY, Optional.ofNullable(timeDimension));
    if (crsCode != null) {
      reader.setCrsCode(crsCode);
    }
    ExternalSpatialDimensions dimensions = reader.getSpatialDimensions();

    Stream<Map.Entry<GeoKey, EngineValue>> resultStream = patchSet.getPatches().stream()
        .flatMap(patch -> interpolatePatch(patch, variableName, timeStep, patchSet, reader,
            dimensions));

    return resultStream.onClose(() -> closeReader(reader));
  }

  private Stream<Map.Entry<GeoKey, EngineValue>> streamPatchesParallel(
      String dataFilePath,
      String variableName,
      int timeStep,
      PatchSet patchSet) throws IOException {

    // Get dimensions from a temporary reader
    ExternalSpatialDimensions dimensions;
    try (ExternalDataReader tempReader = ExternalDataReaderFactory.createReader(
        valueFactory, dataFilePath)) {
      tempReader.open(dataFilePath);
      tempReader.setDimensions(dimensionX, dimensionY, Optional.ofNullable(timeDimension));
      if (crsCode != null) {
        tempReader.setCrsCode(crsCode);
      }
      dimensions = tempReader.getSpatialDimensions();
    } catch (Exception e) {
      throw new IOException("Failed to read spatial dimensions", e);
    }

    ParallelReaderPool readerPool = new ParallelReaderPool(
        valueFactory, dataFilePath, dimensionX, dimensionY, timeDimension, crsCode);

    Stream<Map.Entry<GeoKey, EngineValue>> resultStream = patchSet.getPatches()
        .parallelStream()
        .flatMap(patch -> interpolatePatch(patch, variableName, timeStep, patchSet,
            readerPool.getReader(), dimensions));

    return resultStream.onClose(readerPool::close);
  }

  private Stream<Map.Entry<GeoKey, EngineValue>> interpolatePatch(
      org.joshsim.engine.entity.base.MutableEntity patch,
      String variableName,
      int timeStep,
      PatchSet patchSet,
      ExternalDataReader reader,
      ExternalSpatialDimensions dimensions) {
    try {
      Optional<EngineValue> valueOpt = interpolationStrategy.interpolateValue(
          patch,
          variableName,
          timeStep,
          patchSet.getGridCrsDefinition(),
          coordinateTransformer,
          reader,
          dimensions
      );

      if (valueOpt.isPresent() && patch.getKey().isPresent()) {
        return Stream.of(Map.entry(patch.getKey().get(), valueOpt.get()));
      }
      return Stream.empty();
    } catch (Exception e) {
      throw new RuntimeException("Error interpolating value for patch: " + patch, e);
    }
  }

  private void closeReader(ExternalDataReader reader) {
    try {
      reader.close();
    } catch (Exception e) {
      throw new RuntimeException("Failed to close data reader", e);
    }
  }
}
