package org.joshsim.geo.external;

import org.joshsim.engine.value.engine.EngineValueFactory;

/**
 * Factory for creating different interpolation strategies.
 */
public class GeoInterpolationStrategyFactory {

  /**
   * Available interpolation methods.
   */
  public enum InterpolationMethod {
    NEAREST_NEIGHBOR,
    BILINEAR,
    WEIGHTED_AVERAGE
  }

  private final EngineValueFactory valueFactory;

  /**
   * Constructs an InterpolationStrategyFactory with the specified value factory.
   *
   * @param valueFactory Factory for creating EngineValue objects
   */
  public GeoInterpolationStrategyFactory(EngineValueFactory valueFactory) {
    this.valueFactory = valueFactory;
  }

  /**
   * Creates an interpolation strategy based on the specified method.
   *
   * @param method The interpolation method to use
   * @return The appropriate interpolation strategy
   */
  public GeoInterpolationStrategy createStrategy(InterpolationMethod method) {
    switch (method) {
      case BILINEAR:
        throw new UnsupportedOperationException(
            "Bilinear interpolation is not supported yet.");
      case WEIGHTED_AVERAGE:
        throw new UnsupportedOperationException(
            "Weighted average interpolation is not supported yet.");
      case NEAREST_NEIGHBOR:
      default:
        return new NearestNeighborInterpolationStrategy();
    }
  }
}
