/**
 * Small server which can serve the local editor and run simulations.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud;


/**
 * Undertow server running either Josh Cloud or a self-hosted cloud.
 *
 * <p>Undertow server running either Josh Cloud or a self-hosted cloud which has endpoints for both
 * the worker and leader operations as well as static serving of the editor at the root and a health
 * endpoint which simply responds with 200 healthy. The endpoint for the worker is /runSimulation,
 * the endpoint for the leader is /runReplicates, and the endpoint for health is /health. Runs
 * using HTTP 2.</p>
 */
public class JoshSimServer {

  /**
   * Create a new server for running Josh simluations.
   *
   * @param dataLayer The data layer that should handle API service internals such as logging and
   *     checking API keys.
   * @param useHttp2 Flag indicating if HTTP2 should be used. True if HTTP2 should be used and false
   *     otherwise. If using HTTP2 will use h2c (HTTP/2 cleartext).
   * @param workerUrl The URL at which requests for workers to run a single replicate will be sent.
   *     This may be on the same machine (calls back to this server) or it may go to a different
   *     machine, possibly with a load balancer in the middle.
   * @param port The port number on which the server should listen for requests.
   */
  public JoshSimServer(CloudApiDataLayer dataLayer, boolean useHttp2, String workerUrl, int port) {
    
  }
  
}
