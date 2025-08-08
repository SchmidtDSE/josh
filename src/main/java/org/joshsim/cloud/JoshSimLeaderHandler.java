/**
 * Handler for collating simulation replicate tasks.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.HttpString;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;


/**
 * Handler which calls a JoshSimServer via HTTP 2 to run individual jobs.
 *
 * <p>Handler which calls a JoshSimServer via HTTP to run individual jobs (JoshSimWorkerHandler),
 * sending results from different replicates streamed from HTTP 2 back to the original requester
 * through HTTP 2.</p>
 */
public class JoshSimLeaderHandler implements HttpHandler {

  private final CloudApiDataLayer apiInternalLayer;
  private final String urlToWorker;
  private final int maxParallelRequests;

  /**
   * Create a new leader handler.
   *
   * @param apiInternalLayer The facade to use for checking that API keys are valid before sending
   *     requests to worker with the user-provided API key and the facade in which logging should be
   *     executed.
   * @param urlToWorker String URL at which the worker requests should be executed. This may route
   *     to the same machine or to a different  machine.
   */
  public JoshSimLeaderHandler(CloudApiDataLayer apiInternalLayer, String urlToWorker,
        int maxParallelRequests) {
    this.apiInternalLayer = apiInternalLayer;
    this.urlToWorker = urlToWorker;
    this.maxParallelRequests = maxParallelRequests;
  }

  /**
   * Call up to maxParallelRequests to execute on workers.
   *
   * <p>Execute replicates in parallel by calling the worker URL multiple times, forwarding on the
   * API key, simulation, and simulation name found in the form-encoded body. This will stream
   * results back, splitting results recieved from workers per line and prepending the replicate
   * number to the string being returned such that there is a replicate number per line between
   * brackets like [1] for the second replicate. The prepended string is then streamed back to the
   * requester such that lines remain intact but those complete lines may be interleaved between
   * replicates. No more than maxParallelRequests will be executed at a time with the number of
   * replicates found on the form-encoded body in parameter name replicates.</p>
   *
   * @param httpServerExchange The exchange through which this request should execute.
   */
  @Override
  public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {
    if (httpServerExchange.isInIoThread()) {
      httpServerExchange.dispatch(this);
      return;
    }

    if (!CorsUtil.addCorsHeaders(httpServerExchange)) {
      return;
    }

    long startTime = System.nanoTime();
    Optional<String> apiKey = handleRequestTrusted(httpServerExchange);
    long endTime = System.nanoTime();

    long runtimeSeconds = (endTime - startTime) / 1_000_000_000;
    apiInternalLayer.log(apiKey.orElse(""), "distribute", runtimeSeconds);
  }

