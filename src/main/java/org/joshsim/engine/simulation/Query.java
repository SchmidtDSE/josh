/**
 * Strucutres describing geospatial queries.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.simulation;

import java.util.Optional;
import org.joshsim.engine.geometry.Geometry;


/**
 * A spatial and / or temporal query which can be used to look up Patches.
 *
 * <p>A query which refers to a specific geospatial geometry at a specific time. This provides
 * methods to retrieve geometry and time step information for querying Patches.</p>
 */
public class Query {

  private final int step;
  private final Optional<Geometry> geometry;

  /**
   * Create a query which selects for all Patches in a given time step.
   *
   * @param step unique step count or time step ID for which to query.
   */
  public Query(int step) {
    this.step = step;
    this.geometry = Optional.empty();
  }

  /**
   * Create a query which selects for both a specific time step count and geometry boundary.
   *
   * @param step unique step count or time step ID for which to query.
   * @param geometry spatial bounds within which patches should be returned.
   */
  public Query(int step, Geometry geometry) {
    this.step = step;
    this.geometry = Optional.of(geometry);
  }

  /**
   * Get the time step associated with this query.
   *
   * @return the time step index for this query.
   */
  public int getStep() {
    return step;
  }
  
  /**
   * Get the geometry associated with this query.
   *
   * @return the geometry that defines the spatial bounds of the query.
   */
  public Optional<Geometry> getGeometry() {
    return geometry;
  }

}
