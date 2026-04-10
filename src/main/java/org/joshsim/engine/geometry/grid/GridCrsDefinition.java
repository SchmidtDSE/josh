package org.joshsim.engine.geometry.grid;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.joshsim.engine.geometry.PatchBuilderExtents;

/**
 * Definition of a grid coordinate reference system.
 * Holds parameters needed to map between grid coordinates and a real-world CRS.
 *
 * <p>This class carries two cell size values to handle the count-conversion path:
 * <ul>
 *   <li>{@link #getCellSizeCoords()} — the cell width in the grid's coordinate space.
 *       After count-conversion (WGS84 degrees → count indices), this is {@code 1}.
 *       For projected or meter-space grids, this equals {@link #getCellSizeMeters()}.</li>
 *   <li>{@link #getCellSizeMeters()} — the physical cell size in meters, always the
 *       user-specified value regardless of coordinate conversions.</li>
 * </ul>
 * </p>
 */
public class GridCrsDefinition {
  private final String name;
  private final String baseCrsCode;
  private final PatchBuilderExtents extents;
  private final BigDecimal coordCellSize;
  private final BigDecimal cellSizeMeters;

  /**
   * Creates a grid CRS definition where index cell size equals the meter cell size.
   *
   * <p>Use this constructor when no count-conversion occurs (Earth-space path or
   * pure meter grids where coordinate space matches physical space).</p>
   *
   * @param name The name of the grid system
   * @param baseCrsCode The EPSG code or identifier for base CRS
   * @param extents The grid extents in base CRS coordinates
   * @param cellSize Cell size in both coordinate space and meters (they are equal)
   */
  public GridCrsDefinition(
      String name,
      String baseCrsCode,
      PatchBuilderExtents extents,
      BigDecimal cellSize) {
    this(name, baseCrsCode, extents, cellSize, cellSize);
  }

  /**
   * Creates a grid CRS definition with separate coordinate and meter cell sizes.
   *
   * <p>Use this constructor when count-conversion has occurred: {@code coordCellSize}
   * is typically {@code 1} (the width in count-space), while {@code cellSizeMeters}
   * preserves the user-specified meter value (e.g., 30m, 1000m).</p>
   *
   * @param name The name of the grid system
   * @param baseCrsCode The EPSG code or identifier for base CRS
   * @param extents The grid extents in the (possibly count-converted) coordinate space
   * @param coordCellSize Cell width in coordinate space (1 after count-conversion)
   * @param cellSizeMeters The physical cell size in meters
   */
  public GridCrsDefinition(
      String name,
      String baseCrsCode,
      PatchBuilderExtents extents,
      BigDecimal coordCellSize,
      BigDecimal cellSizeMeters) {

    this.name = Objects.requireNonNull(name, "Name cannot be null");
    this.baseCrsCode = Objects.requireNonNull(baseCrsCode, "Base CRS code cannot be null");
    this.extents = Objects.requireNonNull(extents, "Extents cannot be null");
    this.coordCellSize = Objects.requireNonNull(coordCellSize, "Coord cell size cannot be null");
    this.cellSizeMeters = Objects.requireNonNull(
        cellSizeMeters, "Cell size in meters cannot be null");

    if (coordCellSize.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Coord cell size must be positive");
    }
    if (cellSizeMeters.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Cell size in meters must be positive");
    }
  }

  /**
   * Gets the cell width in the grid's coordinate space.
   *
   * <p>After count-conversion (WGS84 degrees → count indices), this returns {@code 1}.
   * For projected or meter-space grids, this equals {@link #getCellSizeMeters()}.
   * Use this for grid layout, coordinate transforms, and patch construction.</p>
   *
   * @return cell width in coordinate space
   */
  public BigDecimal getCellSizeCoords() {
    return coordCellSize;
  }

  /**
   * Gets the physical cell size in meters.
   *
   * <p>Always the user-specified value, regardless of coordinate conversions.
   * Use this for distance calculations (e.g., spatial query radius conversion).</p>
   *
   * @return the cell size in meters
   */
  public BigDecimal getCellSizeMeters() {
    return cellSizeMeters;
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
    BigDecimal crsX = extents.getTopLeftX().add(gridX.multiply(coordCellSize));
    BigDecimal crsY = extents.getTopLeftY().add(gridY.multiply(coordCellSize));
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
        crsX.subtract(extents.getTopLeftX()).divide(coordCellSize, 10, RoundingMode.HALF_UP);
    BigDecimal gridY =
        crsY.subtract(extents.getTopLeftY()).divide(coordCellSize, 10, RoundingMode.HALF_UP);
    return new BigDecimal[] {gridX, gridY};
  }

  @Override
  public String toString() {
    return String.format(
        "GridCrsDefinition[name=%s, extents=(%s,%s to %s,%s), coordCellSize=%s, cellSizeMeters=%s]",
        name,
        extents.getTopLeftX(),
        extents.getTopLeftY(),
        extents.getBottomRightX(),
        extents.getBottomRightY(),
        coordCellSize,
        cellSizeMeters);
  }
}
