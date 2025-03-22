package org.joshsim.engine.geometry;

import java.util.List;
import org.joshsim.engine.entity.Patch;
import org.joshsim.engine.value.EngineValue;

/**
 * The Grid class contains a list of patches and a cell width for conveniences.
 * Spatial operations will be performed on the elements of the grid, Patches, which
 * are the basic unit of spatial representation.
 */
public class Grid {
  private final List<Patch> patches;
  private final EngineValue spacing;

  /**
   * Constructor for the Grid class.
   *
   * @param patches List of patches in the grid.
   * @param spacing Width of a cell in the grid.
   */
  public Grid(List<Patch> patches, EngineValue spacing) {
    this.patches = patches;
    this.spacing = spacing;
  }

  /**
   * Returns the list of patches in the grid.
   *
   * @return List of patches in the grid.
   */
  public List<Patch> getPatches() {
    return patches;
  }

  /**
   * Returns the width of a cell in the grid.
   *
   * @return Width of a cell in the grid.
   */
  public EngineValue getSpacing() {
    return spacing;
  }
}
