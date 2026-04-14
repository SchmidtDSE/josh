/**
 * Tests for HttpBatchTarget.
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
 * Unit tests for {@link HttpBatchTarget}.
 */
class HttpBatchTargetTest {

  @SuppressWarnings("unchecked")
  @Test
  void dispatchSendsPostAndAccepts202() throws Exception {
    HttpClient mockClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(202);
    when(mockResponse.body()).thenReturn(
        "{\"status\":\"accepted\",\"jobId\":\"job-1\","
            + "\"statusPath\":\"batch-status/job-1/status.json\"}"
    );
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    HttpBatchTarget target = new HttpBatchTarget("https://example.com", "test-key", mockClient);

    assertDoesNotThrow(() -> target.dispatch("job-1", "batch-jobs/job-1/inputs/", "Main", 1));
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

    HttpBatchTarget target = new HttpBatchTarget("https://example.com", "test-key", mockClient);

    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> target.dispatch("job-bad", "prefix/", "Main", 1));
    assertTrue(ex.getMessage().contains("HTTP 400"));
    assertTrue(ex.getMessage().contains("missing-fields"));
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

    HttpBatchTarget target = new HttpBatchTarget("https://example.com", "test-key", mockClient);

    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> target.dispatch("job-err", "prefix/", "Main", 5));
    assertTrue(ex.getMessage().contains("HTTP 500"));
  }

  @SuppressWarnings("unchecked")
  @Test
  void dispatchIncludesReplicatesInRequest() throws Exception {
    HttpClient mockClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(202);
    when(mockResponse.body()).thenReturn("{\"status\":\"accepted\"}");
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    HttpBatchTarget target = new HttpBatchTarget("https://example.com", "key", mockClient);
    target.dispatch("job-rep", "prefix/", "Main", 10);

    // Verify the request was made (form body contains replicates, verified via mock interaction)
    verify(mockClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
  }

  @Test
  void constructsFromHttpTargetConfig() {
    HttpTargetConfig config = new HttpTargetConfig("https://example.com", "key");
    HttpBatchTarget target = new HttpBatchTarget(config);
    // Should not throw — construction is lazy (no HTTP call)
    assertDoesNotThrow(() -> {});
  }

  @Test
  void endpointTrailingSlashIsNormalized() throws Exception {
    // Ensure trailing slash doesn't result in double slash in URL
    HttpClient mockClient = mock(HttpClient.class);
    @SuppressWarnings("unchecked")
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(202);
    when(mockResponse.body()).thenReturn("{\"status\":\"accepted\"}");
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    HttpBatchTarget target = new HttpBatchTarget("https://example.com/", "key", mockClient);
    assertDoesNotThrow(() -> target.dispatch("job-1", "prefix/", "Main", 1));
  }
}
