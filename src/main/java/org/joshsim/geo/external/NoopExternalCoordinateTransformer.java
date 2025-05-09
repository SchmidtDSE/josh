package org.joshsim.geo.external;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.apache.sis.referencing.CRS;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.joshsim.geo.geometry.JtsTransformUtility;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;


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
