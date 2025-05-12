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

}
