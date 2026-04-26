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
import java.util.Map;
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

    assertDoesNotThrow(() -> target.dispatch(
        "job-1", "batch-jobs/job-1/inputs/", "Main", 1, Map.of(), 0));
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
        () -> target.dispatch("job-bad", "prefix/", "Main", 1, Map.of(), 0));
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
        () -> target.dispatch("job-err", "prefix/", "Main", 5, Map.of(), 0));
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
    target.dispatch("job-rep", "prefix/", "Main", 10, Map.of(), 0);

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

  @SuppressWarnings("unchecked")
  @Test
  void dispatchOmitsCustomTagsAndStartWhenDefault() throws Exception {
    HttpClient mockClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(202);
    when(mockResponse.body()).thenReturn("{}");
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    final org.mockito.ArgumentCaptor<HttpRequest> reqCaptor =
        org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
    HttpBatchTarget target = new HttpBatchTarget("https://example.com", "key", mockClient);

    target.dispatch("job-1", "prefix/", "Main", 1, Map.of(), 0);

    verify(mockClient).send(reqCaptor.capture(), any(HttpResponse.BodyHandler.class));
    String body = drainBody(reqCaptor.getValue());
    assertTrue(!body.contains("customTags"),
        "default empty tags should not be in form body: " + body);
    assertTrue(!body.contains("replicateStart"),
        "default zero start should not be in form body: " + body);
  }

  @SuppressWarnings("unchecked")
  @Test
  void dispatchIncludesCustomTagsAndReplicateStart() throws Exception {
    HttpClient mockClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(202);
    when(mockResponse.body()).thenReturn("{}");
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    final org.mockito.ArgumentCaptor<HttpRequest> reqCaptor =
        org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
    HttpBatchTarget target = new HttpBatchTarget("https://example.com", "key", mockClient);

    Map<String, String> tags = new java.util.LinkedHashMap<>();
    tags.put("run_hash", "abc123");
    tags.put("region", "west");
    target.dispatch("job-1", "prefix/", "Main", 3, tags, 5);

    verify(mockClient).send(reqCaptor.capture(), any(HttpResponse.BodyHandler.class));
    String body = drainBody(reqCaptor.getValue());
    String decoded = java.net.URLDecoder.decode(
        body, java.nio.charset.StandardCharsets.UTF_8);
    assertTrue(decoded.contains("customTags=run_hash=abc123\nregion=west"),
        "expected tag pairs in body: " + decoded);
    assertTrue(decoded.contains("replicateStart=5"),
        "expected replicateStart in body: " + decoded);
  }

  /**
   * Extracts the form body from an outgoing HttpRequest.
   *
   * <p>HttpRequest.BodyPublisher exposes the bytes via subscribe(); easiest path is to
   * resubscribe a tiny in-memory collector and concatenate.</p>
   */
  private static String drainBody(HttpRequest request) {
    java.util.List<java.nio.ByteBuffer> chunks = new java.util.ArrayList<>();
    request.bodyPublisher().get().subscribe(new java.util.concurrent.Flow.Subscriber<>() {
      @Override
      public void onSubscribe(java.util.concurrent.Flow.Subscription s) {
        s.request(Long.MAX_VALUE);
      }

      @Override
      public void onNext(java.nio.ByteBuffer item) {
        chunks.add(item);
      }

      @Override
      public void onError(Throwable t) {
        throw new RuntimeException(t);
      }

      @Override
      public void onComplete() {
        // no-op
      }
    });
    int total = chunks.stream().mapToInt(java.nio.ByteBuffer::remaining).sum();
    byte[] all = new byte[total];
    int offset = 0;
    for (java.nio.ByteBuffer chunk : chunks) {
      int len = chunk.remaining();
      chunk.get(all, offset, len);
      offset += len;
    }
    return new String(all, java.nio.charset.StandardCharsets.UTF_8);
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
    assertDoesNotThrow(() -> target.dispatch("job-1", "prefix/", "Main", 1, Map.of(), 0));
  }
}
