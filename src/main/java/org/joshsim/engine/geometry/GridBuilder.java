package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralDirectPosition;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.AbstractCRS;
import org.apache.sis.referencing.cs.AxesConvention;
import org.joshsim.engine.entity.type.Patch;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.SpatialContextFactory;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;


/**
 * This class is responsible for building grid structures.
 * It creates a rectangular grid of patches based on coordinates in any coordinate reference system,
 * converting them to the target CRS if needed.
 */
public class GridBuilder {
  private BigDecimal cellWidth;

  // CRS-related fields
  private boolean usingVirutalCoordinates;
  private CoordinateReferenceSystem inputCoordinateReferenceSystem;
  private CoordinateReferenceSystem targetCoordinateReferenceSystem;

  // Transformed coordinates stored directly as DirectPosition2D
  private DirectPosition2D topLeftTransformed;
  private DirectPosition2D bottomRightTransformed;

  /**
   * Creates a new GridBuilder with specified input and target CRS, and corner coordinates.
   *
   * @param inputCrsCode EPSG code for the input CRS
   * @param targetCrsCode EPSG code for the target CRS
   * @param cornerCoords Map containing corner coordinates with keys like "topLeftX", "topLeftY",
   *                    "bottomRightX", "bottomRightY"
   * @param cellWidth The width of each cell in the grid (in units of the target CRS)
   * @throws FactoryException if any CRS code is invalid
   * @throws TransformException if coordinate transformation fails
   */
  public GridBuilder(String inputCrsCode, String targetCrsCode,
                    GridBuilderExtents extents, BigDecimal cellWidth)
      throws FactoryException, TransformException {

    // Validate cell width
    if (cellWidth == null || cellWidth.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Cell width must be positive");
    }
    this.cellWidth = cellWidth;

    // Set up CRS and ensure X,Y (longitude/easting, latitude/northing) ordering
    CoordinateReferenceSystem inputCrs = CRS.forCode(inputCrsCode);
    CoordinateReferenceSystem targetCrs = CRS.forCode(targetCrsCode);
    usingVirutalCoordinates = false;

    // Ensure consistent X,Y ordering using Apache SIS's recommendation
    // https://sis.apache.org/faq.html#axisOrderInTransforms
    // This will leave projected systems unchanged, but will swap axes for geographic systems
    // such that we don't have to maintain different checks for geographic and projected systems
    this.inputCoordinateReferenceSystem =
        AbstractCRS.castOrCopy(inputCrs).forConvention(AxesConvention.RIGHT_HANDED);
    this.targetCoordinateReferenceSystem =
        AbstractCRS.castOrCopy(targetCrs).forConvention(AxesConvention.RIGHT_HANDED);

    // Transform coordinates immediately
    transformCornerCoordinates(
        extents.getTopLeftX(), extents.getTopLeftY(),
        extents.getBottomRightX(), extents.getBottomRightY());
  }

  /**
   * Creates a new GridBuilder with the given corner coordinates.
   *
   * <p>Create a new GridBuilder in "virtual space" which does not correspond to an actual
   * Earth geographic location.</p>
   *
   * @param cornerCoords Map containing corner coordinates with keys like "topLeftX", "topLeftY",
   *                    "bottomRightX", "bottomRightY"
   * @param cellWidth The width of each cell in the grid (in units of the target CRS)
   * @throws TransformException if coordinate transformation fails
   */
  public GridBuilder(GridBuilderExtents extents, BigDecimal cellWidth)
      throws TransformException {

    // Validate cell width
    if (cellWidth == null || cellWidth.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Cell width must be positive");
    }
    this.cellWidth = cellWidth;

    usingVirutalCoordinates = true;

    // Transform coordinates immediately
    transformCornerCoordinates(
        extents.getTopLeftX(), extents.getTopLeftY(),
        extents.getBottomRightX(), extents.getBottomRightY());
  }

