package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.GeneralPosition;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.joshsim.engine.entity.type.Patch;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.geotools.api.geometry.MismatchedDimensionException;
import org.geotools.api.referencing.FactoryException;
import org.opengis.referencing.crs.GeographicCRS;
import org.geotools.api.referencing.operation.TransformException;

/**
 * This class is responsible for building grid structures.
 * It creates a rectangular grid of patches based on coordinates in any coordinate reference system,
 * converting them to the target CRS if needed.
 */
public class GridBuilder {
  private BigDecimal cellWidth;

  // CRS-related fields
  private CoordinateReferenceSystem inputCoordinateReferenceSystem;
  private CoordinateReferenceSystem targetCoordinateReferenceSystem;

  // Transformed coordinates stored directly as Coordinates
  private GeneralPosition topLeftTransformed;
  private GeneralPosition bottomRightTransformed;

  /**
   * Creates a new GridBuilder with specified input and target CRS, and corner coordinates.
   *
   * @param inputCrsCode EPSG code for the input CRS
   * @param targetCrsCode EPSG code for the target CRS
   * @param cornerCoords Map containing corner coordinates with keys like "topLeftX", "topLeftY",
   *          "bottomRightX", "bottomRightY"
   * @param cellWidth The width of each cell in the grid (in units of the target CRS)
   * @throws FactoryException if any CRS code is invalid
   * @throws TransformException if coordinate transformation fails
   */
  public GridBuilder(String inputCrsCode, String targetCrsCode,
            Map<String, BigDecimal> cornerCoords, BigDecimal cellWidth)
      throws FactoryException, TransformException {

    // Validate cell width
    if (cellWidth == null || cellWidth.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Cell width must be positive");
    }
    this.cellWidth = cellWidth;

    // Set up CRS
    inputCoordinateReferenceSystem = CRS.decode(inputCrsCode);
    targetCoordinateReferenceSystem = CRS.decode(targetCrsCode);

    // Force longitude/latitude ordering for geographic CRS
    inputCoordinateReferenceSystem = CRS.getHorizontalCRS(inputCoordinateReferenceSystem);
    targetCoordinateReferenceSystem = CRS.getHorizontalCRS(targetCoordinateReferenceSystem);

    // Extract with consistent X,Y keys regardless of CRS type
    BigDecimal topLeftX = cornerCoords.get("topLeftX");
    BigDecimal topLeftY = cornerCoords.get("topLeftY");
    BigDecimal bottomRightX = cornerCoords.get("bottomRightX");
    BigDecimal bottomRightY = cornerCoords.get("bottomRightY");

    // Validate corners
    validateCornerCoordinates(topLeftX, topLeftY, bottomRightX, bottomRightY);

    // Transform coordinates immediately
    transformCornerCoordinates(topLeftX, topLeftY, bottomRightX, bottomRightY);
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

  /**
   * Builds and returns a Grid based on the transformed coordinates.
   *
   * @return a new Grid instance
   */
  public Grid build() {
    try {
      // Validate all required parameters first
      validateParameters();

      // Calculate grid dimensions
      GridDimensions dimensions = calculateGridDimensions();

      // Create all patches
      List<Patch> patches = createPatchGrid(dimensions);

      return new Grid(patches, cellWidth);
    } catch (Exception e) {
      throw new RuntimeException("Failed to build grid: " + e.getMessage(), e);
    }
  }

  private boolean isGeographic() {
    return targetCoordinateReferenceSystem instanceof GeographicCRS;
  }

  private static class GridDimensions {
    final int colCells;
    final int rowCells;
    final double cellWidthUnits;

    GridDimensions(int colCells, int rowCells, double cellWidthUnits) {
      this.colCells = colCells;
      this.rowCells = rowCells;
      this.cellWidthUnits = cellWidthUnits;
    }
  }

  private GridDimensions calculateGridDimensions() {
    double cellWidthUnits = this.cellWidth.doubleValue();

    double colDiff = bottomRightTransformed.getOrdinate(0) - topLeftTransformed.getOrdinate(0);
    double rowDiff = topLeftTransformed.getOrdinate(1) - bottomRightTransformed.getOrdinate(1);

    int rowCells = (int) Math.ceil(rowDiff / cellWidthUnits);
    int colCells = (int) Math.ceil(colDiff / cellWidthUnits);

    return new GridDimensions(colCells, rowCells, cellWidthUnits);
  }

  /**
   * Creates a single patch EngineGeometry for a specific cell.
   */
  private EngineGeometry createCellGeometry(
      double leftX, double topY,
      double rightX, double bottomY
  ) {
    return GeometryFactory.createSquare(
      BigDecimal.valueOf(leftX),
      BigDecimal.valueOf(topY),
      BigDecimal.valueOf(rightX),
      BigDecimal.valueOf(bottomY),
      targetCoordinateReferenceSystem
    );
  }

  /**
   * Creates all patches in the grid.
   */
  private List<Patch> createPatchGrid(GridDimensions dimensions) {
    List<Patch> patches = new ArrayList<>();
    for (int rowIdx = 0; rowIdx < dimensions.rowCells; rowIdx++) {
      for (int colIdx = 0; colIdx < dimensions.colCells; colIdx++) {
        double cellTopLeftX = 
            topLeftTransformed.getOrdinate(0) + (colIdx * dimensions.cellWidthUnits);
        double cellTopLeftY =
            topLeftTransformed.getOrdinate(1) - (rowIdx * dimensions.cellWidthUnits);

        double cellBottomRightX = cellTopLeftX + dimensions.cellWidthUnits;
        double cellBottomRightY = cellTopLeftY - dimensions.cellWidthUnits;

        // Ensure we don't exceed grid boundaries
        cellBottomRightX = Math.min(cellBottomRightX, bottomRightTransformed.getOrdinate(0));
        cellBottomRightY = Math.max(cellBottomRightY, bottomRightTransformed.getOrdinate(1));

        // Create geometry for this cell
        EngineGeometry cellGeometry = createCellGeometry(
            cellTopLeftX, cellTopLeftY,
            cellBottomRightX, cellBottomRightY
        );

        if (cellGeometry != null) {
          String cellName = String.format("cell_%d_%d", rowIdx, colIdx);
          Patch patch = new Patch(
              cellGeometry,
              cellName,
              null,
              null
          );
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