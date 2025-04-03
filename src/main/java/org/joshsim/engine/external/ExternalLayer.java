/**
 * Core interface for all external data layers that can fulfill requests.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.external;

import org.joshsim.engine.value.type.RealizedDistribution;

/**
 * Core interface for all external data layers that can fulfill requests
 * with geospatial constraints.
 */

public interface ExternalLayer {
  /**
   * Fulfills a request for data within a specific geometry.
   *
   * @param request Contains request parameters including geometry
   * @return Distribution of values within the requested geometry
   */
  RealizedDistribution fulfill(Request request);
}
