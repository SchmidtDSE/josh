/**
 * HTTP-based preprocessing target.
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
 * Dispatches preprocessing jobs to a joshsim server via HTTP POST to {@code /preprocessBatch}.
 *
 * <p>Sends a form-encoded POST request with preprocessing parameters. The server returns
 * 202 Accepted immediately and executes preprocessing asynchronously. Status is tracked
 * via MinIO status files, not the HTTP response.</p>
 */
public class HttpPreprocessTarget implements RemotePreprocessTarget {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

  private final String endpoint;
  private final String apiKey;
  private final HttpClient httpClient;

  /**
   * Constructs an HttpPreprocessTarget from an HTTP target config.
   *
   * @param config The HTTP target configuration containing endpoint and API key.
   */
  public HttpPreprocessTarget(HttpTargetConfig config) {
    this(config.getEndpoint(), config.getApiKey());
  }

  /**
   * Constructs an HttpPreprocessTarget with explicit endpoint and API key.
   *
   * @param endpoint The joshsim server base URL.
   * @param apiKey The API key for authentication.
   */
  public HttpPreprocessTarget(String endpoint, String apiKey) {
    this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1)
        : endpoint;
    this.apiKey = apiKey;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(CONNECT_TIMEOUT)
        .build();
  }

  /**
   * Constructs an HttpPreprocessTarget with an injected HttpClient (for testing).
   *
   * @param endpoint The joshsim server base URL.
   * @param apiKey The API key for authentication.
   * @param httpClient The HTTP client to use for requests.
   */
  HttpPreprocessTarget(String endpoint, String apiKey, HttpClient httpClient) {
    this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1)
        : endpoint;
    this.apiKey = apiKey;
    this.httpClient = httpClient;
  }

  @Override
  public void dispatch(String jobId, String minioPrefix, String simulation,
      PreprocessParams params) throws Exception {
    Map<String, String> formFields = new LinkedHashMap<>();
    formFields.put("apiKey", apiKey);
    formFields.put("jobId", jobId);
    formFields.put("simulation", simulation);
    formFields.put("dataFile", params.getDataFile());
    formFields.put("variable", params.getVariable());
    formFields.put("units", params.getUnits());
    formFields.put("outputFile", params.getOutputFile());
    formFields.put("workDir", "/tmp/batch-" + jobId);
    formFields.put("stageFromMinio", "true");
    formFields.put("minioPrefix", minioPrefix);

    // Optional preprocess fields
    formFields.put("crs", params.getCrs());
    formFields.put("xCoord", params.getHorizCoord());
    formFields.put("yCoord", params.getVertCoord());
    formFields.put("timeDim", params.getTimeDim());
    if (params.getTimestep() != null && !params.getTimestep().isEmpty()) {
      formFields.put("timestep", params.getTimestep());
    }
    if (params.getDefaultValue() != null && !params.getDefaultValue().isEmpty()) {
      formFields.put("defaultValue", params.getDefaultValue());
    }
    if (params.isParallel()) {
      formFields.put("parallel", "true");
    }
    if (params.isAmend()) {
      formFields.put("amend", "true");
    }

    String formBody = formFields.entrySet().stream()
        .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
            + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
        .collect(Collectors.joining("&"));

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(endpoint + "/preprocessBatch"))
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
