
/**
 * Utility to combine two DataGridLayers.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.engine.EngineValueFactory;
import org.joshsim.engine.value.type.EngineValue;

/**
 * Utility to combine two DataGridLayers.
 *
 * <p>Utility which combines two DataGridLayers by specifying which one's values should take
 * precedence if there is a collision. Note that this specifically combines into a
 * DoublePrecomputedGrid.</p>
 */
public class GridCombiner {

  /**
   * Combine two DataGridLayers into a single DataGridLayer.
   *
   * <p>Combine two DataGridLayers together into a single DataGridLayer where values from the right
   * grid are used if there is a conflict. Note that this specifically combines into a
   * DoublePrecomputedGrid so values are held in memory.</p>
   *
   * @param left The first DataGridLayer to combine and from which values are overwritten by right
   *     in the case of a conflict.
   * @param right The second DataGridLayer to combine and which takes precedence if there is a
   *     conflict.
   * @return Newly created DataGridLayer which combines the two input layers.
   */
  public static DataGridLayer combine(DataGridLayer left, DataGridLayer right) {

  }

  /**
   * Determine the combined superset of extents for the two given grids.
   *
   * <p>Determine the combined extents for the two given grids. Note that, in the case that one
   * grid extents does not fit fully within another, the minimum and maximum values of the returned
   * extents will be expanded to encompass both.</p>
   *
   * @param left The first DataGridLayer from which the fully enclosing extents should be built.
   * @param right The second DataGridLayer from which the fully enclosing extents should be built.
   * @return Extents which fully enclose the extents of both input grids. These extents are to be
   *     be given in grid-space.
   */
  static PatchBuilderExtents getCombinedExtents(DataGridLayer left, DataGridLayer right) {

  }

  /**
   * Get the minimum timestep across both input grids.
   *
   * @param left The first grid from which the minimum timestep should be determined.
   * @param right The second grid from which the minimum timestep should be determined.
   * @return The minimum of the minimum timestep across both input grids.
   */
  private static long getMinTimestep(DataGridLayer left, DataGridLayer right) {

  }

  /**
   * Get the maximum timestep across both input grids.
   *
   * @param left The first grid from which the maximum timestep should be determined.
   * @param right The second grid from which the maximum timestep should be determined.
   * @return The maximum of the maximum timestep across both input grids.
   */
  static long getMaxTimestep(DataGridLayer left, DataGridLayer right) {

  }

  /**
   * Get the units that should be used for the combined grid.
   *
   * <p>Get the units that should be used for the combined grid which, at this time, requires that
   * the two are precisely equal.</p>
   *
   * @param left The first grid from which the units should be determined.
   * @param right The first grid from which the units should be determined.
   * @return The units from the right grid.
   * @throws IllegalArgumentException If the units of the two input grids are not compatible.
   */
  static Units getUnits(DataGridLayer left, DataGridLayer right) {

  }

  /**
   * Add in values from a source grid into the combined grid.
   *
   * <p>Add in values from the given source grid into the given combined grid. This asusmes that the
   * combinedGrid extents, time range, units, and type are compatible or a superset of the
   * source.</p>
   *
   * @param combinedGrid The grid into which all values from source will be copied.
   * @param source The grid from which values will be copied into combinedGrid.
   */
  static void addInValues(DoublePrecomputedGrid combinedGrid, DataGridLayer source) {

  }
}
