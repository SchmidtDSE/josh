package org.joshsim.geo.geometry;

import java.io.IOException;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.operation.transform.DefaultMathTransformFactory;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Real implementation of Grid CRS using direct construction of coordinate reference systems.
 * Uses a `Transverse Mercator` projection or affine transformation depending on the base CRS,
 * with a custom origin to minimize distortion in the area of interest.
 */
public class GridCrsManager {
  private final GridCrsDefinition definition;
  // private final CoordinateReferenceSystem gridProjectedCrs;
  private final CoordinateReferenceSystem baseCrs;

  // SIS factories
  // private final GeodeticObjectFactory geodeticObjectFactory = new GeodeticObjectFactory();
  private final MathTransformFactory mathTransformFactory = new DefaultMathTransformFactory();
  private final MathTransform gridToBaseTransform;
  private final MathTransform baseToGridTransform;

  /**
  * Creates a realized grid CRS from a grid CRS definition.
  *
  * @param definition The grid CRS definition
  * @throws FactoryException If CRS creation fails
  * @throws TransformException if transformation fails
  */
  public GridCrsManager(GridCrsDefinition definition)
      throws FactoryException, IOException, TransformException {
    this.definition = definition;

    // Create the base CRS from the provided definition
    this.baseCrs = JtsTransformUtility.getRightHandedCrs(definition.getBaseCrsCode());

    // Initialize the transforms between grid CRS and base CRS
    this.gridToBaseTransform = createGridToBaseTransform();

    // Initialize the transform from base CRS coordinates back to to grid coordinates
    try {
      this.baseToGridTransform = gridToBaseTransform.inverse();
    } catch (NoninvertibleTransformException e) {
      throw new TransformException("Failed to create inverse transform: " + e.getMessage(), e);
    }
  }

  /**
   * Creates a transform from grid coordinates to base CRS coordinates.
   *
   * @return A math transform for converting grid coordinates to base CRS coordinates
   * @throws FactoryException If the transform cannot be created
   */
  private MathTransform createGridToBaseTransform() throws FactoryException {
    PatchBuilderExtents extents = definition.getExtents();
    double topLeftX = extents.getTopLeftX().doubleValue(); // longitude in degrees
    double topLeftY = extents.getTopLeftY().doubleValue(); // latitude in degrees
    double cellSize = getCellSize(); // in meters

    if (baseCrs instanceof GeographicCRS) {
      // For geographic CRS, we need to:
      // 1. First establish a local projected space centered at our grid origin
      // 2. Scale grid indices to this projected space (in meters)
      // 3. Then convert back to geographic coordinates

      // Step 1: Create a projection transform from geographic to local projected space
      ParameterValueGroup tmParams =
          mathTransformFactory.getDefaultParameters("Transverse Mercator");
      tmParams.parameter("central_meridian").setValue(topLeftX);
      tmParams.parameter("latitude_of_origin").setValue(topLeftY);
      tmParams.parameter("scale_factor").setValue(1.0);
      tmParams.parameter("false_easting").setValue(0.0);
      tmParams.parameter("false_northing").setValue(0.0);

      Ellipsoid ellipsoid = ((GeographicCRS) baseCrs).getDatum().getEllipsoid();
      tmParams.parameter("semi_major").setValue(ellipsoid.getSemiMajorAxis());
      tmParams.parameter("semi_minor").setValue(ellipsoid.getSemiMinorAxis());

      // The forward transform: Geographic → Local Projected
      MathTransform geoToLocal = mathTransformFactory.createParameterizedTransform(tmParams);

      // Step 2: Create transform from grid indices to local projected coordinates (meters)
      // This is a simple scaling: grid indices × cell size = meters
      // With Y flipped because grid Y increases downward, but projected Y increases upward
      LinearTransform gridToMeters = MathTransforms.scale(cellSize, -cellSize);

      // Step 3: Create the inverse projection: Local Projected → Geographic
      MathTransform localToGeo;
      try {
        localToGeo = geoToLocal.inverse();
      } catch (NoninvertibleTransformException e) {
        throw new FactoryException("Failed to invert projection transform", e);
      }

      // Step 4: Concatenate the transforms:
      // Grid indices → Local projected (meters) → Geographic coordinates
      return MathTransforms.concatenate(gridToMeters, localToGeo);
    } else {
      // For projected base CRS, create and concatenate transforms:
      // 1. Scale by cell size (with Y flipped)
      LinearTransform scaleTransform = MathTransforms.scale(cellSize, -cellSize);

      // 2. Translate to the top-left corner
      LinearTransform translateTransform = MathTransforms.translation(topLeftX, topLeftY);

      // 3. Concatenate the transforms (scale first, then translate)
      return MathTransforms.concatenate(scaleTransform, translateTransform);
    }
  }

  /**
   * Gets the base Earth coordinate reference system.
   *
   * @return The base CRS (typically geographic)
   */
  public CoordinateReferenceSystem getBaseCrs() {
    return baseCrs;
  }

  /**
   * Creates a transform from grid coordinates to any target CRS.
   *
   * @param targetCrs The target coordinate reference system
   * @return A math transform for coordinate conversion
   * @throws FactoryException If the transform cannot be created
   * @throws TransformException if the transformation fails
   */
  public MathTransform createGridToTargetCrsTransform(CoordinateReferenceSystem targetCrs)
      throws FactoryException {
    if (targetCrs.equals(baseCrs)) {
      // If target is our base CRS, we can use the pre-computed transform directly
      return gridToBaseTransform;
    } else {
      // For different target CRS, we need to concatenate transforms:
      // 1. Convert from grid to base CRS using pre-computed transform
      // 2. Convert from base CRS to target CRS

      // Create a coordinate operation between base CRS and target CRS
      // CoordinateOperation operation = coordinateOpsFactory.createOperation(baseCrs, targetCrs);

      CoordinateOperation operation = CRS.findOperation(baseCrs, targetCrs, null);
      MathTransform baseCrsToTargetTransform = operation.getMathTransform();

      // Concatenate the transforms
      return MathTransforms.concatenate(gridToBaseTransform, baseCrsToTargetTransform);
    }
  }

  /**
   * Gets the grid CRS definition used to create this realized CRS.
   *
   * @return The grid CRS definition
   */
  public GridCrsDefinition getDefinition() {
    return definition;
  }

  /**
   * Gets the cell size in CRS units after any conversion.
   *
   * @return The cell size in CRS units
   */
  public double getCellSize() {
    return getDefinition().getCellSize().doubleValue();
  }
}
