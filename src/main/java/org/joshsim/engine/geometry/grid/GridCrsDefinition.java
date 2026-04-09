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
 *   <li>{@link #getIndexCellSize()} — the cell width in index/coordinate space.
 *       After count-conversion (WGS84 degrees → count coordinates), this is {@code 1}.
 *       On the Earth-space path (no count-conversion), this equals the meter value.</li>
 *   <li>{@link #getOriginalCellSizeMeters()} — the user-specified cell size in meters,
 *       preserved across count-conversion. Always physically meaningful.</li>
 * </ul>
 * </p>
 */
public class GridCrsDefinition {
  private final String name;
  private final String baseCrsCode;
  private final PatchBuilderExtents extents;
  private final BigDecimal indexCellSize;
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
   * @param cellSize Cell size in both index space and meters (they are equal)
   */
  public GridCrsDefinition(
      String name,
      String baseCrsCode,
      PatchBuilderExtents extents,
      BigDecimal cellSize) {
    this(name, baseCrsCode, extents, cellSize, cellSize);
  }

  /**
   * Creates a grid CRS definition with separate index and meter cell sizes.
   *
   * <p>Use this constructor when count-conversion has occurred: {@code indexCellSize}
   * is typically {@code 1} (the width in count-space), while {@code cellSizeMeters}
   * preserves the original user-specified meter value (e.g., 30m, 1000m).</p>
   *
   * @param name The name of the grid system
   * @param baseCrsCode The EPSG code or identifier for base CRS
   * @param extents The grid extents in the (possibly count-converted) coordinate space
   * @param indexCellSize Cell width in index/coordinate space (1 after count-conversion)
   * @param cellSizeMeters The original cell size in meters
   */
  public GridCrsDefinition(
      String name,
      String baseCrsCode,
      PatchBuilderExtents extents,
      BigDecimal indexCellSize,
      BigDecimal cellSizeMeters) {

    this.name = Objects.requireNonNull(name, "Name cannot be null");
    this.baseCrsCode = Objects.requireNonNull(baseCrsCode, "Base CRS code cannot be null");
    this.extents = Objects.requireNonNull(extents, "Extents cannot be null");
    this.indexCellSize = Objects.requireNonNull(indexCellSize, "Index cell size cannot be null");
    this.cellSizeMeters = Objects.requireNonNull(
        cellSizeMeters, "Cell size in meters cannot be null");

    if (indexCellSize.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Index cell size must be positive");
    }
    if (cellSizeMeters.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Cell size in meters must be positive");
    }
  }

  /**
   * Gets the cell width in index/coordinate space.
   *
   * <p>After count-conversion (WGS84 degrees → count coordinates), this returns {@code 1}.
   * On the Earth-space path or pure meter grids, this returns the meter cell size.
   * Use this for grid layout, coordinate transforms, and patch construction.</p>
   *
   * <p>For the physical cell size in meters (e.g., for radius conversion in spatial queries),
   * use {@link #getOriginalCellSizeMeters()} instead.</p>
   *
   * @return cell width in index/coordinate space
   */
  public BigDecimal getIndexCellSize() {
    return indexCellSize;
  }

  /**
   * Gets the original cell size in meters before any count-space conversion.
   *
   * <p>This is always the user-specified physical cell size. When no count-conversion
   * occurred, this equals {@link #getIndexCellSize()}. When degree extents were converted
   * to count coordinates, {@code getIndexCellSize()} returns {@code 1} while this method
   * returns the original meter value (e.g., 30m).</p>
   *
   * @return the cell size in meters
   */
  public BigDecimal getOriginalCellSizeMeters() {
    return cellSizeMeters;
  }

  /**
   * Gets the cell size. Prefer {@link #getIndexCellSize()} or
   * {@link #getOriginalCellSizeMeters()} for clarity about which value you need.
   *
   * @return cell width in index/coordinate space (same as {@link #getIndexCellSize()})
   * @deprecated Use {@link #getIndexCellSize()} for grid layout or
   *     {@link #getOriginalCellSizeMeters()} for physical distance calculations.
   */
  @Deprecated
  public BigDecimal getCellSize() {
    return getIndexCellSize();
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
    BigDecimal crsX = extents.getTopLeftX().add(gridX.multiply(indexCellSize));
    BigDecimal crsY = extents.getTopLeftY().add(gridY.multiply(indexCellSize));
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
        crsX.subtract(extents.getTopLeftX()).divide(indexCellSize, 10, RoundingMode.HALF_UP);
    BigDecimal gridY =
        crsY.subtract(extents.getTopLeftY()).divide(indexCellSize, 10, RoundingMode.HALF_UP);
    return new BigDecimal[] {gridX, gridY};
  }

  @Override
  public String toString() {
    return String.format(
        "GridCrsDefinition[name=%s, extents=(%s,%s to %s,%s), indexCellSize=%s, cellSizeMeters=%s]",
        name,
        extents.getTopLeftX(),
        extents.getTopLeftY(),
        extents.getBottomRightX(),
        extents.getBottomRightY(),
        indexCellSize,
        cellSizeMeters);
  }
}
