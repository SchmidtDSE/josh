package org.joshsim.geo.external;

import java.util.Optional;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.geo.external.GeoInterpolationStrategyFactory.InterpolationMethod;

/**
 * Builder for ExternalGeoMapper.
 * Creates and configures ExternalGeoMapper instances.
 */
public class ExternalGeoMapperBuilder {

  private final EngineValueFactory valueFactory;
  private ExternalCoordinateTransformer coordinateTransformer;
  private GeoInterpolationStrategy interpolationStrategy;
  private String dimensionX;
  private String dimensionY;
  private String timeDimension;
  private String crsCode;
  private Optional<Long> forcedTimestep = Optional.empty();

  /**
   * Create a new builder.
   *
   * @param valueFactory Factory for creating EngineValue objects in mappers built by this builder.
   */
  public ExternalGeoMapperBuilder(EngineValueFactory valueFactory) {
    this.valueFactory = valueFactory;
  }

  /**
   * Sets the coordinate transformer for the mapper.
   *
   * @param transformer Coordinate transformer for spatial conversions
   * @return This builder instance
   */
  public ExternalGeoMapperBuilder addCoordinateTransformer(
      ExternalCoordinateTransformer transformer) {
    this.coordinateTransformer = transformer;
    return this;
  }

  /**
   * Sets the interpolation strategy for the mapper.
   *
   * @param strategy Strategy for interpolating values from data to patches
   * @return This builder instance
   */
  public ExternalGeoMapperBuilder addInterpolationStrategy(GeoInterpolationStrategy strategy) {
    this.interpolationStrategy = strategy;
    return this;
  }

  /**
   * Sets the interpolation strategy using a predefined method.
   *
   * @param method The interpolation method to use
   * @param valueFactory Factory for creating EngineValue objects
   * @return This builder instance
   */
  public ExternalGeoMapperBuilder addInterpolationMethod(
      InterpolationMethod method, EngineValueFactory valueFactory) {
    GeoInterpolationStrategyFactory factory = new GeoInterpolationStrategyFactory(valueFactory);
    this.interpolationStrategy = factory.createStrategy(method);
    return this;
  }

  /**
   * Sets the dimension names for the data reader.
   *
   * @param dimX The name of the X dimension
   * @param dimY The name of the Y dimension
   * @param timeDim The name of the time dimension (can be null)
   * @return This builder instance
   */
  public ExternalGeoMapperBuilder addDimensions(String dimX, String dimY, String timeDim) {
    this.dimensionX = dimX;
    this.dimensionY = dimY;
    this.timeDimension = timeDim;
    return this;
  }

  /**
   * Indicate a timestep that all read data should be assuemd to be part of.
   *
   * @param timestep The timestep to force for all data points.
   * @return This builder instance
   */
  public ExternalGeoMapperBuilder forceTimestep(long timestep) {
    forcedTimestep = Optional.of(timestep);
    return this;
  }

  /**
   * Return to deault of reading the timestep from the file.
   *
   * @return This builder instance
   */
  public ExternalGeoMapperBuilder clearForcedTimestep() {
    forcedTimestep = Optional.empty();
    return this;
  }

  /**
   * Sets the coordinate reference system code.
   *
   * @param crsCode The CRS code (e.g., "EPSG:4326")
   * @return This builder instance
   */
  public ExternalGeoMapperBuilder addCrsCode(String crsCode) {
    this.crsCode = crsCode;
    return this;
  }

  /**
   * Builds and returns a configured ExternalGeoMapper instance.
   *
   * @return A new ExternalGeoMapper instance
   */
  public ExternalGeoMapper build() {
    if (coordinateTransformer == null) {
      coordinateTransformer = new GridExternalCoordinateTransformer();
    }

    if (interpolationStrategy == null) {
      interpolationStrategy = new NearestNeighborInterpolationStrategy();
    }

    return new ExternalGeoMapper(
        valueFactory,
        coordinateTransformer,
        interpolationStrategy,
        dimensionX,
        dimensionY,
        timeDimension,
        crsCode,
        forcedTimestep
    );
  }
}
