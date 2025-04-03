/**
 * Request object containing all parameters needed to fetch external data.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.external.core;

import java.util.Optional;
import org.joshsim.engine.geometry.Geometry;

/**
 * Interface representing a request to fetch external data.
 */
public interface Request {
  /**
   * Returns the protocol used for the request.
   *
   * @return Protocol used for the request (file, http, etc.)
   */
  String getProtocol();
  
  /**
   * Returns the host or location to query.
   *
   * @return Host or location to query
   */
  String getHost();
  
  /**
   * Returns the path to the resource (e.g., file path, URL).
   *
   * @return Path to the resource
   */
  String getPath();
  
  /**
   * Returns the geometry for which to fetch data.
   *
   * @return Geometry defining the area of interest
   */
  Optional<Geometry> getGeometry();

  /**
   * Returns the resource identifier or name, for further subsetting the data if needed.
   *
   * @return Optional resource identifier or name
   */
  Optional<String> getResource();
}