  /**
   * Execute a request assuming its environment is set up for blocking.
   *
   * @param httpServerExchange The exchange through which this request should execute.
   * @returns The API key used in the request or empty if rejected.
   */
  public Optional<String> handleRequestTrusted(HttpServerExchange httpServerExchange) {
    if (!httpServerExchange.getRequestMethod().equalToString("POST")) {
      httpServerExchange.setStatusCode(405);
      return Optional.empty();
    }

    httpServerExchange.setStatusCode(200);
    httpServerExchange.getResponseHeaders().put(new HttpString("Content-Type"), "text/plain");
    httpServerExchange.startBlocking();

    FormDataParser parser = FormParserFactory.builder().build().createParser(httpServerExchange);
    if (parser == null) {
      httpServerExchange.setStatusCode(400);
      return Optional.empty();
    }

    FormData formData;
    try {
      formData = parser.parseBlocking();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    ApiKeyUtil.ApiCheckResult apiCheckResult = ApiKeyUtil.checkApiKey(formData, apiInternalLayer);
    if (!apiCheckResult.getKeyIsValid()) {
      httpServerExchange.setStatusCode(401);
      return Optional.empty();
    }
    String apiKey = apiCheckResult.getApiKey();

    boolean hasCode = formData.contains("code");
    boolean hasName = formData.contains("name");
    boolean hasReplicates = formData.contains("replicates");
    boolean hasExternalData = formData.contains("externalData");
    boolean hasFavorBigDecimal = formData.contains("favorBigDecimal");
    boolean hasRequired = (
        hasCode && hasName && hasReplicates && hasExternalData && hasFavorBigDecimal
    );
    if (!hasRequired) {
      httpServerExchange.setStatusCode(400);
      return Optional.of(apiKey);
    }

    String code = formData.getFirst("code").getValue();
    String simulationName = formData.getFirst("name").getValue();
    int replicates;
    try {
      replicates = Integer.parseInt(formData.getFirst("replicates").getValue());
    } catch (NumberFormatException e) {
      httpServerExchange.setStatusCode(400);
      return Optional.of(apiKey);
    }

    if (code == null || simulationName == null || replicates <= 0) {
      httpServerExchange.setStatusCode(400);
      return Optional.of(apiKey);
    }

    String externalData = formData.getFirst("externalData").getValue();
    boolean favorBigDecimal = Boolean.parseBoolean(formData.getFirst("favorBigDecimal").getValue());

    int effectiveThreadCount = Math.min(replicates, maxParallelRequests);
    ExecutorService executor = Executors.newFixedThreadPool(effectiveThreadCount);
    // Execute replicates with streaming approach
    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < replicates; i++) {
      final int replicateNumber = i;
      futures.add(executor.submit(() ->
          executeReplicateStreaming(code, simulationName, replicateNumber,
                                    apiKey, externalData, favorBigDecimal,
                                    httpServerExchange)
      ));
    }

    try {
      // Wait for all replicates to complete
      for (Future<?> future : futures) {
        try {
          future.get();
        } catch (Exception e) {
          System.err.println("Exception in replicate execution: " 
              + e.getClass().getSimpleName() + ": " + e.getMessage());
          // Extract meaningful error message from the exception
          String errorMessage = extractErrorMessage(e);
          String errorOutput = String.format("[error] %s\n", errorMessage);
          synchronized (httpServerExchange.getOutputStream()) {
            httpServerExchange.getOutputStream().write(errorOutput.getBytes());
            httpServerExchange.getOutputStream().flush();
          }
        }
      }
      
      httpServerExchange.endExchange();
    } catch (Exception e) {
      httpServerExchange.setStatusCode(500);
      throw new RuntimeException("Critical error in leader execution: " + e.getMessage(), e);
    } finally {
      executor.shutdown();
    }

    return Optional.of(apiKey);
  }

  /**
   * Extract a meaningful error message from an exception for user display.
   *
   * @param e The exception that occurred during replicate execution.
   * @return A user-friendly error message describing the failure.
   */
  private String extractErrorMessage(Exception e) {
    String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    
    // Check if this is a worker error that already contains detailed information
    if (message.contains("Replicate") && message.contains("failed:")) {
      return message; // Already formatted by buildWorkerErrorMessage
    }
    
    // Handle specific exception types
    if (e.getCause() != null) {
      String causeMessage = e.getCause().getMessage();
      if (causeMessage != null && causeMessage.contains("External data")) {
        return causeMessage;
      }
    }
    
    // Default formatting for other errors
    return String.format("Execution error: %s", message);
  }

  /**
   * Build an informative error message for worker failures.
   *
   * @param statusCode The HTTP status code returned by the worker.
   * @param errorBody The error response body from the worker.
   * @param replicateNumber The replicate number that failed.
   * @return A descriptive error message for the leader to propagate.
   */
  private String buildWorkerErrorMessage(int statusCode, String errorBody, int replicateNumber) {
    switch (statusCode) {
      case 400:
        if (errorBody.contains("External data file not found")) {
          return String.format("Replicate %d failed: %s", replicateNumber, errorBody);
        } else if (errorBody.contains("missing required columns")) {
          return String.format("Replicate %d failed: %s", replicateNumber, errorBody);
        } else if (errorBody.contains("Invalid numeric value")) {
          return String.format("Replicate %d failed: %s", replicateNumber, errorBody);
        } else {
          return String.format(
              "Replicate %d failed with invalid request: %s",
              replicateNumber,
              errorBody
          );
        }
      case 401:
        return String.format(
            "Replicate %d failed: Authentication error - invalid API key",
            replicateNumber
        );
      case 404:
        return String.format(
            "Replicate %d failed: Simulation '%s' not found in the provided code",
            replicateNumber,
            errorBody
        );
      case 405:
        return String.format(
            "Replicate %d failed: Invalid HTTP method - only POST is supported",
            replicateNumber
        );
      case 500:
        if (errorBody.contains("External data file not found")) {
          return String.format("Replicate %d failed: %s", replicateNumber, errorBody);
        } else if (errorBody.contains("External data")) {
          return String.format("Replicate %d failed: %s", replicateNumber, errorBody);
        } else {
          return String.format(
              "Replicate %d failed: Internal server error - %s",
              replicateNumber,
              errorBody
          );
        }
      default:
        return String.format(
            "Replicate %d failed: Worker returned status %d - %s",
            replicateNumber,
            statusCode,
            errorBody
        );
    }
  }

