package org.joshsim.geo.external;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Nearest neighbor interpolation strategy.
 * Returns the value of the cell that is closest to the patch center point.
 */
public class NearestNeighborInterpolationStrategy implements GeoInterpolationStrategy {

  @Override
  public Optional<EngineValue> interpolateValue(
      MutableEntity patch,
      String variableName,
      int timeStep,
      GridCrsDefinition gridCrsDefinition,
      ExternalCoordinateTransformer transformer,
      ExternalDataReader reader,
      ExternalSpatialDimensions dimensions) throws IOException {

    Optional<EngineGeometry> geometryOpt = patch.getGeometry();
    if (geometryOpt.isEmpty()) {
      return Optional.empty();
    }

    EngineGeometry geometry = geometryOpt.get();

    // Get center point of patch
    BigDecimal patchX = geometry.getCenterX();
    BigDecimal patchY = geometry.getCenterY();

    try {
      // Transform patch coordinates to data coordinates
      BigDecimal[] dataCoords = transformer.transformPatchToDataCoordinates(
          patchX, patchY, gridCrsDefinition, dimensions);

      // Read value at the nearest neighbor point
      return reader.readValueAt(variableName, dataCoords[0], dataCoords[1], timeStep);
    } catch (Exception e) {
      throw new IOException("Failed to interpolate value: " + e.getMessage(), e);
    }
  }
}
