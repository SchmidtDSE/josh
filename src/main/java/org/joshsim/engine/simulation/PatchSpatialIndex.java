/**
 * Spatial index for efficient patch lookups by geometry.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.simulation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joshsim.engine.entity.base.Entity;
import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.geometry.EngineGeometry;
import org.joshsim.engine.geometry.grid.GridShape;
import org.joshsim.engine.geometry.grid.GridShapeType;

/**
 * Spatial index for efficient patch lookups by geometry.
 *
 * <p>Thread-safe read-only index built once per TimeStep. Uses a 2D grid structure
 * to organize patches by their spatial location for efficient candidate lookup.
 * Pre-filters patches based on grid cell overlap before exact intersection tests.</p>
 */
class PatchSpatialIndex {
  private static final int MAX_SIZE = 10000;

  private final Entity[][] grid;
  private final BigDecimal minX;
  private final BigDecimal minY;
  private final BigDecimal maxX;
  private final BigDecimal maxY;
  private final BigDecimal cellSize;
  private final int gridWidth;
  private final int gridHeight;
  private final Map<GeoKey, Entity> allPatches;

  /**
   * Builds a spatial index from the given patches.
   *
   * @param patches map of patches to index
   * @throws IllegalStateException if patches don't form a regular grid
   */
  PatchSpatialIndex(Map<GeoKey, Entity> patches) {
    this.allPatches = patches;

    if (patches.isEmpty()) {
      this.grid = new Entity[0][0];
      this.minX = BigDecimal.ZERO;
      this.minY = BigDecimal.ZERO;
      this.maxX = BigDecimal.ZERO;
      this.maxY = BigDecimal.ZERO;
      this.cellSize = BigDecimal.ONE;
      this.gridWidth = 0;
      this.gridHeight = 0;
      return;
    }

    GridParameters params = analyzePatches(patches);

    if (params == null) {
      this.grid = new Entity[0][0];
      this.minX = BigDecimal.ZERO;
      this.minY = BigDecimal.ZERO;
      this.maxX = BigDecimal.ZERO;
      this.maxY = BigDecimal.ZERO;
      this.cellSize = BigDecimal.ONE;
      this.gridWidth = 0;
      this.gridHeight = 0;
      return;
    }

    this.minX = params.minX;
    this.minY = params.minY;
    this.maxX = params.maxX;
    this.maxY = params.maxY;
    this.cellSize = params.cellSize;

    GridDimensions dims = calculateGridDimensions(params);
    this.gridWidth = dims.width;
    this.gridHeight = dims.height;

    if (gridWidth > MAX_SIZE || gridHeight > MAX_SIZE) {
      throw new IllegalStateException(
          "Grid too large for spatial index: " + gridWidth + "x" + gridHeight);
    }

    this.grid = buildGrid(patches);
  }

  /**
   * Analyzes patches to determine grid parameters.
   */
  private GridParameters analyzePatches(Map<GeoKey, Entity> patches) {
    BigDecimal foundMinX = null;
    BigDecimal foundMinY = null;
    BigDecimal foundMaxX = null;
    BigDecimal foundMaxY = null;
    BigDecimal foundCellSize = null;

    for (Entity patch : patches.values()) {
      Optional<EngineGeometry> geomOpt = patch.getGeometry();
      if (geomOpt.isEmpty()) {
        continue;
      }

      EngineGeometry geom = geomOpt.get();
      BigDecimal centerX = geom.getCenterX();
      BigDecimal centerY = geom.getCenterY();

      if (foundMinX == null || centerX.compareTo(foundMinX) < 0) {
        foundMinX = centerX;
      }
      if (foundMaxX == null || centerX.compareTo(foundMaxX) > 0) {
        foundMaxX = centerX;
      }
      if (foundMinY == null || centerY.compareTo(foundMinY) < 0) {
        foundMinY = centerY;
      }
      if (foundMaxY == null || centerY.compareTo(foundMaxY) > 0) {
        foundMaxY = centerY;
      }

      if (foundCellSize == null && geom.getOnGrid() != null) {
        foundCellSize = geom.getOnGrid().getWidth();
      }
    }

    if (foundMinX == null || foundCellSize == null) {
      return null;
    }

    return new GridParameters(foundMinX, foundMinY, foundMaxX, foundMaxY, foundCellSize);
  }

