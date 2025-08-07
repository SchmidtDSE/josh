
/**
 * Command line interface handler for running the JoshSim local web server.
 *
 * <p>This class implements the 'server' command which starts a local web server to provide
 * a browser-based interface for running Josh simulations. It supports configuration of worker
 * threads, HTTP/2, and patch processing modes.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import org.joshsim.cloud.EnvCloudApiDataLayer;
import org.joshsim.cloud.JoshSimServer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;


/**
 * Command handler for running the local JoshSim web server.
 *
 * <p>Processes command line arguments to configure and start a local web server that provides
 * a browser-based interface for Josh simulations. Supports configuration of worker threads,
 * HTTP/2 protocol, and parallel/serial patch processing modes.</p>
 */
@Command(
    name = "server",
    description = "Run the JoshSim server locally"
)
public class ServerCommand implements Callable<Integer> {
  @Option(names = "--port", description = "Port number for the server", defaultValue = "8085")
  private int port;

  @Option(
      names = "--concurrent-workers",
      description = "Number of concurrent workers allowed",
      defaultValue = "1"
  )
  private int workers;

  @Option(
      names = "--worker-url",
      description = "URL for worker requests",
      defaultValue = "http://0.0.0.0:8085/runReplicate"
  )
  private String workerUrl;

  @Option(names = "--use-http2", description = "Enable HTTP/2 support", defaultValue = "false")
  private boolean useHttp2;

  @Option(
      names = "--serial-patches",
      description = "Run patches in serial instead of parallel",
      defaultValue = "false"
  )
  private boolean serialPatches;

  @Override
  public Integer call() {
    try {
      int numProcessors = Runtime.getRuntime().availableProcessors();

      // Fix worker URL: replace 0.0.0.0 with localhost and update port to match server port
      String processedWorkerUrl = workerUrl.replaceAll("\"", "").trim();
      if (processedWorkerUrl.contains("0.0.0.0")) {
        processedWorkerUrl = processedWorkerUrl.replace("0.0.0.0", "localhost");
        System.out.println("Updated worker URL from 0.0.0.0 to localhost");
      }
      
      // Update port in worker URL to match server port using proper URL parsing
      if (processedWorkerUrl.contains("localhost")) {
        try {
          URL url = new URL(processedWorkerUrl);
          if (url.getPort() != port) {
            processedWorkerUrl = String.format(
                "%s://%s:%d%s", 
                url.getProtocol(),
                url.getHost(),
                port,
                url.getPath()
            );
            String message = String.format(
                "Updated worker URL port to match server port: %s",
                processedWorkerUrl
            );
            System.out.println(message);
          }
        } catch (MalformedURLException e) {
          String message = String.format(
              "Warning: Could not parse worker URL: %s. Using %s.",
              e.getMessage(),
              processedWorkerUrl
          );
          System.out.println(message);
        }
      }
      
      if (workers == 0) {
        workers = processedWorkerUrl.contains("localhost") ? 1 : numProcessors - 1;
      }
      
      JoshSimServer server = new JoshSimServer(
          new EnvCloudApiDataLayer(),
          useHttp2,
          processedWorkerUrl,
          port,
          workers,
          serialPatches
      );

      server.start();
      System.out.println("Server started on port " + port);
      System.out.println(
          "Open your browser at http://localhost:" + port + "/ to run simulations"
      );

      Thread.currentThread().join();
      return 0;
    } catch (Exception e) {
      System.err.println("Server error: " + e.getMessage());
      return 1;
    }
  }
}
