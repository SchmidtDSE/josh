/**
 * HTTP-based batch execution target.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Dispatches batch jobs to a joshsim server via HTTP POST to {@code /runBatch}.
 *
 * <p>Sends a form-encoded POST request with job parameters. The server returns
 * 202 Accepted immediately and executes the simulation asynchronously. Status
 * is tracked via MinIO status files, not the HTTP response.</p>
 */
public class HttpBatchTarget implements RemoteBatchTarget {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

  private final String endpoint;
  private final String apiKey;
  private final HttpClient httpClient;

  /**
   * Constructs an HttpBatchTarget from an HTTP target config.
   *
   * @param config The HTTP target configuration containing endpoint and API key.
   */
  public HttpBatchTarget(HttpTargetConfig config) {
    this(config.getEndpoint(), config.getApiKey());
  }

  /**
   * Constructs an HttpBatchTarget with explicit endpoint and API key.
   *
   * @param endpoint The joshsim server base URL (e.g., {@code https://josh-executor.run.app}).
   * @param apiKey The API key for authentication.
   */
  public HttpBatchTarget(String endpoint, String apiKey) {
    this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1)
        : endpoint;
    this.apiKey = apiKey;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .build();
  }

  /**
   * Constructs an HttpBatchTarget with an injected HttpClient (for testing).
   *
   * @param endpoint The joshsim server base URL.
   * @param apiKey The API key for authentication.
   * @param httpClient The HTTP client to use for requests.
   */
  HttpBatchTarget(String endpoint, String apiKey, HttpClient httpClient) {
    this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1)
        : endpoint;
    this.apiKey = apiKey;
    this.httpClient = httpClient;
  }

  @Override
  public void dispatch(String jobId, String minioPrefix, String simulation, int replicates)
      throws Exception {
    Map<String, String> formFields = new LinkedHashMap<>();
    formFields.put("apiKey", apiKey);
    formFields.put("jobId", jobId);
    formFields.put("simulation", simulation);
    formFields.put("replicates", String.valueOf(replicates));
    formFields.put("workDir", "/tmp/batch-" + jobId);
    formFields.put("stageFromMinio", "true");
    formFields.put("minioPrefix", minioPrefix);

    String formBody = formFields.entrySet().stream()
        .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
            + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(endpoint + "/runBatch"))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .timeout(REQUEST_TIMEOUT)
        .POST(HttpRequest.BodyPublishers.ofString(formBody))
        .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 202) {
      throw new RuntimeException(
          "Dispatch failed (HTTP " + response.statusCode() + "): " + response.body()
      );
    }
  }
}
