package org.joshsim.engine.geometry.grid;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.joshsim.engine.geometry.PatchBuilderExtents;

/**
 * Definition of a grid coordinate reference system.
 * Holds parameters needed to map between grid coordinates and a real-world CRS.
 */
public class GridCrsDefinition {
  private final String name;
  private final String baseCrsCode;  // e.g. "EPSG:4326" 
  private final PatchBuilderExtents extents;
  private final BigDecimal cellSize;
  private final String cellSizeUnit;  // e.g. "m", "degrees"
  private final String crsUnits;    // e.g. "degrees", "m"
  
  /**
   * Creates a grid CRS definition with specified parameters.
   *
   * @param name The name of the grid system
   * @param baseCrsCode The EPSG code or identifier for base CRS
   * @param extents The grid extents in base CRS coordinates
   * @param cellSize Size of each cell
   * @param cellSizeUnit Unit of the cell size (e.g., "m", "degrees")
   * @param crsUnits Units of the base CRS (e.g., "degrees", "m")
   */
  public GridCrsDefinition(
      String name,
      String baseCrsCode,
      PatchBuilderExtents extents, 
      BigDecimal cellSize,
      String cellSizeUnit,
      String crsUnits) {
    
    this.name = Objects.requireNonNull(name, "Name cannot be null");
    this.baseCrsCode = Objects.requireNonNull(baseCrsCode, "Base CRS code cannot be null");
    this.extents = Objects.requireNonNull(extents, "Extents cannot be null");
    this.cellSize = Objects.requireNonNull(cellSize, "Cell size cannot be null");
    this.cellSizeUnit = Objects.requireNonNull(cellSizeUnit, "Cell size unit cannot be null");
    this.crsUnits = Objects.requireNonNull(crsUnits, "CRS units cannot be null");
    
    if (cellSize.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Cell size must be positive");
    }
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

  public BigDecimal getCellSize() {
    return cellSize;
  }

  public String getCellSizeUnit() {
    return cellSizeUnit;
  }

  public String getCrsUnits() {
    return crsUnits;
  }
  
  /**
   * Checks if cell size units match CRS units.
   *
   * @return true if units match, false otherwise
   */
  public boolean hasSameUnits() {
    return cellSizeUnit.equalsIgnoreCase(crsUnits);
  }
  
  /**
   * Convert grid position (cell x,y) to base CRS coordinates.
   * Note: This doesn't handle unit conversion - that happens in RealizedGridCrs.
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
   * Note: This doesn't handle unit conversion - that happens in RealizedGridCrs.
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
      "GridCrsDefinition[name=%s, extents=(%s,%s to %s,%s), cellSize=%s %s, baseCrs=%s (%s)]", 
      name, 
      extents.getTopLeftX(), extents.getTopLeftY(),
      extents.getBottomRightX(), extents.getBottomRightY(),
      cellSize, cellSizeUnit, 
      baseCrsCode, crsUnits);
  }
}