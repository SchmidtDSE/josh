package org.joshsim.geo.external;

import java.math.BigDecimal;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;


/**
 * Coordinate transformer that simply passes values through as is.
 */
public class NoopExternalCoordinateTransformer implements ExternalCoordinateTransformer {

  @Override
  public BigDecimal[] transformPatchToDataCoordinates(
      BigDecimal patchX,
      BigDecimal patchY,
      GridCrsDefinition gridCrs,
      ExternalSpatialDimensions sourceDimensions
  ) throws Exception {

    // Otherwise, transform between CRSes using a CRS transformation service
    return new BigDecimal[] {
        patchX,
        patchY
    };
  }

  @Override
  public BigDecimal[] transformDataToPatchCoordinates(
      BigDecimal patchX,
      BigDecimal patchY,
      GridCrsDefinition gridCrs,
      ExternalSpatialDimensions sourceDimensions
  ) throws Exception {

    // Otherwise, transform between CRSes using a CRS transformation service
    return new BigDecimal[] {
        patchX,
        patchY
    };
  }

}
