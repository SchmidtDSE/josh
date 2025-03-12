/**
 * Structures describing a read only external data soruce.
 * 
 * @license BSD-3-Clause
 */

package org.joshsim.engine.entity;

import org.joshsim.engine.value.Distribution;
import org.joshsim.engine.geometry.Geometry;


/**
 * Immutable external data source.
 * 
 * <p>Represents an external resource entity in the system which provides access to distributed values
 * based on a geometry and attribute-sensitive paths.
 * </p>
 */
public interface ExternalResource extends Entity {
  
  /**
   * Get distribution values for the specified geometry.
   *
   * @param geometry the geometry to query values for
   * @return the distribution of values for the given geometry
   */
  Distribution getDistribution(Geometry geometry);
}