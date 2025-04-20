package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.geotools.api.geometry.MismatchedDimensionException;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.GeneralPosition;
import org.geotools.referencing.CRS;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.geo.geometry.EarthGeometryFactory;

/**
 * Utility responsible for building grid structures in Earth space.
 *
 * <p>Utility creating a rectangular grid of patches based on coordinates in any coordinate
 * reference system, converting them to the target CRS if needed.</p>
 */
public class EarthPatchBuilder implements PatchBuilder {

  private final BigDecimal cellWidth;
  private final EntityPrototype prototype;
  private final EngineGeometryFactory geometryFactory;

  // CRS-related fields
  private CoordinateReferenceSystem inputCoordinateReferenceSystem;
  private CoordinateReferenceSystem targetCoordinateReferenceSystem;

  // Transformed coordinates stored directly as Coordinates
  private GeneralPosition topLeftTransformed;
  private GeneralPosition bottomRightTransformed;

  /**
   * Creates a new PatchBuilder with specified input and target CRS, and corner coordinates.
   *
   * @param inputCrs input CRS
   * @param targetCrs target CRS
   * @param extents Structure describing the extents or bounds of the grid to be built.
   * @param cellWidth The width of each cell in the grid (in units of the target CRS)
   * @param prototype The entity prototype used to create grid cells
   * @throws FactoryException if any CRS code is invalid
   * @throws TransformException if coordinate transformation fails
   */
  public EarthPatchBuilder(
      CoordinateReferenceSystem inputCrs,
      CoordinateReferenceSystem targetCrs,
      PatchBuilderExtents extents,
      BigDecimal cellWidth,
      EntityPrototype prototype
  ) throws TransformException {

    this.prototype = prototype;
    // Validate cell width
    if (cellWidth == null || cellWidth.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Cell width must be positive");
    }
    this.cellWidth = cellWidth;
    this.inputCoordinateReferenceSystem = inputCrs;
    this.targetCoordinateReferenceSystem = targetCrs;

    geometryFactory = new EarthGeometryFactory(targetCrs);

    // Force longitude/latitude ordering for geographic CRS
    inputCoordinateReferenceSystem = CRS.getHorizontalCRS(inputCoordinateReferenceSystem);
    targetCoordinateReferenceSystem = CRS.getHorizontalCRS(targetCoordinateReferenceSystem);

    // Validate corners
    validateCornerCoordinates(
        extents.getTopLeftX(),
        extents.getTopLeftY(),
        extents.getBottomRightX(),
        extents.getBottomRightY()
    );

    // Transform coordinates immediately
    transformCornerCoordinates(
        extents.getTopLeftX(),
        extents.getTopLeftY(),
        extents.getBottomRightX(),
        extents.getBottomRightY()
    );
  }

  /**
   * Builds and returns a PatchSet based on the transformed coordinates.
   *
   * @return a new PatchSet instance
   */
  public PatchSet build() {
    try {
      // Validate all required parameters first
      validateParameters();

      // Calculate grid dimensions
      int colCells = getColCells();
      int rowCells = getRowCells();
      double cellWidthDouble = cellWidth.doubleValue();

      // Create all patches
      List<MutableEntity> patches = createPatchGrid(colCells, rowCells, cellWidthDouble);
      return new PatchSet(patches, cellWidth);

    } catch (Exception e) {
      throw new RuntimeException("Failed to build grid: " + e.getMessage(), e);
    }
  }

  /**
   * Validates corner coordinates based on coordinate system type.
   * For both geographic and projected coordinates, we expect Y to increase northward
   * and X to increase eastward.
   */
  private void validateCornerCoordinates(
      BigDecimal topLeftX,
      BigDecimal topLeftY,
      BigDecimal bottomRightX,
      BigDecimal bottomRightY
  ) {

    if (topLeftX == null || topLeftY == null || bottomRightX == null || bottomRightY == null) {
      throw new IllegalArgumentException("Missing corner coordinates");
    }

    // Consistent validation for both geographic and projected coordinates
    // Y-coordinate (latitude/northing) should decrease from top to bottom
    if (topLeftY.compareTo(bottomRightY) <= 0) {
      throw new IllegalArgumentException(
          "Top-left Y-coordinate must be greater than bottom-right Y-coordinate");
    }

    // X-coordinate (longitude/easting) should increase from left to right
    if (topLeftX.compareTo(bottomRightX) >= 0) {
      throw new IllegalArgumentException(
          "Top-left X-coordinate must be less than bottom-right X-coordinate");
    }
  }

  /**
   * Transforms corner coordinates from input CRS to target CRS.
   */
  private void transformCornerCoordinates(
      BigDecimal topLeftX,
      BigDecimal topLeftY,
      BigDecimal bottomRightX,
      BigDecimal bottomRightY
  ) throws TransformException {

    // Create DirectPosition2D objects for the corners using x,y order (longitude,latitude)
    GeneralPosition topLeft = new GeneralPosition(topLeftX.doubleValue(), topLeftY.doubleValue());
    topLeft.setCoordinateReferenceSystem(inputCoordinateReferenceSystem);

    GeneralPosition bottomRight = new GeneralPosition(
        bottomRightX.doubleValue(),
        bottomRightY.doubleValue()
    );
    bottomRight.setCoordinateReferenceSystem(inputCoordinateReferenceSystem);

    GeneralPosition[] corners = {topLeft, bottomRight};

    // Transform the corners
    GeneralPosition[] transformed = transformCornerCoordinates(
        corners,
        inputCoordinateReferenceSystem,
        targetCoordinateReferenceSystem
    );

    // Store the transformed coordinates, with appropriate CRS
    this.topLeftTransformed = transformed[0];
    this.topLeftTransformed.setCoordinateReferenceSystem(targetCoordinateReferenceSystem);
    this.bottomRightTransformed = transformed[1];
    this.bottomRightTransformed.setCoordinateReferenceSystem(targetCoordinateReferenceSystem);
  }

