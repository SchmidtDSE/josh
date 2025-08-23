/**
 * Strategy that offloads leader execution to the remote server.
 *
 * <p>This strategy implements the original behavior of RunRemoteCommand where the remote
 * server's JoshSimLeaderHandler manages all replicate execution and coordination.
 * The client simply sends a single request and processes the streaming response.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.joshsim.lang.io.ExportFacadeFactory;
import org.joshsim.lang.io.InputOutputLayer;
import org.joshsim.lang.io.JvmInputOutputLayerBuilder;

/**
 * Remote execution strategy that offloads leadership to the remote server.
 *
 * <p>This strategy represents the traditional approach where the remote JoshSimLeaderHandler
 * manages all replicate execution and coordination. The local client sends a single HTTP
 * request with all parameters and processes the streaming response containing results from
 * all replicates managed by the remote leader.</p>
 */
public class RunRemoteOffloadLeaderStrategy implements RunRemoteStrategy {

  /**
   * Executes remote simulation by offloading leadership to remote server.
   *
   * <p>This method sends a single HTTP request to the remote JoshSimLeaderHandler endpoint
   * (/runReplicates) and processes the streaming response. The remote server handles all
   * replicate coordination, thread management, and progress aggregation.</p>
   *
   * @param context The execution context containing all necessary parameters
   * @throws IOException if network communication fails
   * @throws InterruptedException if the operation is interrupted
   * @throws RuntimeException if execution fails for other reasons
   */
  @Override
  public void execute(RunRemoteContext context) throws IOException, InterruptedException {
    context.getOutputOptions().printInfo("Using remote leader mode - "
        + "offloading replicate coordination to remote server");

    // Create HTTP client with HTTP/2 support
    HttpClient client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    // Create HTTP request for remote leader
    HttpRequest request = createRemoteLeaderRequest(context);

    context.getOutputOptions().printInfo("Sending simulation request to "
        + context.getEndpointUri());

    // Send request and process streaming response
    HttpResponse<Stream<String>> response = client.send(
        request,
        HttpResponse.BodyHandlers.ofLines()
    );

    if (response.statusCode() != 200) {
      throw new RuntimeException("Remote execution failed with status: "
          + response.statusCode());
    }

    context.getOutputOptions().printInfo(
        "Connected to remote server, processing streaming response...");
    processStreamingResponse(response.body(), context);
  }

  /**
   * Creates the HTTP request for the remote leader endpoint.
   *
   * <p>This method builds the form data needed for the /runReplicates endpoint,
   * including all simulation parameters, external data, and configuration options.
   * The request is sent to the leader handler which manages multiple replicates.</p>
   *
   * @param context The execution context
   * @return The configured HTTP request for the remote leader
   */
  private HttpRequest createRemoteLeaderRequest(RunRemoteContext context) {
    // Build form data for leader endpoint
    String formBody = buildFormData(context);

    return HttpRequest.newBuilder()
        .uri(context.getEndpointUri())
        .header("Content-Type", "application/x-www-form-urlencoded")
        .timeout(Duration.ofMinutes(30))
        .POST(HttpRequest.BodyPublishers.ofString(formBody))
        .build();
  }

  /**
   * Builds the form data for the remote leader request.
   *
   * <p>Creates URL-encoded form data compatible with JoshSimLeaderHandler /runReplicates
   * endpoint. Currently configured for single replicate execution but structured to
   * support multiple replicates in the future.</p>
   *
   * @param context The execution context
   * @return The URL-encoded form request body
   */
  private String buildFormData(RunRemoteContext context) {
    Map<String, String> formData = new HashMap<>();
    formData.put("code", context.getJoshCode());
    formData.put("name", context.getSimulation());
    formData.put("replicates", "1"); // Currently supporting single replicate
    formData.put("apiKey", context.getApiKey());
    formData.put("externalData", context.getExternalDataSerialized());
    formData.put("favorBigDecimal", String.valueOf(!context.isUseFloat64()));

    // URL encode the form data
    StringBuilder formBody = new StringBuilder();
    boolean first = true;
    for (Map.Entry<String, String> entry : formData.entrySet()) {
      if (!first) {
        formBody.append("&");
      }
      first = false;

      try {
        String encodedKey = java.net.URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
        String encodedValue = java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
        formBody.append(encodedKey).append("=").append(encodedValue);
      } catch (Exception e) {
        throw new RuntimeException("Failed to encode form data", e);
      }
    }

    return formBody.toString();
  }

  /**
   * Processes streaming HTTP response from remote leader with progress tracking.
   *
   * <p>Uses the shared RemoteResponseHandler to process each response line consistently
   * with the local leader strategy. The remote leader handles all replicate coordination,
   * so responses may contain data from multiple replicates with appropriate numbering.</p>
   *
   * @param responseStream Stream of response lines from remote leader
   * @param context The execution context containing progress calculator and options
   * @throws RuntimeException if response parsing or data persistence fails
   */
  private void processStreamingResponse(Stream<String> responseStream,
                                       RunRemoteContext context) {
    // Initialize export system using Component 2 infrastructure
    InputOutputLayer ioLayer = new JvmInputOutputLayerBuilder()
        .withReplicate(context.getReplicateNumber())
        .build();
    ExportFacadeFactory exportFactory = ioLayer.getExportFacadeFactory();

    // Create shared response handler - no cumulative progress needed for remote leader
    RemoteResponseHandler responseHandler = new RemoteResponseHandler(
        context, exportFactory, false); // useCumulativeProgress = false

    try {
      responseStream.forEach(line -> {
        // Use shared handler to process each line
        responseHandler.processResponseLine(line, 0, null);
      });

    } finally {
      // Ensure all export facades are properly closed using the shared handler
      responseHandler.closeExportFacades();
    }

    context.getOutputOptions().printInfo("Results saved locally via export facade");
  }
}
