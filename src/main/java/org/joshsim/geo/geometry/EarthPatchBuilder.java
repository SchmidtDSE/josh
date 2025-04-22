package org.joshsim.geo.geometry;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CRS;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.PatchBuilder;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchSet;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

/**
 * Utility responsible for building grid structures in Earth space.
 *
 * <p>Utility creating a rectangular grid of patches based on coordinates in any coordinate
 * reference system, converting them to the target CRS if needed.</p>
 */
public class EarthPatchBuilder implements PatchBuilder {

  private static final int ESTIMATED_CELLS_WARNING_SIZE = 1_000_000;
  private final BigDecimal cellWidth;
  private final EntityPrototype prototype;
  private final EngineGeometryFactory geometryFactory;

  // CRS-related fields
  private CoordinateReferenceSystem inputCoordinateReferenceSystem;
  private CoordinateReferenceSystem targetCoordinateReferenceSystem;

  // Transformed coordinates stored directly as Coordinates
  private DirectPosition2D topLeftTransformed;
  private DirectPosition2D bottomRightTransformed;

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
    this.cellWidth = cellWidth;
    this.inputCoordinateReferenceSystem = inputCrs;
    this.targetCoordinateReferenceSystem = targetCrs;

    geometryFactory = new EarthGeometryFactory(targetCoordinateReferenceSystem);

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
    DirectPosition2D topLeft = new DirectPosition2D(
        inputCoordinateReferenceSystem,
        topLeftX.doubleValue(),
        topLeftY.doubleValue()
    );

    DirectPosition2D bottomRight = new DirectPosition2D(
        inputCoordinateReferenceSystem,
        bottomRightX.doubleValue(),
        bottomRightY.doubleValue()
    );

    DirectPosition2D[] corners = {topLeft, bottomRight};

    // Transform the corners
    DirectPosition2D[] transformed = transformCornerCoordinates(
        corners,
        inputCoordinateReferenceSystem,
        targetCoordinateReferenceSystem
    );

    // Store the transformed coordinates
    this.topLeftTransformed = transformed[0];
    this.bottomRightTransformed = transformed[1];
  }

  /**
   * Transforms corner coordinates to a target CRS.
   *
   * @param corners Array of DirectPosition2D objects representing corner points
   * @param sourceCrs The source Coordinate Reference System
   * @param targetCrs The target Coordinate Reference System
   * @return Array of transformed DirectPosition2D objects
   * @throws TransformException if transformation fails
   * @throws MismatchedDimensionException if dimensions don't match
   */
  public DirectPosition2D[] transformCornerCoordinates(
      DirectPosition2D[] corners,
      CoordinateReferenceSystem sourceCrs,
      CoordinateReferenceSystem targetCrs
  ) throws MismatchedDimensionException, TransformException {
    // Get the transformation between the two CRS
    MathTransform transform;
    try {
      transform = CRS.findOperation(sourceCrs, targetCrs, null).getMathTransform();
    } catch (FactoryException e) {
      throw new TransformException("Failed to find transformation between CRS: " + e.getMessage());
    }

    DirectPosition2D[] transformedCorners = new DirectPosition2D[corners.length];

    for (int i = 0; i < corners.length; i++) {
      // Create a new position for the result with the target CRS
      DirectPosition2D result = new DirectPosition2D(targetCrs);
      transform.transform(corners[i], result);
      transformedCorners[i] = result;
    }

    return transformedCorners;
  }

  private int getRowCells() {
    double rowDiff = topLeftTransformed.getY() - bottomRightTransformed.getY();
    int rowCells = (int) Math.ceil(rowDiff / cellWidth.doubleValue());
    return rowCells;
  }

  private int getColCells() {
    double colDiff = bottomRightTransformed.getX() - topLeftTransformed.getX();
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
        double cellTopLeftX = topLeftTransformed.getX() + (colIdx * cellWidth);
        double cellTopLeftY = topLeftTransformed.getY() - (rowIdx * cellWidth);

        double cellBottomRightX = cellTopLeftX + cellWidth;
        double cellBottomRightY = cellTopLeftY - cellWidth;

        // Ensure we don't exceed grid boundaries
        cellBottomRightX = Math.min(cellBottomRightX, bottomRightTransformed.getX());
        cellBottomRightY = Math.max(cellBottomRightY, bottomRightTransformed.getY());

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
   * Validates that all required objects are properly initialized and that
   * the target CRS is projected as required for area/distance calculations.
   */
  private void validateParameters() {
    if (topLeftTransformed == null || bottomRightTransformed == null) {
      throw new IllegalStateException("Corner coordinates not transformed");
    }

    if (cellWidth == null || cellWidth.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Cell width must be positive");
    }

    if (inputCoordinateReferenceSystem == null) {
      throw new IllegalStateException("Input CRS not specified");
    }

    if (targetCoordinateReferenceSystem == null) {
      throw new IllegalStateException("Target CRS not specified");
    }

    // Ensure target CRS is projected (not geographic) for proper distance calculations
    if (targetCoordinateReferenceSystem instanceof GeographicCRS) {
      throw new IllegalArgumentException(
          "Target CRS must be projected for accurate area/distance calculations");
    }

    // Verify coordinate orientation after transformation
    if (topLeftTransformed.getY() <= bottomRightTransformed.getY()) {
      throw new IllegalArgumentException(
          "After transformation, top-left Y-coord must be greater than bottom-right Y-coord");
    }

    if (topLeftTransformed.getX() >= bottomRightTransformed.getX()) {
      throw new IllegalArgumentException(
          "After transformation, top-left X-coord must be less than bottom-right X-coord");
    }

    // Verify reasonable cell size for the target CRS
    // Assumes most projected CRSs use meters, but doesn't enforce specific units
    double cellWidthDouble = cellWidth.doubleValue();
    double gridWidth = bottomRightTransformed.getX() - topLeftTransformed.getX();
    double gridHeight = topLeftTransformed.getY() - bottomRightTransformed.getY();

    // Warn if grid dimensions suggest unreasonably many cells
    int estimatedCells =
        (int) (gridWidth / cellWidthDouble) * (int) (gridHeight / cellWidthDouble);
    if (estimatedCells > ESTIMATED_CELLS_WARNING_SIZE) {
      // This is just a warning since some applications might legitimately need large grids
      System.err.println("Warning: Grid configuration will create approximately "
          + estimatedCells + " cells, which may impact performance");
    }
}
}
