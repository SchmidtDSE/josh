package org.joshsim.geo.external;

import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.geo.external.GeoInterpolationStrategyFactory.InterpolationMethod;

/**
 * Builder for ExternalGeoMapper.
 * Creates and configures ExternalGeoMapper instances.
 */
public class ExternalGeoMapperBuilder {
  private ExternalCoordinateTransformer coordinateTransformer;
  private GeoInterpolationStrategy interpolationStrategy;
  private String dimensionX;
  private String dimensionY;
  private String timeDimension;
  private String crsCode;

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
   * @param xDim The name of the X dimension
   * @param yDim The name of the Y dimension
   * @param timeDim The name of the time dimension (can be null)
   * @return This builder instance
   */
  public ExternalGeoMapperBuilder addDimensions(String xDim, String yDim, String timeDim) {
    this.dimensionX = xDim;
    this.dimensionY = yDim;
    this.timeDimension = timeDim;
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
        coordinateTransformer, 
        interpolationStrategy,
        dimensionX,
        dimensionY,
        timeDimension,
        crsCode);
  }
}