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
 * Default implementation of ExternalCoordinateTransformer.
 * Performs basic transformations between patch grid coordinates and data coordinates.
 */
public class GridExternalCoordinateTransformer implements ExternalCoordinateTransformer {

  @Override
  public BigDecimal[] transformPatchToDataCoordinates(
      BigDecimal patchX,
      BigDecimal patchY,
      GridCrsDefinition gridCrs,
      ExternalSpatialDimensions sourceDimensions
  ) throws Exception {

    // First, transform from grid to CRS coordinates
    BigDecimal[] crsCoords = gridCrs.gridToCrsCoordinates(patchX, patchY);

    // If grid CRS and data CRS are the same, return directly
    if (gridCrs.getBaseCrsCode().equals(sourceDimensions.getCrs())) {
      return crsCoords;
    }

    // Otherwise, transform between CRSes using a CRS transformation service
    return transformCoordinatesBetweenCrs(
        crsCoords[0],
        crsCoords[1],
        gridCrs.getBaseCrsCode(),
        sourceDimensions.getCrs()
    );
  }

  @Override
  public BigDecimal[] transformDataToPatchCoordinates(
      BigDecimal dataX,
      BigDecimal dataY,
      GridCrsDefinition gridCrs,
      ExternalSpatialDimensions sourceDimensions) throws Exception {

    // If grid CRS and data CRS are the same, transform directly
    if (gridCrs.getBaseCrsCode().equals(sourceDimensions.getCrs())) {
      return gridCrs.crsToGridCoordinates(dataX, dataY);
    }

    // Otherwise, first transform between CRSes
    BigDecimal[] gridCrsCoords = transformCoordinatesBetweenCrs(
        dataX,
        dataY,
        sourceDimensions.getCrs(),
        gridCrs.getBaseCrsCode()
    );

    // Then transform from CRS to grid coordinates
    return gridCrs.crsToGridCoordinates(gridCrsCoords[0], gridCrsCoords[1]);
  }

  /**
   * Transforms coordinates between different coordinate reference systems.
   *
   * @param x X coordinate
   * @param y Y coordinate
   * @param sourceCrs Source CRS code
   * @param targetCrs Target CRS code
   * @return Transformed coordinates [x, y]
   * @throws Exception If transformation fails
   */
  private BigDecimal[] transformCoordinatesBetweenCrs(
      BigDecimal x,
      BigDecimal y,
      String sourceCrs,
      String targetCrs
  ) throws Exception {
    // Skip transformation if CRS codes are identical
    if (sourceCrs.equals(targetCrs)) {
      return new BigDecimal[] {x, y};
    }

    try {
      // Get right-handed coordinate reference systems for both source and target
      CoordinateReferenceSystem sourceReference = JtsTransformUtility.getRightHandedCrs(sourceCrs);
      CoordinateReferenceSystem targetReference = JtsTransformUtility.getRightHandedCrs(targetCrs);

      // Create the transform between the two CRS
      MathTransform transform = CRS.findOperation(sourceReference, targetReference, null)
          .getMathTransform();

      // Create a JTS point from the input coordinates
      Point sourcePoint = JtsTransformUtility.createJtsPoint(x.doubleValue(), y.doubleValue());

      // Transform the point
      Point transformedPoint = (Point) JtsTransformUtility.transform(sourcePoint, transform);

      // Convert back to BigDecimal with reasonable precision (6 decimal places)
      return new BigDecimal[] {
          new BigDecimal(transformedPoint.getX()).setScale(6, RoundingMode.HALF_UP),
          new BigDecimal(transformedPoint.getY()).setScale(6, RoundingMode.HALF_UP)
      };
    } catch (FactoryException e) {
      throw new Exception("Failed to create CRS transformation: " + e.getMessage(), e);
    } catch (TransformException e) {
      throw new Exception("Failed to transform coordinates: " + e.getMessage(), e);
    }
  }
}
