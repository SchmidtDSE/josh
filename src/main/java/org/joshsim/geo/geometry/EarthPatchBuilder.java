package org.joshsim.geo.geometry;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.referencing.CRS;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.PatchBuilder;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchSet;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;
import org.joshsim.engine.geometry.grid.GridSquare;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.MathTransform;
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
   * @param inputCrs input CRS
   * @param targetCrs target CRS
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

    // Determine CRS units for grid definition
    String crsUnits = targetCrs.getCoordinateSystem().getAxis(0).getUnit().toString();
    String cellSizeUnit = crsUnits; // Assume cell size is in the same units as target CRS

    // Create GridCrsDefinition
    this.gridCrsDefinition = new GridCrsDefinition(
        "Grid_" + System.currentTimeMillis(),
        targetCrsStr,
        extents,
        cellWidth,
        cellSizeUnit
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
      List<MutableEntity> patches = createPatchGrid(colCells, rowCells);

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
   * Creates all patches in the grid using GridCRS.
   */
  private List<MutableEntity> createPatchGrid() throws TransformException {
    List<MutableEntity> patches = new ArrayList<>();
    BigDecimal cellWidthMeters = gridCrsDefinition.getCellSize();
    BigDecimal halfCellWidthMeters = cellWidthMeters.divide(new BigDecimal(2));
    BigDecimal topLeftLon = extents.getTopLeftX();
    BigDecimal topLeftLat = extents.getTopLeftY();

    long numRowCells = 0;  // TODO: get number of cells along latitude or y using HaversineUtil
    long numColCells = 0;  // TODO: get number of cells along longitude or x using HaversineUtil
    List<MutableEntity> patches = new ArrayList<>();

    for (int rowIdx = 0; rowIdx < rowCells; rowIdx++) {
      for (int colIdx = 0; colIdx < colCells; colIdx++) {
        BigDecimal patchLongitude = null;  // TODO: calculate center longitude using HaversineUtil
        BigDecimal patchLatitude = null;  // TODO: calculate center latitude using HaversineUtil
        BigDecimal patchWidthDegrees = null;  // TODO: calculate using HaversineUtil

        GridSquare square = new GridSquare(patchLongitude, patchLatitude, patchWidthDegrees);
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

    // Ensure target CRS is projected for proper distance calculations
    if (targetCrs instanceof GeographicCRS) {
      throw new IllegalArgumentException(
          "Target CRS must be projected for accurate area/distance calculations");
    }
  }
}
