package org.joshsim.cloud;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * Handler which calls a JoshSimServer via HTTP 2 to run individual jobs.
 *
 * <p>Handler which calls a JoshSimServer via HTTP to run individual jobs (JoshSimWorkerHandler),
 * sending results from different replicates streamed from HTTP 2 back to the original requester
 * through HTTP 2.</p>
 */
public class JoshSimLeaderHandler {

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
    String apiKey = httpServerExchange.getRequestHeaders().get("api-key").getFirst();

    if (apiKey == null || !apiInternalLayer.apiKeyIsValid(apiKey)) {
      httpServerExchange.setStatusCode(401);
      return;
    }

    long startTime = System.nanoTime();
    handleRequestTrusted(httpServerExchange);
    long endTime = System.nanoTime();

    long runtimeSeconds = (endTime - startTime) / 1_000_000_000;
    apiInternalLayer.log(apiKey, "distribute", runtimeSeconds);
  }

  /**
   * Execute a request without interacting with the API service internals.
   *
   * <p>Execute a request without interacting with the API service inernals as described in
   * handleRequest which checks the API key and reports logging.</p>
   *
   * @param httpServerExchange The exchange through which this request should execute.
   */
  public void handleRequestTrusted(HttpServerExchange httpServerExchange) {
    if (!httpServerExchange.getRequestMethod().equalToString("POST")) {
      httpServerExchange.setStatusCode(405);
      return;
    }

    httpServerExchange.setStatusCode(200);
    httpServerExchange.getResponseHeaders().put(new HttpString("Content-Type"), "text/plain");
    httpServerExchange.startBlocking();

    FormDataParser parser = FormParserFactory.builder().build().createParser(httpServerExchange);
    if (parser == null) {
      httpServerExchange.setStatusCode(400);
      return;
    }

    FormData formData;
    try {
      formData = parser.parseBlocking();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    boolean hasCode = formData.contains("code");
    boolean hasName = formData.contains("name");
    boolean hasReplicates = formData.contains("replicates");
    boolean hasRequired = hasCode && hasName && hasReplicates;
    if (!hasRequired) {
      httpServerExchange.setStatusCode(400);
      return;
    }

    String code = formData.getFirst("code").getValue();
    String simulationName = formData.getFirst("name").getValue();
    int replicates;
    try {
      replicates = Integer.parseInt(formData.getFirst("replicates").getValue());
    } catch (NumberFormatException e) {
      httpServerExchange.setStatusCode(400);
      return;
    }

    if (code == null || simulationName == null || replicates <= 0) {
      httpServerExchange.setStatusCode(400);
      return;
    }

    int effectiveThreadCount = Math.min(replicates, maxParallelRequests);
    ExecutorService executor = Executors.newFixedThreadPool(effectiveThreadCount);
    List<Future<String>> futures = new ArrayList<>();

    for (int i = 0; i < replicates; i++) {
      final int replicateNumber = i;
      futures.add(executor.submit(() -> executeReplicate(code, simulationName, replicateNumber)));
    }

    try {
      for (Future<String> future : futures) {
        String result = future.get();
        if (result != null) {
          httpServerExchange.getOutputStream().write(result.getBytes());
          httpServerExchange.getOutputStream().flush();
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      executor.shutdown();
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
   * @return A string with the result, including the replicate number for each line of output.
   * @throws IOException If an I/O error occurs when sending or receiving.
   * @throws InterruptedException If the operation is interrupted.
   */
  private String executeReplicate(String code, String simulationName, int replicateNumber) {
    HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.ofString(
      String.format("code=%s&name=%s", 
          URLEncoder.encode(code, StandardCharsets.UTF_8),
          URLEncoder.encode(simulationName, StandardCharsets.UTF_8)
      )
    );
    
    HttpClient client = HttpClient.newBuilder().build();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(urlToWorker))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(body)
        .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() == 200) {
      String[] lines = response.body().split("\n");
      StringBuilder result = new StringBuilder();
      for (String line : lines) {
        result.append(String.format("[%d] %s\n", replicateNumber, line));
      }
      return result.toString();
    }
  }
  
}
