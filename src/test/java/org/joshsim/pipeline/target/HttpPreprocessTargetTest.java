/**
 * Tests for HttpPreprocessTarget.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import org.joshsim.command.PreprocessOptions;
import org.joshsim.command.TimeAxisParams;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;


/**
 * Unit tests for {@link HttpPreprocessTarget}.
 */
class HttpPreprocessTargetTest {

  private static final PreprocessParams TEST_PARAMS = new PreprocessParams(
      "data.nc", "temperature", "celsius", "output.jshd",
      PreprocessOptions.defaults());

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
        PreprocessOptions.builder()
            .horizCoordName("longitude")
            .vertCoordName("latitude")
            .timeName("time")
            .timestep("2020")
            .defaultValue("-999")
            .parallel(true)
            .build());

    HttpPreprocessTarget target = new HttpPreprocessTarget(
        "https://example.com", "key", mockClient
    );
    target.dispatch("job-opts", "prefix/", "Main", paramsWithOpts);

    Map<String, String> fields = captureFormFields(mockClient);
    assertEquals("longitude", fields.get("xCoord"));
    assertEquals("latitude", fields.get("yCoord"));
    assertEquals("time", fields.get("timeDim"));
    assertEquals("2020", fields.get("timestep"));
    assertEquals("-999", fields.get("defaultValue"));
    assertEquals("true", fields.get("parallel"));
    assertFalse(fields.containsKey("amend"));
  }

  @SuppressWarnings("unchecked")
  @Test
  void dispatchSendsDeclaredTimeAxisAsFormFields() throws Exception {
    HttpClient mockClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(202);
    when(mockResponse.body()).thenReturn("{\"status\":\"accepted\"}");
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    PreprocessParams params = new PreprocessParams(
        "temp.nc", "temperature", "celsius", "temp.jshd",
        PreprocessOptions.builder()
            .timeName("time")
            .timeAxis(TimeAxisParams.of("count", "2015", "year", "86", "1", "", ""))
            .build());

    HttpPreprocessTarget target = new HttpPreprocessTarget(
        "https://example.com", "key", mockClient
    );
    target.dispatch("job-time", "prefix/", "Main", params);

    Map<String, String> fields = captureFormFields(mockClient);
    assertEquals("count", fields.get("timeType"));
    assertEquals("2015", fields.get("timeStart"));
    assertEquals("year", fields.get("timeUnit"));
    assertEquals("86", fields.get("timeCount"));
    assertEquals("1", fields.get("timeIncrement"));
    // Blank values must stay off the wire; the handler reads absent and declared the same way.
    assertFalse(fields.containsKey("timeInterval"));
    assertFalse(fields.containsKey("timeInstant"));
  }

  @SuppressWarnings("unchecked")
  @Test
  void dispatchOmitsTimeAxisFieldsWhenNoneDeclared() throws Exception {
    HttpClient mockClient = mock(HttpClient.class);
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(202);
    when(mockResponse.body()).thenReturn("{\"status\":\"accepted\"}");
    when(mockClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    HttpPreprocessTarget target = new HttpPreprocessTarget(
        "https://example.com", "key", mockClient
    );
    target.dispatch("job-no-time", "prefix/", "Main", TEST_PARAMS);

    Map<String, String> fields = captureFormFields(mockClient);
    for (TimeAxisParams.Field field : TimeAxisParams.Field.values()) {
      assertFalse(fields.containsKey(field.getFieldName()), field.getFieldName());
    }
  }

  /**
   * Captures the form-encoded body of the single request sent to the client and decodes it.
   *
   * @param mockClient The mock client the target dispatched through.
   * @return Form field name to decoded value.
   */
  @SuppressWarnings("unchecked")
  private static Map<String, String> captureFormFields(HttpClient mockClient) throws Exception {
    ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(mockClient).send(captor.capture(), any(HttpResponse.BodyHandler.class));

    HttpRequest.BodyPublisher publisher = captor.getValue().bodyPublisher().orElseThrow();
    StringBuilder collected = new StringBuilder();
    CountDownLatch done = new CountDownLatch(1);
    publisher.subscribe(new Flow.Subscriber<ByteBuffer>() {
      @Override
      public void onSubscribe(Flow.Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
      }

      @Override
      public void onNext(ByteBuffer item) {
        collected.append(StandardCharsets.UTF_8.decode(item));
      }

      @Override
      public void onError(Throwable throwable) {
        done.countDown();
      }

      @Override
      public void onComplete() {
        done.countDown();
      }
    });
    assertTrue(done.await(5, TimeUnit.SECONDS), "body publisher did not complete");

    Map<String, String> fields = new LinkedHashMap<>();
    for (String pair : collected.toString().split("&")) {
      int split = pair.indexOf('=');
      fields.put(
          URLDecoder.decode(pair.substring(0, split), StandardCharsets.UTF_8),
          URLDecoder.decode(pair.substring(split + 1), StandardCharsets.UTF_8));
    }
    return fields;
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
