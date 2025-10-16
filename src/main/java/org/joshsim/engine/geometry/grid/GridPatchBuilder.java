/**
 * Logic to build grid space patches.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry.grid;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.joshsim.compat.CompatibilityLayerKeeper;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.geometry.PatchBuilder;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchSet;

/**
 * Builder which can create patches within grid space.
 *
 * <p>Builds a rectangular grid of spatial patches using specified grid CRS definition and an
 * entity prototype within grid-space.</p>
 */
public class GridPatchBuilder implements PatchBuilder {

  // Required for WASM build
  private static final BigDecimal ONE_THRESHOLD = new BigDecimal("0.00001");

  /**
   * Size of the coordinate cache for reducing BigDecimal allocations.
   *
   * <p>This cache covers coordinate values from 0 to COORD_CACHE_SIZE-1, which includes
   * typical grid dimensions up to 10,000x10,000 cells. Values beyond this range fall back
   * to on-demand BigDecimal creation.</p>
   */
  private static final int COORD_CACHE_SIZE = 10000;

  /**
   * Static cache of BigDecimal coordinate values.
   *
   * <p>Pre-allocated BigDecimal objects for integer coordinates 0 through COORD_CACHE_SIZE-1.
   * This eliminates millions of BigDecimal allocations during grid building. The cache is
   * read-only after static initialization, making it thread-safe.</p>
   */
  private static final BigDecimal[] COORD_CACHE = new BigDecimal[COORD_CACHE_SIZE];

  static {
    for (int i = 0; i < COORD_CACHE_SIZE; i++) {
      COORD_CACHE[i] = new BigDecimal(i);
    }
  }

  private final GridCrsDefinition gridCrsDefinition;
  private final EntityPrototype prototype;

  /**
   * Creates a new PatchBuilder with specified grid CRS definition and entity prototype.
   *
   * @param gridCrsDefinition Definition of the grid CRS, including extents and cell size.
   * @param prototype The entity prototype used to create grid cells.
   */
  public GridPatchBuilder(GridCrsDefinition gridCrsDefinition, EntityPrototype prototype) {
    validate(gridCrsDefinition.getExtents());
    this.gridCrsDefinition = gridCrsDefinition;
    this.prototype = prototype;
  }

  /**
   * Build a new set of patches in grid space.
   *
   * <p>Using the given grid CRS definition, create a grid of cells each with width and height
   * of the defined cell size where each cell is an instance built through the prototype
   * given at construction.</p>
   *
   * @return Set of patches constructed where each patch is an instance built through the prototype
   *     given at construction.
   */
  @Override
  public PatchSet build() {
    PatchBuilderExtents extents = gridCrsDefinition.getExtents();
    BigDecimal cellWidth = gridCrsDefinition.getCellSize();

    BigDecimal minX = extents.getTopLeftX();
    BigDecimal maxX = extents.getBottomRightX();
    BigDecimal minY = extents.getTopLeftY();
    BigDecimal maxY = extents.getBottomRightY();

    long numCellsX;
    long numCellsY;
    if (cellWidth.subtract(BigDecimal.ONE).abs().compareTo(ONE_THRESHOLD) <= 0) {
      numCellsX = maxX.subtract(minX).setScale(0, RoundingMode.HALF_UP).longValue();
      numCellsY = maxY.subtract(minY).setScale(0, RoundingMode.HALF_UP).longValue();
    } else {
      numCellsX = maxX.subtract(minX).divide(cellWidth, RoundingMode.CEILING).longValue();
      numCellsY = maxY.subtract(minY).divide(cellWidth, RoundingMode.CEILING).longValue();
    }

    int totalPatches = Math.multiplyExact((int) numCellsX, (int) numCellsY);
    List<MutableEntity> patches = new ArrayList<>(totalPatches);
    BigDecimal halfWidth = cellWidth.divide(CompatibilityLayerKeeper.get().getTwo());
    for (long x = 0; x < numCellsX; x++) {
      for (long y = 0; y < numCellsY; y++) {
        BigDecimal bdX = (x < COORD_CACHE_SIZE) ? COORD_CACHE[(int) x] : new BigDecimal(x);
        BigDecimal bdY = (y < COORD_CACHE_SIZE) ? COORD_CACHE[(int) y] : new BigDecimal(y);
        BigDecimal offsetX = cellWidth.multiply(bdX);
        BigDecimal offsetY = cellWidth.multiply(bdY);

        BigDecimal centerX = minX.add(offsetX).add(halfWidth);
        BigDecimal centerY = minY.add(offsetY).add(halfWidth);

        GridSquare square = new GridSquare(centerX, centerY, cellWidth);
        MutableEntity patch = prototype.buildSpatial(square);
        patches.add(patch);
      }
    }

    return new PatchSet(patches, gridCrsDefinition);
  }

  /**
   * Check that the positions provided in the given extents are as expected.
   *
   * <p>Check that these extents are valid in grid-space, specifically that the top left X is less
   * than the bottom right X and that the top left Y is less than the bottom right Y.</p>
   *
   * @param extents The extents to check.
   * @throws IllegalArgumentException Thrown if the extents are not valid in grid-space.
   */
  private void validate(PatchBuilderExtents extents) {
    if (extents.getTopLeftX().compareTo(extents.getBottomRightX()) >= 0) {
      throw new IllegalArgumentException("Top left X must be less than bottom right X");
    }

    if (extents.getTopLeftY().compareTo(extents.getBottomRightY()) >= 0) {
      throw new IllegalArgumentException("Top left Y must be less than bottom right Y");
    }
  }
}
