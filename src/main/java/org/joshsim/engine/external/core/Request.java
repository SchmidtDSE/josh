/**
 * Request object containing all parameters needed to fetch external data.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.engine.external.core;

import java.util.Optional;
import org.joshsim.engine.geometry.EngineGeometry;


/**
 * Interface representing a request to fetch external data.
 */
public class Request {
  private String protocol;
  private String host;
  private String path;
  private Optional<EngineGeometry> geometry;
  private Optional<EngineGeometry> primingGeometry = Optional.empty();
  private Optional<String> resource;

  /**
   * Constructor for the Request class.
   *
   * @param protocol Protocol used for the request (file, http, etc.)
   * @param host Host or location to query
   * @param path Path to the resource (e.g., file path, URL)
   * @param geometry EngineGeometry for which to fetch data
   * @param resource Resource identifier or name
   */
  public Request(
      String protocol,
      String host,
      String path,
      Optional<EngineGeometry> geometry,
      Optional<String> resource
  ) {
    this.protocol = protocol;
    this.host = host;
    this.path = path;
    this.geometry = geometry;
    this.resource = resource;
  }

  /**
   * Returns the protocol used for the request.
   *
   * @return Protocol used for the request (file, http, etc.)
   */
  public String getProtocol() {
    return protocol;
  }

  /**
   * Returns the host or location to query.
   *
   * @return Host or location to query
   */
  public String getHost() {
    return host;
  }

  /**
   * Returns the path to the resource (e.g., file path, URL).
   *
   * @return Path to the resource
   */
  public String getPath() {
    return path;
  }

  /**
   * Returns the geometry for which to fetch data.
   *
   * @return EngineGeometry defining the area of interest
   */
  public Optional<EngineGeometry> getGeometry() {
    return geometry;
  }

  /**
   * Returns the resource identifier or name, for further subsetting the data if needed.
   *
   * @return Optional resource identifier or name
   */
  public Optional<String> getResource() {
    return resource;
  }

  /**
   * Sets the priming geometry, which is assumed to be a superset of the current request's
   * geometry, to be used in caching operations.
   */
  public void setPrimingGeometry(Optional<EngineGeometry> geometry) {
    primingGeometry = geometry;
  }

  /**
   * Returns the priming geometry, which is assumed to be a superset of the current request's
   * geometry, to be used in caching operations.
   */
  public Optional<EngineGeometry> getPrimingGeometry() {
    return primingGeometry;
  }
}