  /**
   * Transforms corner coordinates to a target CRS.
   *
   * @param corners Array of GeneralPosition objects representing corner points
   * @param sourceCrs The source Coordinate Reference System
   * @param targetCrs The target Coordinate Reference System
   * @return Array of transformed GeneralPosition objects
   * @throws TransformException if transformation fails
   * @throws MismatchedDimensionException if dimensions don't match
   */
  public GeneralPosition[] transformCornerCoordinates(
      GeneralPosition[] corners,
      CoordinateReferenceSystem sourceCrs,
      CoordinateReferenceSystem targetCrs
  ) throws MismatchedDimensionException, TransformException {
    // Get the transformation between the two CRS
    MathTransform transform;
    try {
      transform = CRS.findMathTransform(sourceCrs, targetCrs, true);
    } catch (FactoryException e) {
      throw new TransformException("Failed to find transformation between CRS: " + e.getMessage());
    }

    GeneralPosition[] transformedCorners = new GeneralPosition[corners.length];

    for (int i = 0; i < corners.length; i++) {
      // Create a new position for the result
      GeneralPosition result = new GeneralPosition(targetCrs);
      transform.transform(corners[i], result);
      transformedCorners[i] = result;
    }

    return transformedCorners;
  }

  private int getRowCells() {
    double rowDiff = topLeftTransformed.getOrdinate(1) - bottomRightTransformed.getOrdinate(1);
    int rowCells = (int) Math.ceil(rowDiff / cellWidth.doubleValue());
    return rowCells;
  }

  private int getColCells() {
    double colDiff = bottomRightTransformed.getOrdinate(0) - topLeftTransformed.getOrdinate(0);
    int colCells = (int) Math.ceil(colDiff / cellWidth.doubleValue());
    return colCells;
  }

  /**
   * Creates a single patch EngineGeometry for a specific cell.
   */
  private EngineGeometry createCellGeometry(
      double leftX, double topY,
      double rightX, double bottomY
  ) {
    return geometryFactory.createSquare(
        BigDecimal.valueOf(leftX),
        BigDecimal.valueOf(topY),
        BigDecimal.valueOf(rightX),
        BigDecimal.valueOf(bottomY)
    );
  }

  /**
   * Creates all patches in the grid.
   */
  private List<MutableEntity> createPatchGrid(int colCells, int rowCells, double cellWidth) {
    List<MutableEntity> patches = new ArrayList<>();
    for (int rowIdx = 0; rowIdx < rowCells; rowIdx++) {
      for (int colIdx = 0; colIdx < colCells; colIdx++) {
        double cellTopLeftX =
            topLeftTransformed.getOrdinate(0) + (colIdx * cellWidth);
        double cellTopLeftY =
            topLeftTransformed.getOrdinate(1) - (rowIdx * cellWidth);

        double cellBottomRightX = cellTopLeftX + cellWidth;
        double cellBottomRightY = cellTopLeftY - cellWidth;

        // Ensure we don't exceed grid boundaries
        cellBottomRightX = Math.min(cellBottomRightX, bottomRightTransformed.getOrdinate(0));
        cellBottomRightY = Math.max(cellBottomRightY, bottomRightTransformed.getOrdinate(1));

        // Create geometry for this cell
        EngineGeometry cellGeometry = createCellGeometry(
            cellTopLeftX, cellTopLeftY,
            cellBottomRightX, cellBottomRightY
        );

        if (cellGeometry != null) {
          MutableEntity patch = prototype.buildSpatial(cellGeometry);
          patches.add(patch);
        }
      }
    }

    return patches;
  }

  /**
   * Validates that all required objects are properly initialized.
   */
  private void validateParameters() {
    if (topLeftTransformed == null || bottomRightTransformed == null) {
      throw new IllegalStateException("Corner coordinates not transformed");
    }

    if (cellWidth == null) {
      throw new IllegalStateException("Cell width not specified");
    }

    if (inputCoordinateReferenceSystem == null) {
      throw new IllegalStateException("Input CRS not specified");
    }

    if (targetCoordinateReferenceSystem == null) {
      throw new IllegalStateException("Target CRS not specified");
    }

    if (cellWidth.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Cell width must be positive");
    }

    // Validate that after transformation, the coordinates still make sense
    if (topLeftTransformed.getOrdinate(1) <= bottomRightTransformed.getOrdinate(1)) {
      throw new IllegalArgumentException(
          "After transformation, top-left Y-coord must be greater than bottom-right Y-coord");
    }

    if (topLeftTransformed.getOrdinate(0) >= bottomRightTransformed.getOrdinate(0)) {
      throw new IllegalArgumentException(
          "After transformation, top-left X-coord must be less than bottom-right X-coord");
    }
  }
}
