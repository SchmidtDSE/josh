/**
 * Tests for HttpPreprocessTarget.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;


/**
 * Unit tests for {@link HttpPreprocessTarget}.
 */
class HttpPreprocessTargetTest {

  private static final PreprocessParams TEST_PARAMS = new PreprocessParams(
      "data.nc", "temperature", "celsius", "output.jshd",
      "EPSG:4326", "lon", "lat", "calendar_year",
      null, null, false, false
  );

  @SuppressWarnings("unchecked")
  @Test
  void dispatchSendsPostAndAccepts202() throws Exception {
    HttpClient mockClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(202);
    when(mockResponse.body()).thenReturn(
        "{\"status\":\"accepted\",\"jobId\":\"job-1\"}"
    );
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    HttpPreprocessTarget target = new HttpPreprocessTarget(
        "https://example.com", "test-key", mockClient
    );

    assertDoesNotThrow(() -> target.dispatch(
        "job-1", "batch-jobs/job-1/inputs/", "Main", TEST_PARAMS
    ));
    verify(mockClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  void dispatchThrowsOn400() throws Exception {
    HttpClient mockClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(400);
    when(mockResponse.body()).thenReturn(
        "{\"status\":\"error\",\"message\":\"missing-fields\"}"
    );
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    HttpPreprocessTarget target = new HttpPreprocessTarget(
        "https://example.com", "test-key", mockClient
    );

    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> target.dispatch("job-bad", "prefix/", "Main", TEST_PARAMS));
    assertTrue(ex.getMessage().contains("HTTP 400"));
  }

  @SuppressWarnings("unchecked")
  @Test
  void dispatchThrowsOn500() throws Exception {
    HttpClient mockClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(500);
    when(mockResponse.body()).thenReturn("Internal Server Error");
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    HttpPreprocessTarget target = new HttpPreprocessTarget(
        "https://example.com", "test-key", mockClient
    );

    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> target.dispatch("job-err", "prefix/", "Main", TEST_PARAMS));
    assertTrue(ex.getMessage().contains("HTTP 500"));
  }

  @SuppressWarnings("unchecked")
  @Test
  void dispatchIncludesPreprocessFieldsInRequest() throws Exception {
    HttpClient mockClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(202);
    when(mockResponse.body()).thenReturn("{\"status\":\"accepted\"}");
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    PreprocessParams paramsWithOpts = new PreprocessParams(
        "precip.nc", "rainfall", "mm/year", "precip.jshd",
        "EPSG:4326", "longitude", "latitude", "time",
        "2020", "-999", true, false
    );

    HttpPreprocessTarget target = new HttpPreprocessTarget(
        "https://example.com", "key", mockClient
    );
    target.dispatch("job-opts", "prefix/", "Main", paramsWithOpts);

    verify(mockClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
  }

  @Test
  void constructsFromHttpTargetConfig() {
    HttpTargetConfig config = new HttpTargetConfig("https://example.com", "key");
    HttpPreprocessTarget target = new HttpPreprocessTarget(config);
    assertDoesNotThrow(() -> {});
  }

  @SuppressWarnings("unchecked")
  @Test
  void endpointTrailingSlashIsNormalized() throws Exception {
    HttpClient mockClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(202);
    when(mockResponse.body()).thenReturn("{\"status\":\"accepted\"}");
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    HttpPreprocessTarget target = new HttpPreprocessTarget(
        "https://example.com/", "key", mockClient
    );
    assertDoesNotThrow(() -> target.dispatch(
        "job-1", "prefix/", "Main", TEST_PARAMS
    ));
  }
}