  /**
   * Calculates grid dimensions from parameters.
   */
  private GridDimensions calculateGridDimensions(GridParameters params) {
    BigDecimal widthInCells = params.maxX.subtract(params.minX)
        .divide(params.cellSize, java.math.RoundingMode.HALF_UP)
        .add(BigDecimal.ONE);
    BigDecimal heightInCells = params.maxY.subtract(params.minY)
        .divide(params.cellSize, java.math.RoundingMode.HALF_UP)
        .add(BigDecimal.ONE);

    return new GridDimensions(widthInCells.intValue(), heightInCells.intValue());
  }

  /**
   * Builds the 2D grid from patches.
   */
  private Entity[][] buildGrid(Map<GeoKey, Entity> patches) {
    Entity[][] newGrid = new Entity[gridWidth][gridHeight];

    for (Entity patch : patches.values()) {
      Optional<EngineGeometry> geomOpt = patch.getGeometry();
      if (geomOpt.isEmpty()) {
        continue;
      }

      EngineGeometry geom = geomOpt.get();
      int gridX = worldToGridX(geom.getCenterX());
      int gridY = worldToGridY(geom.getCenterY());

      if (gridX >= 0 && gridX < gridWidth && gridY >= 0 && gridY < gridHeight) {
        newGrid[gridX][gridY] = patch;
      }
    }

    return newGrid;
  }

  /**
   * Converts world X coordinate to grid index.
   */
  private int worldToGridX(BigDecimal worldX) {
    return worldX.subtract(minX)
        .divide(cellSize, java.math.RoundingMode.HALF_UP)
        .intValue();
  }

  /**
   * Converts world Y coordinate to grid index.
   */
  private int worldToGridY(BigDecimal worldY) {
    return worldY.subtract(minY)
        .divide(cellSize, java.math.RoundingMode.HALF_UP)
        .intValue();
  }

  /**
   * Queries patches that potentially intersect with the given geometry.
   *
   * @param geometry the query geometry
   * @return list of candidate patches (may include false positives)
   */
  List<Entity> queryCandidates(EngineGeometry geometry) {
    if (gridWidth == 0 || gridHeight == 0) {
      return new ArrayList<>(allPatches.values());
    }

    GridShape gridGeom = geometry.getOnGrid();
    if (gridGeom == null) {
      return new ArrayList<>(allPatches.values());
    }

    if (gridGeom.getGridShapeType() == GridShapeType.CIRCLE) {
      return queryCandidatesForCircle(gridGeom);
    }

    return queryCandidatesForNonCircle(gridGeom);
  }

  /**
   * Queries candidates for non-circle geometries using bounding box.
   */
  private List<Entity> queryCandidatesForNonCircle(GridShape gridGeom) {
    BigDecimal centerX = gridGeom.getCenterX();
    BigDecimal centerY = gridGeom.getCenterY();
    BigDecimal width = gridGeom.getWidth();
    BigDecimal height = gridGeom.getHeight();

    BigDecimal radius = width.max(height)
        .divide(new BigDecimal("2"), java.math.RoundingMode.HALF_UP);

    BigDecimal queryMinX = centerX.subtract(radius);
    BigDecimal queryMaxX = centerX.add(radius);
    BigDecimal queryMinY = centerY.subtract(radius);
    BigDecimal queryMaxY = centerY.add(radius);

    int minGridX = Math.max(0, worldToGridX(queryMinX));
    int maxGridX = Math.min(gridWidth - 1, worldToGridX(queryMaxX));
    int minGridY = Math.max(0, worldToGridY(queryMinY));
    int maxGridY = Math.min(gridHeight - 1, worldToGridY(queryMaxY));

    int estimatedSize = (maxGridX - minGridX + 1) * (maxGridY - minGridY + 1);
    List<Entity> candidates = new ArrayList<>(estimatedSize);
    for (int x = minGridX; x <= maxGridX; x++) {
      for (int y = minGridY; y <= maxGridY; y++) {
        Entity patch = grid[x][y];
        if (patch != null) {
          candidates.add(patch);
        }
      }
    }

    return candidates;
  }

