package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import java.util.List;
import org.joshsim.engine.entity.base.MutableEntity;
import org.joshsim.engine.geometry.grid.GridCrsDefinition;

/**
 * List of patches and spacing infromation for those patches.
 */
public class PatchSet {
  private final List<MutableEntity> patches;
  private final GridCrsDefinition gridCrsDefinition;

  /**
   * Constructor for a new PatchSet.
   *
   * @param patches List of patches in the grid.
   * @param gridCrsDefinition definition of 'Grid Space' for later CRS construction.
   */
  public PatchSet(List<MutableEntity> patches, GridCrsDefinition gridCrsDefinition) {
    this.patches = patches;
    this.gridCrsDefinition = gridCrsDefinition;
  }

  /**
   * Returns the list of patches in the grid.
   *
   * @return List of patches in the grid.
   */
  public List<MutableEntity> getPatches() {
    return patches;
  }

  /**
   * Returns the width of a cell in the grid.
   *
   * @return Width of a cell in the grid in meters.
   */
  public BigDecimal getSpacing() {
    return gridCrsDefinition.getCellSize();
  }

  /**
   * Returns the grid CRS definition.
   *
   * @return Grid CRS definition.
   */
  public GridCrsDefinition getGridCrsDefinition() {
    return gridCrsDefinition;
  }
}
