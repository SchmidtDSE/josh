/**
 * Small server which can serve the local editor and run simulations.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud;

import io.undertow.server.HttpHandler;
import java.util.Optional;
import java.util.Set;
import org.joshsim.engine.geometry.EngineGeometryFactory;
import org.joshsim.engine.geometry.grid.GridGeometryFactory;
import org.joshsim.geo.geometry.EarthGeometryFactory;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.io.JvmInputOutputLayer;
import org.joshsim.lang.io.SandboxInputOutputLayer;


/**
 * Handler which calls a JoshSimServer via HTTP 2 to run individual jobs.
 *
 * Handler which calls a JoshSimServer via HTTP to run individual jobs (JoshSimWorkerHandler),
 * sending results from different replicates streamed from HTTP 2 back to the original requester
 * through HTTP 2.
 */
public class JoshSimLeaderHandler implements HttpHandler {

  private final CloudApiDataLayer apiDataLayer;
  private final EngineGeometryFactory geometryFactory;
  private final boolean sandboxed;

  /**
   * Create a new handler for the leader node which is invoked via an HTTP 2 request.
   */
  public JoshSimLeaderHandler(CloudApiDataLayer apiInternalLayer, boolean sandboxed,
        Optional<String> crs) {
    this.apiDataLayer = apiInternalLayer;
    this.sandboxed = sandboxed;

    if (crs.isPresent()) {
      geometryFactory = new EarthGeometryFactory(crs.get());
    } else {
      geometryFactory = new GridGeometryFactory();
    }
  }

  /**
   *
   */
  @Override
  public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

  }

  

}

