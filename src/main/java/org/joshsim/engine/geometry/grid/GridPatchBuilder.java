/**
 * Logic to build grid space patches.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.geometry.grid;

import java.math.BigDecimal;
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
 * <p>Builds a rectangular grid of spatial patches using specified grid extents, cell width, and an
 * entity prototype within grid-space.</p>
 */
public class GridPatchBuilder implements PatchBuilder {

  // Required for WASM build
  private static final BigDecimal ONE_THRESHOLD = new BigDecimal("0.00001");

  private final PatchBuilderExtents extents;
  private final BigDecimal cellWidth;
  private final EntityPrototype prototype;

  /**
   * Creates a new PatchBuilder with specified input and target CRS, and corner coordinates.
   *
   * @param extents Structure describing the extents or bounds of the grid to be built.
   * @param cellWidth The width of each cell in the grid (in units of count).
   * @param prototype The entity prototype used to create grid cells
   */
  public GridPatchBuilder(PatchBuilderExtents extents, BigDecimal cellWidth,
      EntityPrototype prototype) {
    validate(extents);

    this.extents = extents;
    this.cellWidth = cellWidth;
    this.prototype = prototype;
  }

  /**
   * Build a new set of patches in grid space.
   *
   * <p>Using the given extents, create a grid of cells each with width and height of cellWidth
   * where each cell is an instance built through the prototype given at construction.</p>
   *
   * @return Set of patches constructed where each patch is an instance built through the prototype
   *     given at construction.
   */
  @Override
  public PatchSet build() {
    BigDecimal minX = extents.getTopLeftX();
    BigDecimal maxX = extents.getBottomRightX();
    BigDecimal minY = extents.getTopLeftY();
    BigDecimal maxY = extents.getBottomRightY();

    long numCellsX;
    long numCellsY;
    if (cellWidth.subtract(BigDecimal.ONE).abs().compareTo(ONE_THRESHOLD) <= 0) {
      numCellsX = maxX.subtract(minX).setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
      numCellsY = maxY.subtract(minY).setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
    } else {
      numCellsX = maxX.subtract(minX).divide(cellWidth, BigDecimal.ROUND_CEILING).longValue();
      numCellsY = maxY.subtract(minY).divide(cellWidth, BigDecimal.ROUND_CEILING).longValue();
    }

    List<MutableEntity> patches = new ArrayList<>();
    BigDecimal halfWidth = cellWidth.divide(CompatibilityLayerKeeper.get().getTwo());
    for (long x = 0; x < numCellsX; x++) {
      for (long y = 0; y < numCellsY; y++) {
        BigDecimal offsetX = cellWidth.multiply(new BigDecimal(x));
        BigDecimal offsetY = cellWidth.multiply(new BigDecimal(y));

        BigDecimal centerX = minX.add(offsetX).add(halfWidth);
        BigDecimal centerY = minY.add(offsetY).add(halfWidth);

        GridSquare square = new GridSquare(centerX, centerY, cellWidth);
        MutableEntity patch = prototype.buildSpatial(square);
        patches.add(patch);
      }
    }

    return new PatchSet(patches, cellWidth);
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
