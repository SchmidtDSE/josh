package org.joshsim.engine.geometry.grid;

import org.joshsim.engine.entity.prototype.EntityPrototype;
import org.joshsim.engine.geometry.PatchBuilder;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.geometry.PatchSet;

import java.math.BigDecimal;


public class GridPatchBuilder implements PatchBuilder {

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
    return null;
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

  }

}
