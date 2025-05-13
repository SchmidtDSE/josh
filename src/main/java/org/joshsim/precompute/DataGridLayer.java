/**
 * Interface for pre-computed data grids like those loaded from jshd files.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.precompute;

import org.joshsim.engine.entity.base.GeoKey;
import org.joshsim.engine.geometry.PatchBuilderExtents;
import org.joshsim.engine.value.converter.Units;
import org.joshsim.engine.value.type.EngineValue;


/**
 * Grid which includes precomputed data for Josh simulations.
 *
 * <p>Grid which includes precomputed data for Josh simulations in a structure optimized for Josh
 * access, specifically pre-segmented into grid space and timesteps. This reduces repetitive
 * geospatial computation when running replicates.</p>
 */
public interface DataGridLayer {

  /**
   * Get the value for the grid at a specific location and time.
   *
   * @param location The key for the location in grid-space where the value is requested.
   * @param timestep The timestep at which the the value is requested.
   * @returns The value found at this position.
   */
  EngineValue getAt(GeoKey location, long timestep);

  /**
   * Determine if a set of extents is compatible with this pre-computed dataset.
   *
   * @param extents The grid space ot check
   * @param minTimestep The minimum timestep that needs to be available.
   * @param maxTimestep The maximum timestep that needs to be available.
   * @returns True if compatible and can be used with this grid and false otherwise.
   */
  boolean isCompatible(PatchBuilderExtents extents, long minTimestep, long maxTimestep);

  /**
   * Get the units that are used in generating values from this grid.
   *
   * @returns Units that are reported for values in the grid.
   */
  Units getUnits();

  /**
   * Get the minimum X coordinate of this grid.
   *
   * @return The minimum X coordinate in grid-space.
   */
  long getMinX();

  /**
   * Get the minimum Y coordinate of this grid.
   *
   * @return The minimum Y coordinate in grid-space.
   */
  long getMinY();

  /**
   * Get the maximum X coordinate of this grid.
   *
   * @return The maximum X coordinate in grid-space.
   */
  long getMaxX();

  /**
   * Get the maximum Y coordinate of this grid.
   *
   * @return The maximum Y coordinate in grid-space.
   */
  long getMaxY();

  /**
   * Get the width of this grid in number of horizontal patches.
   *
   * @return The x size of this grid in grid-space.
   */
  long getWidth();

  /**
   * Get the height of this grid in number of horizontal patches.
   *
   * @return The y size of this grid in grid-space.
   */
  long getHeight();

  /**
   * The earliest timestep available in this grid.
   *
   * @return The minimum available timestep.
   */
  long getMinTimestep();

  /**
   * The latest timestep available in this grid.
   *
   * @return The maximum available timestep.
   */
  long getMaxTimestep();

}