  /**
   * Validates corner coordinates based on coordinate system type.
   * For both geographic and projected coordinates, we expect Y to increase northward
   * and X to increase eastward.
   */
  private void validateCornerCoordinates(
      BigDecimal topLeftX, BigDecimal topLeftY,
      BigDecimal bottomRightX, BigDecimal bottomRightY) {

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
      BigDecimal topLeftX, BigDecimal topLeftY,
      BigDecimal bottomRightX, BigDecimal bottomRightY) throws TransformException {

    // Create DirectPosition2D objects for the corners
    DirectPosition2D topLeft = new DirectPosition2D(topLeftX.doubleValue(), topLeftY.doubleValue());
    DirectPosition2D bottomRight = new DirectPosition2D(
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
   * Transforms corner coordinates to a target CRS, with consistent X,Y axis ordering.
   *
   * @param corners Array of DirectPosition2D objects representing corner points
   * @param sourceCrs The source Coordinate Reference System
   * @param targetCrs The target Coordinate Reference System
   * @return Array of transformed DirectPosition2D objects
   * @throws TransformException if transformation fails
   */
  public DirectPosition2D[] transformCornerCoordinates(
      DirectPosition2D[] corners,
      CoordinateReferenceSystem sourceCrs,
      CoordinateReferenceSystem targetCrs
  ) throws TransformException {
    // Get the transformation between the two CRS
    MathTransform transform;
    try {
      transform = CRS.findOperation(sourceCrs, targetCrs, null).getMathTransform();
    } catch (FactoryException e) {
      throw new TransformException("Failed to find transformation between CRS: " + e.getMessage());
    }

    DirectPosition2D[] transformedCorners = new DirectPosition2D[corners.length];

    for (int i = 0; i < corners.length; i++) {
      // Create a general DirectPosition for the transformation
      DirectPosition result = new GeneralDirectPosition(
          targetCrs.getCoordinateSystem().getDimension()
      );

      // Transform the coordinates
      transform.transform(corners[i], result);

      // Since we've set consistent X,Y ordering using AxesConvention.RIGHT_HANDED,
      // we can directly use the ordinates in the original order
      transformedCorners[i] = new DirectPosition2D(
          result.getOrdinate(0),
          result.getOrdinate(1)
      );
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

      // Create spatial context
      SpatialContext targetContext = createSpatialContext();

      // Calculate grid dimensions
      GridDimensions dimensions = calculateGridDimensions();

      // Create all patches
      List<Patch> patches = createPatchGrid(dimensions, targetContext);

      return new Grid(patches, cellWidth);
    } catch (Exception e) {
      throw new RuntimeException("Failed to build grid: " + e.getMessage(), e);
    }
  }

  private SpatialContext createSpatialContext() {
    SpatialContextFactory factory = new SpatialContextFactory();
    factory.geo = targetCoordinateReferenceSystem instanceof GeographicCRS;
    return factory.newSpatialContext();
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

    double rowDiff = topLeftTransformed.y - bottomRightTransformed.y;
    double colDiff = bottomRightTransformed.x - topLeftTransformed.x;

    int rowCells = (int) Math.ceil(rowDiff / cellWidthUnits);
    int colCells = (int) Math.ceil(colDiff / cellWidthUnits);

    return new GridDimensions(colCells, rowCells, cellWidthUnits);
  }

  /**
   * Creates a single patch geometry for a specific cell.
   *
   * @param leftX The left X coordinate
   * @param topY The top Y coordinate
   * @param rightX The right X coordinate
   * @param bottomY The bottom Y coordinate
   * @param context The spatial context
   */
  private Geometry createCellGeometry(
        double leftX, double topY,
        double rightX, double bottomY,
        SpatialContext context
  ) {
    return GeometryFactory.createSquare(
        BigDecimal.valueOf(leftX),
        BigDecimal.valueOf(topY),
        BigDecimal.valueOf(rightX),
        BigDecimal.valueOf(bottomY),
        context
    );
  }

  /**
   * Creates all patches in the grid.
   */
  private List<Patch> createPatchGrid(
        GridDimensions dimensions,
        SpatialContext context
  ) {
    List<Patch> patches = new ArrayList<>();
    for (int rowIdx = 0; rowIdx < dimensions.rowCells; rowIdx++) {
      for (int colIdx = 0; colIdx < dimensions.colCells; colIdx++) {
        double cellTopLeftX = topLeftTransformed.x + (colIdx * dimensions.cellWidthUnits);
        double cellTopLeftY = topLeftTransformed.y - (rowIdx * dimensions.cellWidthUnits);

        double cellBottomRightX = cellTopLeftX + dimensions.cellWidthUnits;
        double cellBottomRightY = cellTopLeftY - dimensions.cellWidthUnits;

        // Ensure we don't exceed grid boundaries
        cellBottomRightX = Math.min(cellBottomRightX, bottomRightTransformed.x);
        cellBottomRightY = Math.max(cellBottomRightY, bottomRightTransformed.y);

        // Create geometry for this cell
        Geometry cellGeometry = createCellGeometry(
            cellTopLeftX, cellTopLeftY,
            cellBottomRightX, cellBottomRightY,
            context
        );

        if (!(cellGeometry == null)) {
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
   * This is different from validateCornerCoordinates which validates the specific
   * coordinate values.
   */
  private void validateParameters() {
    if (topLeftTransformed == null || bottomRightTransformed == null) {
      throw new IllegalStateException("Corner coordinates not transformed");
    }

    if (cellWidth == null) {
      throw new IllegalStateException("Cell width not specified");
    }

    if (!usingVirutalCoorindates && inputCoordinateReferenceSystem == null) {
      throw new IllegalStateException("Input CRS not specified");
    }

    if (!usingVirutalCoorindates && targetCoordinateReferenceSystem == null) {
      throw new IllegalStateException("Target CRS not specified");
    }

    if (cellWidth.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Cell width must be positive");
    }

    // Validate that after transformation, the coordinates still make sense
    if (topLeftTransformed.y <= bottomRightTransformed.y) {
      throw new IllegalArgumentException(
        "After transformation, top-left Y-coord must be greater than bottom-right Y-coord");
    }

    if (topLeftTransformed.x >= bottomRightTransformed.x) {
      throw new IllegalArgumentException(
        "After transformation, top-left X-coord must be less than bottom-right X-coord");
    }
  }
}
