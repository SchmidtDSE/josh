/**
 * Small server which can serve the local editor and run simulations.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.util.Headers;
import java.io.File;
import java.util.Optional;


/**
 * Undertow server running either Josh Cloud or a self-hosted cloud.
 *
 * <p>Undertow server running either Josh Cloud or a self-hosted cloud which has endpoints for both
 * the worker and leader operations as well as static serving of the editor at the root and a health
 * endpoint which simply responds with 200 healthy. The endpoint for the worker is /runReplicate,
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
  private final Undertow server;

  public JoshSimServer(CloudApiDataLayer dataLayer, boolean useHttp2, String workerUrl, int port) {
    PathHandler pathHandler = Handlers.path()
        // Static file handlers
        .addPrefixPath("/", Handlers.resource(new FileResourceManager(new File("editor"), 100))
            .addWelcomeFiles("index.html"))
        .addPrefixPath("/js", Handlers.resource(new FileResourceManager(new File("editor/js"), 100)))
        .addPrefixPath("/style", Handlers.resource(new FileResourceManager(new File("editor/style"), 100)))
        .addPrefixPath("/third_party", Handlers.resource(new FileResourceManager(new File("editor/third_party"), 100)))
        .addPrefixPath("/war", Handlers.resource(new FileResourceManager(new File("editor/war"), 100)))
        
        // API handlers
        .addPrefixPath("/runReplicate", new JoshSimWorkerHandler(dataLayer, true, Optional.empty()))
        .addPrefixPath("/runReplicates", new JoshSimLeaderHandler(dataLayer, workerUrl, 4))
      
        // Health endpoint
        .addPrefixPath("/health", exchange -> {
            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("healthy");
        });

    Undertow.Builder builder = Undertow.builder()
        .addHttpListener(port, "0.0.0.0")
        .setHandler(pathHandler);

    if (useHttp2) {
        builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);
    }

    this.server = builder.build();
  }

  /**
   * Start running this server.
   */
  public void start() {
    this.server.start();
  }

  /**
   * Stop the server and release resources.
   */
  public void stop() {
    if (server != null) {
      server.stop();
    }
  }
  
}
