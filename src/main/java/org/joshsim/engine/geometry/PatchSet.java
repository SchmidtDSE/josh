package org.joshsim.engine.geometry;

import java.math.BigDecimal;
import java.util.List;
import org.joshsim.engine.entity.base.MutableEntity;

/**
 * List of patches and spacing infromation for those patches.
 */
public class PatchSet {
  private final List<MutableEntity> patches;
  private final BigDecimal spacing;


  /**
   * Constructor for a new PatchSet.
   *
   * @param patches List of patches in the grid.
   * @param spacing Width of a cell in the grid in meters.
   */
  public PatchSet(List<MutableEntity> patches, BigDecimal spacing) {
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
