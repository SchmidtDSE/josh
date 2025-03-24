package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.referencing.CRS;
import org.joshsim.engine.entity.Patch;
import org.joshsim.engine.value.EngineValue;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.SpatialContextFactory;
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
  private BigDecimal topLeftLatitude;
  private BigDecimal topLeftLongitude;
  private BigDecimal bottomRightLatitude;
  private BigDecimal bottomRightLongitude;
  private EngineValue cellWidth;
  private boolean inputIsGeographic;

  // CRS-related fields
  private CoordinateReferenceSystem inputCoordinateReferenceSystem;
  private CoordinateReferenceSystem targetCoordinateReferenceSystem;

  /**
   * Creates a new GridBuilder using WGS84 as the default CRS.
   */
  public GridBuilder() {
    try {
      this.inputCoordinateReferenceSystem = CRS.forCode("EPSG:4326"); // WGS84
      this.inputIsGeographic = true;
    } catch (FactoryException e) {
      throw new RuntimeException("Failed to create default CRS", e);
    }
  }

  /**
   * Sets the input Coordinate Reference System.
   *
   * @param epsg EPSG code for the input CRS (e.g., "EPSG:4326" for WGS84)
   * @return this builder for method chaining
   * @throws FactoryException if the CRS code is invalid
   */
  public GridBuilder setInputCoordinateReferenceSystem(String epsg) throws FactoryException {
    inputCoordinateReferenceSystem = CRS.forCode(epsg);
    inputIsGeographic = inputCoordinateReferenceSystem instanceof GeographicCRS;
    return this;
  }

  /**
   * Sets the target Coordinate Reference System to which coordinates will be converted.
   * If not specified, a suitable CRS will be automatically selected.
   *
   * @param crsCode EPSG code for the target CRS
   * @return this builder for method chaining
   * @throws FactoryException if the CRS code is invalid
   */
  public GridBuilder setTargetCoordinateReferenceSystem(String crsCode) throws FactoryException {
    this.targetCoordinateReferenceSystem = CRS.forCode(crsCode);
    return this;
  }

  /**
   * Gets the current input CRS.
   */
  public CoordinateReferenceSystem getInputCoordinateReferenceSystem() {
    return inputCoordinateReferenceSystem;
  }

  /**
   * Gets the current target CRS.
   */
  public CoordinateReferenceSystem getTargetCoordinateReferenceSystem() {
    return targetCoordinateReferenceSystem;
  }

  /**
   * Sets the top-left coordinates of the grid.
   *
   * @param latitude The top-left latitude or Y-coordinate
   * @param longitude The top-left longitude or X-coordinate
   * @return this builder for method chaining
   */
  public GridBuilder setTopLeft(BigDecimal latitude, BigDecimal longitude) {
    this.topLeftLatitude = latitude;
    this.topLeftLongitude = longitude;
    return this;
  }

  /**
   * Sets the bottom-right coordinates of the grid.
   *
   * @param latitude The bottom-right latitude or Y-coordinate
   * @param longitude The bottom-right longitude or X-coordinate
   * @return this builder for method chaining
   */
  public GridBuilder setBottomRight(BigDecimal latitude, BigDecimal longitude) {
    this.bottomRightLatitude = latitude;
    this.bottomRightLongitude = longitude;
    return this;
  }

  /**
   * Sets the width of each cell in the grid.
   *
   * @param cellWidth The width of each cell (in units of the target CRS)
   * @return this builder for method chaining
   */
  public GridBuilder setCellWidth(EngineValue cellWidth) {
    this.cellWidth = cellWidth;
    return this;
  }

  /**
   * Builds and returns a Grid based on the provided specifications.
   * Coordinates are automatically converted to the target CRS if needed.
   *
   * @return a new Grid instance
   * @throws IllegalStateException if any required parameters are missing
   */
  public Grid build() {
    validateParameters();

    try {
      List<Patch> patches = createPatches();
      return new Grid(patches, cellWidth);
    } catch (FactoryException | TransformException e) {
      throw new RuntimeException("Failed to build grid: " + e.getMessage(), e);
    }
  }

  /**
   * Creates patches after transforming coordinates to the target CRS.
   */
  private List<Patch> createPatches() throws FactoryException, TransformException {
    // Transform corner coordinates to target CRS
    DirectPosition2D[] transformedCorners = transformCornerCoordinates();
    DirectPosition2D topLeft = transformedCorners[0];
    DirectPosition2D bottomRight = transformedCorners[1];

    // Create spatial context
    SpatialContext targetContext = createSpatialContext();

    // Calculate grid dimensions
    GridDimensions dimensions = calculateGridDimensions(topLeft, bottomRight);

    // Create all patches
    return createPatchGrid(topLeft, bottomRight, dimensions, targetContext);
  }

  /**
   * Transforms the corner coordinates from input CRS to target CRS.
   */
  private DirectPosition2D[] transformCornerCoordinates() throws FactoryException, TransformException {
    MathTransform transform = CRS.findOperation(
        inputCoordinateReferenceSystem,
        targetCoordinateReferenceSystem,
        null
    ).getMathTransform();

    DirectPosition2D topLeft = new DirectPosition2D(
        topLeftLongitude.doubleValue(), topLeftLatitude.doubleValue());
    DirectPosition2D bottomRight = new DirectPosition2D(
        bottomRightLongitude.doubleValue(), bottomRightLatitude.doubleValue());

    transform.transform(topLeft, topLeft);
    transform.transform(bottomRight, bottomRight);

    return new DirectPosition2D[] { topLeft, bottomRight };
  }

  /**
   * Creates a spatial context for the target CRS.
   */
  private SpatialContext createSpatialContext() {
    SpatialContextFactory factory = new SpatialContextFactory();
    factory.geo = targetCoordinateReferenceSystem instanceof GeographicCRS;
    return factory.newSpatialContext();
  }

  /**
   * Represents the dimensions of the grid in terms of cell counts.
   */
  private static class GridDimensions {
    final int xCells;
    final int yCells;
    final double cellWidthUnits;

    GridDimensions(int xCells, int yCells, double cellWidthUnits) {
        this.xCells = xCells;
        this.yCells = yCells;
        this.cellWidthUnits = cellWidthUnits;
    }
  }

  /**
   * Calculates the number of cells in each direction.
   */
  private GridDimensions calculateGridDimensions(DirectPosition2D topLeft, DirectPosition2D bottomRight) {
    double cellWidthUnits = cellWidth.getAsDecimal().doubleValue();

    double yDiff = topLeft.y - bottomRight.y;
    double xDiff = bottomRight.x - topLeft.x;

    int yCells = (int) Math.ceil(yDiff / cellWidthUnits);
    int xCells = (int) Math.ceil(xDiff / cellWidthUnits);

    return new GridDimensions(xCells, yCells, cellWidthUnits);
  }

  /**
   * Creates a single patch geometry for a specific cell.
   */
  private Geometry createCellGeometry(
        double topY, double leftX,
        double bottomY, double rightX,
        SpatialContext context
  ) {
    return GeometryFactory.createSquare(
        BigDecimal.valueOf(topY),
        BigDecimal.valueOf(leftX),
        BigDecimal.valueOf(bottomY),
        BigDecimal.valueOf(rightX),
        context
    );
  }

  /**
   * Creates all patches in the grid.
   */
  private List<Patch> createPatchGrid(
        DirectPosition2D topLeft,
        DirectPosition2D bottomRight,
        GridDimensions dimensions,
        SpatialContext context
  ) {
    List<Patch> patches = new ArrayList<>();
    for (int yIdx = 0; yIdx < dimensions.yCells; yIdx++) {
      for (int xIdx = 0; xIdx < dimensions.xCells; xIdx++) {
        double cellTopLeftY = topLeft.y - (yIdx * dimensions.cellWidthUnits);
        double cellTopLeftX = topLeft.x + (xIdx * dimensions.cellWidthUnits);

        double cellBottomRightY = cellTopLeftY - dimensions.cellWidthUnits;
        double cellBottomRightX = cellTopLeftX + dimensions.cellWidthUnits;

        // Ensure we don't exceed grid boundaries
        cellBottomRightY = Math.max(cellBottomRightY, bottomRight.y);
        cellBottomRightX = Math.min(cellBottomRightX, bottomRight.x);

        // Create geometry for this cell
        Geometry cellGeometry = createCellGeometry(
            cellTopLeftY, cellTopLeftX,
            cellBottomRightY, cellBottomRightX,
            context
        );

        patches.add(new Patch(cellGeometry));
      }
    }

    return patches;
  }

  private void validateParameters() {
    if (topLeftLatitude == null || topLeftLongitude == null) {
      throw new IllegalStateException("Top-left coordinates not specified");
    }

    if (bottomRightLatitude == null || bottomRightLongitude == null) {
      throw new IllegalStateException("Bottom-right coordinates not specified");
    }

    if (cellWidth == null) {
      throw new IllegalStateException("Cell width not specified");
    }

    if (inputCoordinateReferenceSystem == null) {
      throw new IllegalStateException("Input CRS not specified");
    }

    // For geographic coordinates, check validity
    if (inputIsGeographic) {
      if (topLeftLatitude.compareTo(bottomRightLatitude) <= 0) {
        throw new IllegalArgumentException(
          "Top-left latitude must be greater than bottom-right latitude");
      }

      if (topLeftLongitude.compareTo(bottomRightLongitude) >= 0) {
        throw new IllegalArgumentException(
          "Top-left longitude must be less than bottom-right longitude");
      }
    } else {
      // For projected coordinates, Y increases upward, X increases rightward
      if (topLeftLatitude.compareTo(bottomRightLatitude) <= 0) {
        throw new IllegalArgumentException(
          "Top-left Y-coordinate must be greater than bottom-right Y-coordinate");
      }

      if (topLeftLongitude.compareTo(bottomRightLongitude) >= 0) {
        throw new IllegalArgumentException(
          "Top-left X-coordinate must be less than bottom-right X-coordinate");
      }
    }

    if (targetCoordinateReferenceSystem == null) {
      throw new IllegalStateException("Target CRS not specified");
    }
  }
}
