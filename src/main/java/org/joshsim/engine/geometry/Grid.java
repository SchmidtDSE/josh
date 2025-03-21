package org.joshsim.engine.geometry;

import java.util.List;
import org.joshsim.engine.entity.Patch;
import org.joshsim.engine.value.EngineValue;

/**
 * The Grid class contains a list of patches and a cell width for conveniences.
 * Spatial operations will be performed on the elements of the grid, Patches, which
 * are the basic unit of spatial representation.
 */
public abstract class Grid {
  private List<Patch> patches;
  private EngineValue spacing;
}
