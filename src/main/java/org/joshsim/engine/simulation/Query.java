/**
 * Strucutres describing geospatial queries.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.simulation;

import org.joshsim.engine.geometry.Geometry;


/**
 * A spatial and temporal query.
 *
 * <p>A query which refers to a specific geospatial geometry at a specific time. This provides
 * methods to retrieve geometry and time step information for querying entities.</p>
 */
public interface Query {
  /**
   * Get the geometry associated with this query.
   *
   * @return the geometry that defines the spatial bounds of the query
   */
  Geometry getGeometry();

  /**
   * Get the time step associated with this query.
   *
   * @return the time step index for this query
   */
  int getStep();
}
