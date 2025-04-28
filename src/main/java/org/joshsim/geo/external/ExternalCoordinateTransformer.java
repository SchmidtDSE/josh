package org.joshsim.geo.external;

import java.math.BigDecimal;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;

/**
 * Interface for coordinate transformation between different coordinate systems.
 */
public interface ExternalCoordinateTransformer {
  /**
   * Transforms coordinates from patch grid space to data source coordinate system.
   *
   * @param patchX X coordinate in patch grid
   * @param patchY Y coordinate in patch grid
   * @param gridCrs Definition of the patch grid coordinate system
   * @param sourceDimensions Dimensions of the data source
   * @return Transformed coordinates in data source coordinate system [x, y]
   * @throws Exception If transformation fails
   */
  BigDecimal[] transformPatchToDataCoordinates(
          BigDecimal patchX, 
          BigDecimal patchY,
          GridCrsDefinition gridCrs,
          ExternalSpatialDimensions sourceDimensions) throws Exception;
  
  /**
   * Transforms coordinates from data source coordinate system to patch grid space.
   *
   * @param dataX X coordinate in data source
   * @param dataY Y coordinate in data source
   * @param gridCrs Definition of the patch grid coordinate system
   * @param sourceDimensions Dimensions of the data source
   * @return Transformed coordinates in patch grid space [x, y]
   * @throws Exception If transformation fails
   */
  BigDecimal[] transformDataToPatchCoordinates(
          BigDecimal dataX, 
          BigDecimal dataY,
          GridCrsDefinition gridCrs,
          ExternalSpatialDimensions sourceDimensions) throws Exception;
}