  /**
   * Execute a replicate of the simulation with given parameters.
   *
   * <p>This method constructs a HTTP request to send to the worker server, using the provided
   * code and simulation name. The replicate number is included in the request body.</p>
   *
   * @param code The code to execute for the simulation.
   * @param simulationName The name of the simulation to run.
   * @param replicateNumber The number of the replicate to execute.
   * @param apiKey The API key to include in the request.
   * @param externalData String serialization of external data available to the simulation.
   * @param favorBigDecimal Flag indicating if BigDecimal should be used for numbers. True if
   *     BigDecimal should be used or false for double.
   * @return A string with the result, including the replicate number for each line of output.
   * @throws IOException If an I/O error occurs when sending or receiving.
   * @throws InterruptedException If the operation is interrupted.
   */
  private void executeReplicateStreaming(String code, String simulationName, int replicateNumber,
        String apiKey, String externalData, boolean favorBigDecimal,
        HttpServerExchange clientExchange) {
    String bodyString = String.format(
        "code=%s&name=%s&apiKey=%s&externalData=%s&favorBigDecimal=%s",
        URLEncoder.encode(code, StandardCharsets.UTF_8),
        URLEncoder.encode(simulationName, StandardCharsets.UTF_8),
        URLEncoder.encode(apiKey, StandardCharsets.UTF_8),
        URLEncoder.encode(externalData, StandardCharsets.UTF_8),
        URLEncoder.encode(favorBigDecimal ? "true" : "false", StandardCharsets.UTF_8)
    );
    HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofString(bodyString);

    HttpClient client = HttpClient.newBuilder().build();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(urlToWorker))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(body)
        .build();

    HttpResponse<Stream<String>> response = null;
    try {
      response = client.send(request, HttpResponse.BodyHandlers.ofLines());
    } catch (IOException | InterruptedException e) {
      System.err.println("Worker connection failed for replicate " + replicateNumber 
          + " to " + urlToWorker + ": " + e.getMessage());
      throw new RuntimeException("Encountered issue in worker thread: " + e);
    }

    if (response.statusCode() == 200) {
      try {
        response.body().forEach(line -> {
          try {
            String outputLine;
            if (line.startsWith("[progress ")) {
              // Pass through progress messages without modification
              outputLine = line + "\n";
            } else {
              // Prepend replicate number to data lines
              outputLine = String.format("[%d] %s\n", replicateNumber, line);
            }

            // Thread-safe write to client
            synchronized (clientExchange.getOutputStream()) {
              clientExchange.getOutputStream().write(outputLine.getBytes());
              clientExchange.getOutputStream().flush();
            }
          } catch (IOException e) {
            throw new RuntimeException("Error streaming to client", e);
          }
        });

        // Send end marker
        synchronized (clientExchange.getOutputStream()) {
          String endMarker = String.format("[end %d]\n", replicateNumber);
          clientExchange.getOutputStream().write(endMarker.getBytes());
          clientExchange.getOutputStream().flush();
        }
      } catch (Exception e) {
        throw new RuntimeException("Error processing response stream", e);
      }
    } else {
      // Handle different error status codes from worker with informative messages
      String errorBody = "No error details available for streaming response";
      String errorMessage = buildWorkerErrorMessage(
          response.statusCode(),
          errorBody,
          replicateNumber
      );
      throw new RuntimeException(errorMessage);
    }
  }

}
