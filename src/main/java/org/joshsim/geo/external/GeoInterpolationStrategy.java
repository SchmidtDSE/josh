package org.joshsim.geo.external;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Strategy interface for different geospatial data interpolation methods.
 * Implementations define how values are interpolated from data sources to patches.
 */
public interface GeoInterpolationStrategy {
  
  /**
   * Interpolates a value from a data source for a specific patch.
   *
   * @param patch The patch to interpolate a value for
   * @param variableName The name of the variable to extract
   * @param timeStep The time step to process
   * @param gridCrsDefinition Grid CRS definition
   * @param transformer Coordinate transformer
   * @param reader Data source reader
   * @param dimensions Dimensions of the data source
   * @return Optional containing the interpolated EngineValue if available
   * @throws IOException If there's an error reading the data
   */
  Optional<EngineValue> interpolateValue(
      MutableEntity patch,
      String variableName,
      int timeStep,
      GridCrsDefinition gridCrsDefinition,
      ExternalCoordinateTransformer transformer,
      ExternalDataReader reader,
      ExternalSpatialDimensions dimensions) throws IOException;
  
  /**
   * Creates a GeoKey for the patch.
   *
   * @param patch The patch to create a key for
   * @return A GeoKey for the patch
   */
  default GeoKey getGeoKey(MutableEntity patch) {
    return patch.getKey().orElseThrow();
  }
}