  /**
   * Optimized candidate query for circle geometries using exact intersection mathematics.
   *
   * <p>This method computes the exact set of grid cells that intersect the query circle.
   * Uses precomputed offsets cached globally per radius for efficient query processing.</p>
   *
   * <p>Offsets are computed once per unique radius and cached in CIRCLE_OFFSETS_CACHE.
   * Subsequent queries with the same radius use the cached offsets for direct iteration
   * over intersecting cells.</p>
   *
   * @param circle the circle geometry to query
   * @return list of patches that intersect the circle
   */
  private List<Entity> queryCandidatesForCircle(GridShape circle) {
    BigDecimal centerX = circle.getCenterX();
    BigDecimal centerY = circle.getCenterY();
    BigDecimal diameter = circle.getWidth();

    int centerGridX = worldToGridX(centerX);
    int centerGridY = worldToGridY(centerY);

    double radiusInGridCells = diameter.doubleValue() / (2.0 * cellSize.doubleValue());

    List<GridOffset> offsets = CircleOffsetsCache.getOffsetsForRadius(radiusInGridCells);

    int maxOffset = (int) Math.ceil(radiusInGridCells + Math.sqrt(2.0));
    if (maxOffset >= gridWidth || maxOffset >= gridHeight) {
      return new ArrayList<>(allPatches.values());
    }

    List<Entity> candidates = new ArrayList<>(offsets.size());

    int offsetCount = offsets.size();
    for (int i = 0; i < offsetCount; i++) {
      GridOffset offset = offsets.get(i);
      int gridX = centerGridX + offset.getOffsetX();
      int gridY = centerGridY + offset.getOffsetY();

      if (gridX >= 0 && gridX < gridWidth && gridY >= 0 && gridY < gridHeight) {
        Entity patch = grid[gridX][gridY];
        if (patch != null) {
          candidates.add(patch);
        }
      }
    }

    return candidates;
  }

  /**
   * Tests if a unit square at grid offset (dx, dy) intersects a circle centered at origin.
   *
   * <p>Uses the closest-point-on-rectangle algorithm for exact intersection detection.
   * The square has bounds [dx-0.5, dx+0.5] x [dy-0.5, dy+0.5] and the circle has
   * the specified radius, centered at (0, 0) in offset space.</p>
   *
   * @param dx the x-offset of the square center in grid cells
   * @param dy the y-offset of the square center in grid cells
   * @param radius the circle radius in grid cell units
   * @return true if the square intersects the circle, false otherwise
   */
  static boolean isSquareIntersectingCircle(int dx, int dy, double radius) {
    double squareMinX = dx - 0.5;
    double squareMaxX = dx + 0.5;
    double squareMinY = dy - 0.5;
    double squareMaxY = dy + 0.5;

    double closestX = clamp(0.0, squareMinX, squareMaxX);
    double closestY = clamp(0.0, squareMinY, squareMaxY);

    double distanceSquared = closestX * closestX + closestY * closestY;
    double distance = Math.sqrt(distanceSquared);

    return distance <= radius;
  }

  /**
   * Clamps a value to the range [min, max].
   *
   * @param value the value to clamp
   * @param min the minimum bound
   * @param max the maximum bound
   * @return the clamped value
   */
  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(value, max));
  }

  /**
   * Helper class to hold grid parameters during construction.
   */
  private static class GridParameters {
    final BigDecimal minX;
    final BigDecimal minY;
    final BigDecimal maxX;
    final BigDecimal maxY;
    final BigDecimal cellSize;

    GridParameters(BigDecimal minX, BigDecimal minY, BigDecimal maxX,
                   BigDecimal maxY, BigDecimal cellSize) {
      this.minX = minX;
      this.minY = minY;
      this.maxX = maxX;
      this.maxY = maxY;
      this.cellSize = cellSize;
    }
  }

  /**
   * Helper class to hold grid dimensions.
   */
  private static class GridDimensions {
    final int width;
    final int height;

    GridDimensions(int width, int height) {
      this.width = width;
      this.height = height;
    }
  }
}
