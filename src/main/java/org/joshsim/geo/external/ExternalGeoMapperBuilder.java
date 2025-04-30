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

  /**
   * Sets the coordinate transformer for the mapper.
   *
   * @param transformer Coordinate transformer for spatial conversions
   * @return This builder instance
   */
  public ExternalGeoMapperBuilder withCoordinateTransformer(
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
  public ExternalGeoMapperBuilder withInterpolationStrategy(GeoInterpolationStrategy strategy) {
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
  public ExternalGeoMapperBuilder withInterpolationMethod(
      InterpolationMethod method, EngineValueFactory valueFactory) {
    GeoInterpolationStrategyFactory factory = new GeoInterpolationStrategyFactory(valueFactory);
    this.interpolationStrategy = factory.createStrategy(method);
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

    return new ExternalGeoMapper(coordinateTransformer, interpolationStrategy);
  }
}
