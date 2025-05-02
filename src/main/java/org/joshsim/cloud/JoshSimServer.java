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
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.MimeMappings;
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

  /**
   * Constructs a JoshSimServer instance.
   *
   * @param dataLayer The instance of CloudApiDataLayer that handles internal API logic like
   *     validation and logging.
   * @param useHttp2 A boolean flag indicating whether the server should enable HTTP/2 support.
   * @param workerUrl The URL of the worker service to which tasks may be delegated.
   * @param port The port on which the server listens for HTTP requests.
   * @param maxParallelRequests The maximum parallel requests to allow in leaders.
   * @param serialPatches Flag indicating if patches should be processed in serial. True if serial
   *     and false if parallel.
   */
  public JoshSimServer(CloudApiDataLayer dataLayer, boolean useHttp2, String workerUrl, int port,
      int maxParallelRequests, boolean serialPatches) {
    PathHandler pathHandler = Handlers.path()
        .addPrefixPath("/", exchange -> {
          // Set common CORS headers for all requests
          exchange.getResponseHeaders().put(
              new HttpString("Access-Control-Allow-Origin"),
              "*"
          );
          exchange.getResponseHeaders().put(
              new HttpString("Access-Control-Allow-Methods"),
              "GET, POST, PUT, DELETE, OPTIONS"
          );
          exchange.getResponseHeaders().put(
              new HttpString("Access-Control-Allow-Headers"),
              "Content-Type, Authorization"
          );
          
          if (exchange.getRequestMethod().toString().equals("OPTIONS")) {
            exchange.getResponseHeaders().put(
                new HttpString("Access-Control-Max-Age"),
                "3600"
            );
            exchange.setStatusCode(200);
            exchange.endExchange();
            return;
          }
        })
        // Static file handlers
        .addPrefixPath(
            "/",
            Handlers.resource(
                new ClassPathResourceManager(JoshSimServer.class.getClassLoader(), "editor")
            ).addWelcomeFiles("index.html")
        )
        .addPrefixPath(
            "/js",
            Handlers.resource(
                new ClassPathResourceManager(JoshSimServer.class.getClassLoader(), "editor/js")
            )
        )
        .addPrefixPath(
            "/style",
            Handlers.resource(
                new ClassPathResourceManager(JoshSimServer.class.getClassLoader(), "editor/style")
            )
        )
        .addPrefixPath(
            "/third_party",
            Handlers.resource(
                new ClassPathResourceManager(
                    JoshSimServer.class.getClassLoader(),
                    "editor/third_party"
                )
            )
        )
        .addPrefixPath(
            "/war",
            Handlers.resource(
                new ClassPathResourceManager(JoshSimServer.class.getClassLoader(), "editor/war")
            ).setMimeMappings(
                MimeMappings.builder()
                .addMapping("wasm", "application/wasm")
                .build()
            )
        )

        // API handlers
        .addPrefixPath(
            "/runReplicate",
            new JoshSimWorkerHandler(dataLayer, true, Optional.empty(), serialPatches)
        )
        .addPrefixPath(
            "/runReplicates",
            new JoshSimLeaderHandler(dataLayer, workerUrl, maxParallelRequests)
        )

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