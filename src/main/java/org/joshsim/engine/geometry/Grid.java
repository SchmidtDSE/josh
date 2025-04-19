package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import java.util.List;
import org.joshsim.engine.entity.base.MutableEntity;

/**
 * The Grid class contains a list of patches and a cell width for conveniences.
 * Spatial operations will be performed on the elements of the grid, Patches, which
 * are the basic unit of spatial representation.
 */
public class Grid {
  private final List<MutableEntity> patches;
  private final BigDecimal spacing;


  /**
   * Constructor for the Grid class.
   *
   * @param patches List of patches in the grid.
   * @param spacing Width of a cell in the grid in meters.
   */
  public Grid(List<MutableEntity> patches, BigDecimal spacing) {
    this.patches = patches;
    this.spacing = spacing;
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
    return spacing;
  }
}
