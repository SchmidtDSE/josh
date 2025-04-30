
package org.joshsim.cloud;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class JoshSimLeaderHandler implements HttpHandler {

  private final CloudApiDataLayer apiInternalLayer;
  private final String urlToWorker;
  private final int maxParallelRequests;
  private final HttpClient httpClient;
  private final ExecutorService executorService;

  public JoshSimLeaderHandler(CloudApiDataLayer apiInternalLayer, String urlToWorker,
      int maxParallelRequests) {
    this.apiInternalLayer = apiInternalLayer;
    this.urlToWorker = urlToWorker;
    this.maxParallelRequests = maxParallelRequests;
    this.httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    this.executorService = Executors.newFixedThreadPool(maxParallelRequests);
  }

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

  public void handleRequestTrusted(HttpServerExchange httpServerExchange) throws Exception {
    if (!httpServerExchange.getRequestMethod().equalToString("POST")) {
      httpServerExchange.setStatusCode(405);
      return;
    }

    httpServerExchange.startBlocking();
    FormDataParser parser = FormParserFactory.builder().build().createParser(httpServerExchange);
    
    if (parser == null) {
      httpServerExchange.setStatusCode(400);
      return;
    }

    FormData formData = parser.parseBlocking();
    if (!formData.contains("code") || !formData.contains("name") || !formData.contains("replicates")) {
      httpServerExchange.setStatusCode(400);
      return;
    }

    String code = formData.getFirst("code").getValue();
    String simulationName = formData.getFirst("name").getValue();
    int replicates = Integer.parseInt(formData.getFirst("replicates").getValue());

    if (code == null || simulationName == null || replicates <= 0) {
      httpServerExchange.setStatusCode(400);
      return;
    }

    httpServerExchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
    OutputStream outputStream = httpServerExchange.getOutputStream();

    // Create batches of requests to respect maxParallelRequests
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    for (int i = 0; i < replicates; i++) {
      final int replicateNumber = i;
      
      // Build request to worker
      String requestBody = "code=" + code + "&name=" + simulationName;
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(urlToWorker))
          .header("Content-Type", "application/x-www-form-urlencoded")
          .header("api-key", httpServerExchange.getRequestHeaders().get("api-key").getFirst())
          .POST(HttpRequest.BodyPublishers.ofString(requestBody))
          .build();

      // Execute request and handle streaming response
      CompletableFuture<Void> future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
          .thenAcceptAsync(response -> {
            try {
              response.body().forEach(line -> {
                try {
                  outputStream.write(("[" + replicateNumber + "] " + line + "\n").getBytes());
                  outputStream.flush();
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          }, executorService);

      futures.add(future);

      // Wait for batch to complete if we've hit maxParallelRequests
      if (futures.size() >= maxParallelRequests) {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        futures.clear();
      }
    }

    // Wait for any remaining requests
    if (!futures.isEmpty()) {
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
  }
}
