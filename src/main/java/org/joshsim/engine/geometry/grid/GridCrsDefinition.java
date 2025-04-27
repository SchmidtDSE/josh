package org.joshsim.engine.geometry.grid;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.joshsim.engine.geometry.PatchBuilderExtents;

/**
 * Definition of a grid coordinate reference system.
 * Holds parameters needed to map between grid coordinates and a real-world CRS.
 *
 * <p>The cell size is always specified in meters, regardless of the base CRS units.
 * When using a geographic CRS (such as EPSG:4326), the cell size will be applied
 * in the intermediate projected space.</p>
 */
public class GridCrsDefinition {
  private final String name;
  private final String baseCrsCode;
  private final PatchBuilderExtents extents;
  private final BigDecimal cellSize;  // Always in meters
  private final String cellSizeUnits;      // For documentation only

  /**
   * Creates a grid CRS definition with specified parameters.
   *
   * @param name The name of the grid system
   * @param baseCrsCode The EPSG code or identifier for base CRS
   * @param extents The grid extents in base CRS coordinates
   * @param cellSize Size of each cell in meters
   * @param cellSizeUnits Units of the base CRS (e.g., "degrees", "m")
   */
  public GridCrsDefinition(
      String name,
      String baseCrsCode,
      PatchBuilderExtents extents,
      BigDecimal cellSize,
      String cellSizeUnits) {

    this.name = Objects.requireNonNull(name, "Name cannot be null");
    this.baseCrsCode = Objects.requireNonNull(baseCrsCode, "Base CRS code cannot be null");
    this.extents = Objects.requireNonNull(extents, "Extents cannot be null");
    this.cellSize = Objects.requireNonNull(cellSize, "Cell size cannot be null");
    this.cellSizeUnits = Objects.requireNonNull(cellSizeUnits, "Cell size units cannot be null");

    if (cellSize.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Cell size must be positive");
    }
  }

  // Update getters
  public BigDecimal getCellSize() {
    return cellSize;
  }

  public String getCellSizeUnit() {
    return "m"; // Always meters
  }

  public String getName() {
    return name;
  }

  public String getBaseCrsCode() {
    return baseCrsCode;
  }

  public PatchBuilderExtents getExtents() {
    return extents;
  }

  /**
   * Convert grid position (cell x,y) to base CRS coordinates.
   * Note: This doesn't handle unit conversion - that happens in GridCrsManager.
   *
   * @param gridX Grid X position (cell index)
   * @param gridY Grid Y position (cell index)
   * @return Coordinate in base CRS [x,y]
   */
  public BigDecimal[] gridToCrsCoordinates(BigDecimal gridX, BigDecimal gridY) {
    // Origin is at top-left corner
    BigDecimal crsX = extents.getTopLeftX().add(gridX.multiply(cellSize));
    BigDecimal crsY = extents.getTopLeftY().add(gridY.multiply(cellSize));
    return new BigDecimal[] {crsX, crsY};
  }

  /**
   * Convert base CRS coordinates to grid position (cell x,y).
   * Note: This doesn't handle unit conversion - that happens in GridCrsManager.
   *
   * @param crsX X coordinate in base CRS
   * @param crsY Y coordinate in base CRS
   * @return Grid position [x,y]
   */
  public BigDecimal[] crsToGridCoordinates(BigDecimal crsX, BigDecimal crsY) {
    BigDecimal gridX =
        crsX.subtract(extents.getTopLeftX()).divide(cellSize, 10, RoundingMode.HALF_UP);
    BigDecimal gridY =
        crsY.subtract(extents.getTopLeftY()).divide(cellSize, 10, RoundingMode.HALF_UP);
    return new BigDecimal[] {gridX, gridY};
  }

  @Override
  public String toString() {
    return String.format(
      "GridCrsDefinition[name=%s, extents=(%s,%s to %s,%s), cellSize=%s %s]",
      name,
      extents.getTopLeftX(), extents.getTopLeftY(),
      extents.getBottomRightX(), extents.getBottomRightY(),
      cellSize, cellSizeUnits);
  }
}
