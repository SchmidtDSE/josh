package org.joshsim.geo.geometry;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.referencing.CRS;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.geometry.HaversineUtil;
import org.joshsim.engine.geometry.PatchBuilder;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchSet;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.joshsim.engine.geometry.grid.GridSquare;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;


/**
 * Utility responsible for building grid structures in Earth space using GridCrsManager.
 *
 * <p>Utility creating a rectangular grid of patches based on coordinates in any coordinate
 * reference system, converting them to the target CRS if needed.</p>
 */
public class EarthPatchBuilder implements PatchBuilder {

  private final EntityPrototype prototype;
  private final EarthGeometryFactory geometryFactory;
  private final GridCrsDefinition gridCrsDefinition;
  private final GridCrsManager gridCrsManager;
  private final PatchBuilderExtents extents;

  // CRS-related fields
  private final CoordinateReferenceSystem targetCrs;

  /**
   * Creates a new PatchBuilder with specified input and target CRS, and corner coordinates.
   *
   * @param inputCrsStr input CRS
   * @param targetCrsStr target CRS
   * @param extents Structure describing the extents or bounds of the grid to be built.
   * @param cellWidth The width of each cell in the grid in meters.
   * @param prototype The entity prototype used to create grid cells
   * @throws FactoryException if any CRS code is invalid
   * @throws TransformException if coordinate transformation fails
   */
  public EarthPatchBuilder(
      String inputCrsStr,
      String targetCrsStr,
      PatchBuilderExtents extents,
      BigDecimal cellWidth,
      EntityPrototype prototype
  ) throws TransformException, FactoryException {
    if (inputCrsStr == null || inputCrsStr.isBlank()) {
      throw new IllegalArgumentException("Must specify input CRS");
    }

    if (targetCrsStr == null || targetCrsStr.isBlank()) {
      throw new IllegalArgumentException("Must specify target CRS");
    }

    if (cellWidth == null || cellWidth.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Cell width must be positive");
    }

    this.prototype = prototype;
    this.extents = extents;
    this.targetCrs = CRS.forCode(targetCrsStr);

    // Validate corners
    validateCornerCoordinates(
        extents.getTopLeftX(),
        extents.getTopLeftY(),
        extents.getBottomRightX(),
        extents.getBottomRightY()
    );

    // Create GridCrsDefinition
    this.gridCrsDefinition = new GridCrsDefinition(
        "Grid_" + System.currentTimeMillis(),
        targetCrsStr,
        extents,
        cellWidth,
        "meters"
    );

    try {
      // Create GridCrsManager
      this.gridCrsManager = new GridCrsManager(gridCrsDefinition);

      // Create geometry factory
      this.geometryFactory = new EarthGeometryFactory(targetCrs, gridCrsManager);
    } catch (IOException | TransformException e) {
      throw new FactoryException("Failed to create GridCrsManager: " + e.getMessage(), e);
    }
  }

  /**
   * Builds and returns a PatchSet based on the grid CRS definition.
   *
   * @return a new PatchSet instance
   */
  @Override
  public PatchSet build() {
    try {
      // Validate parameters before building
      validateParameters();

      // Create all patches using grid CRS
      List<MutableEntity> patches = createPatchGrid();

      // Return PatchSet with GridCrsDefinition
      return new PatchSet(patches, gridCrsDefinition);

    } catch (Exception e) {
      throw new RuntimeException("Failed to build grid: " + e.getMessage(), e);
    }
  }

  /**
   * Validates corner coordinates based on coordinate system type.
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

    // Y-coordinate should decrease from top to bottom
    if (topLeftY.compareTo(bottomRightY) <= 0) {
      throw new IllegalArgumentException(
          "Top-left Y-coordinate must be greater than bottom-right Y-coordinate");
    }

    // X-coordinate should increase from left to right
    if (topLeftX.compareTo(bottomRightX) >= 0) {
      throw new IllegalArgumentException(
          "Top-left X-coordinate must be less than bottom-right X-coordinate");
    }
  }

  /**
   * Creates all patches in the grid.
   *
   * <p>Creates all patches in the grid using Haversine to build cells which span from the top left
   * to bottom right in the extents provided where the center of each square patch is at distance
   * specified by provided cell size in meters.</p>
   */
  private List<MutableEntity> createPatchGrid() {
    BigDecimal cellWidthMeters = gridCrsDefinition.getCellSize();
    BigDecimal topLeftLon = extents.getTopLeftX();
    BigDecimal topLeftLat = extents.getTopLeftY();
    BigDecimal bottomRightLon = extents.getBottomRightX();
    BigDecimal bottomRightLat = extents.getBottomRightY();

    // Calculate total distances
    HaversineUtil.HaversinePoint topLeft = new HaversineUtil.HaversinePoint(
        topLeftLon,
        topLeftLat
    );
    HaversineUtil.HaversinePoint topRight = new HaversineUtil.HaversinePoint(
        bottomRightLon,
        topLeftLat
    );
    HaversineUtil.HaversinePoint bottomLeft = new HaversineUtil.HaversinePoint(
        topLeftLon,
        bottomRightLat
    );

    BigDecimal widthMeters = HaversineUtil.getDistance(topLeft, topRight);
    BigDecimal heightMeters = HaversineUtil.getDistance(topLeft, bottomLeft);
    BigDecimal halfWidth = cellWidthMeters.divide(BigDecimal.TWO, RoundingMode.HALF_UP);

    // Calculate number of cells needed
    long numColCells = widthMeters.divide(
        cellWidthMeters,
        0,
        RoundingMode.CEILING
    ).longValue();
    long numRowCells = heightMeters.divide(
        cellWidthMeters,
        0,
        RoundingMode.CEILING
    ).longValue();

    List<MutableEntity> patches = new ArrayList<>();
    HaversineUtil.HaversinePoint currentPoint = topLeft;

    for (int rowIdx = 0; rowIdx < numRowCells; rowIdx++) {
      // Reset to start of row
      currentPoint = HaversineUtil.getAtDistanceFrom(
          topLeft,
          cellWidthMeters.multiply(new BigDecimal(rowIdx)).add(halfWidth),
          "S"
      );

      for (int colIdx = 0; colIdx < numColCells; colIdx++) {
        // Move east along row
        HaversineUtil.HaversinePoint patchCenter = HaversineUtil.getAtDistanceFrom(
            currentPoint,
            cellWidthMeters.multiply(new BigDecimal(colIdx)).add(halfWidth),
            "E"
        );

        // Create grid square using the center point
        GridSquare square = new GridSquare(
            patchCenter.getLongitude(),
            patchCenter.getLatitude(),
            cellWidthMeters
        );

        MutableEntity patch = prototype.buildSpatial(square);
        patches.add(patch);
      }
    }

    return patches;
  }

  /**
   * Validates that all required objects are properly initialized.
   */
  private void validateParameters() {
    if (gridCrsDefinition == null || gridCrsManager == null) {
      throw new IllegalStateException("Grid CRS not initialized");
    }

    if (gridCrsDefinition.getCellSize().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Cell width must be positive");
    }
  }
}
