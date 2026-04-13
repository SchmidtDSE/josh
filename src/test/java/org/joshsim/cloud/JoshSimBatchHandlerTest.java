/**
 * Tests for the /runBatch endpoint handler.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for JoshSimBatchHandler.
 *
 * <p>Tests HTTP-layer validation (method, API key, required fields) via
 * {@code processFormData} for tests that need form data, and via
 * {@code handleRequest} for HTTP-level tests.</p>
 */
class JoshSimBatchHandlerTest {

  @Mock
  private HttpServerExchange exchange;
  @Mock
  private CloudApiDataLayer apiDataLayer;
  @Mock
  private HeaderMap headerMap;
  @Mock
  private FormData formData;

  private JoshSimBatchHandler handler;
  private ByteArrayOutputStream responseBody;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    handler = new JoshSimBatchHandler(apiDataLayer);
    responseBody = new ByteArrayOutputStream();

    when(exchange.getRequestHeaders()).thenReturn(headerMap);
    when(exchange.getResponseHeaders()).thenReturn(headerMap);
    when(exchange.getResponseSender()).thenReturn(new StubSender(responseBody));
  }

  @Test
  void whenMethodIsGet_shouldReturn405() throws Exception {
    when(exchange.getRequestMethod()).thenReturn(new HttpString("GET"));

    handler.handleRequest(exchange);

    verify(exchange).setStatusCode(405);
  }

  @Test
  void whenFormParserIsNull_shouldReturn400() throws Exception {
    when(exchange.getRequestMethod()).thenReturn(new HttpString("POST"));

    handler.handleRequest(exchange);

    verify(exchange).setStatusCode(400);
    assertTrue(responseBody.toString().contains("missing-form"));
  }

  @Test
  void whenApiKeyIsInvalid_shouldReturn401() {
    FormData.FormValue apiKeyValue = mock(FormData.FormValue.class);
    when(formData.contains("apiKey")).thenReturn(true);
    when(formData.getFirst("apiKey")).thenReturn(apiKeyValue);
    when(apiKeyValue.getValue()).thenReturn("bad-key");
    when(apiDataLayer.apiKeyIsValid("bad-key")).thenReturn(false);

    handler.processFormData(exchange, formData);

    verify(exchange).setStatusCode(401);
  }

  @Test
  void whenNoApiKeyProvided_shouldReturn401() {
    when(formData.contains("apiKey")).thenReturn(false);
    when(apiDataLayer.apiKeyIsValid("")).thenReturn(false);

    handler.processFormData(exchange, formData);

    verify(exchange).setStatusCode(401);
  }

  @Test
  void whenMissingAllRequiredFields_shouldReturn400() {
    setupValidApiKey();
    when(formData.contains("jobId")).thenReturn(false);
    when(formData.contains("simulation")).thenReturn(false);
    when(formData.contains("workDir")).thenReturn(false);

    handler.processFormData(exchange, formData);

    verify(exchange).setStatusCode(400);
    assertTrue(responseBody.toString().contains("missing-fields"));
  }

  @Test
  void whenMissingJobId_shouldReturn400() {
    setupValidApiKey();
    when(formData.contains("jobId")).thenReturn(false);
    when(formData.contains("simulation")).thenReturn(true);
    when(formData.contains("workDir")).thenReturn(true);

    handler.processFormData(exchange, formData);

    verify(exchange).setStatusCode(400);
  }

  @Test
  void whenMissingSimulation_shouldReturn400() {
    setupValidApiKey();
    when(formData.contains("jobId")).thenReturn(true);
    when(formData.contains("simulation")).thenReturn(false);
    when(formData.contains("workDir")).thenReturn(true);

    handler.processFormData(exchange, formData);

    verify(exchange).setStatusCode(400);
  }

  @Test
  void whenMissingWorkDir_shouldReturn400() {
    setupValidApiKey();
    when(formData.contains("jobId")).thenReturn(true);
    when(formData.contains("simulation")).thenReturn(true);
    when(formData.contains("workDir")).thenReturn(false);

    handler.processFormData(exchange, formData);

    verify(exchange).setStatusCode(400);
  }

  @Test
  void whenWorkDirDoesNotExist_shouldReturn400() {
    setupValidApiKey();
    setupRequiredFields("test-job-123", "TestSim", "/nonexistent/path/abc123");

    handler.processFormData(exchange, formData);

    verify(exchange).setStatusCode(400);
    String body = responseBody.toString();
    assertTrue(body.contains("test-job-123"), "Response should include jobId");
    assertTrue(body.contains("does not exist"), "Response should explain the error");
  }

  @Test
  void whenStageFromMinioTrueButMissingPrefix_shouldReturn400() {
    setupValidApiKey();
    setupRequiredFields("job-stage-1", "TestSim", "/tmp/some-dir");

    FormData.FormValue stageFlag = mock(FormData.FormValue.class);
    when(formData.contains("stageFromMinio")).thenReturn(true);
    when(formData.getFirst("stageFromMinio")).thenReturn(stageFlag);
    when(stageFlag.getValue()).thenReturn("true");
    when(formData.contains("minioPrefix")).thenReturn(false);

    handler.processFormData(exchange, formData);

    verify(exchange).setStatusCode(400);
    assertTrue(responseBody.toString().contains("minioPrefix is required"));
  }

  @Test
  void whenStageFromMinioFalse_shouldNotRequirePrefix() {
    setupValidApiKey();
    setupRequiredFields("job-no-stage", "TestSim", "/nonexistent");

    FormData.FormValue stageFlag = mock(FormData.FormValue.class);
    when(formData.contains("stageFromMinio")).thenReturn(true);
    when(formData.getFirst("stageFromMinio")).thenReturn(stageFlag);
    when(stageFlag.getValue()).thenReturn("false");
    when(formData.contains("minioPrefix")).thenReturn(false);

    handler.processFormData(exchange, formData);

    // Should hit workDir validation (400), not minioPrefix validation
    verify(exchange).setStatusCode(400);
    assertTrue(responseBody.toString().contains("does not exist"));
  }

  @Test
  void whenStageFromMinioAbsent_shouldNotRequirePrefix() {
    setupValidApiKey();
    setupRequiredFields("job-default", "TestSim", "/nonexistent");
    when(formData.contains("stageFromMinio")).thenReturn(false);

    handler.processFormData(exchange, formData);

    // Should hit workDir validation (400), not minioPrefix validation
    verify(exchange).setStatusCode(400);
    assertTrue(responseBody.toString().contains("does not exist"));
  }

  @Test
  void processFormDataReturnsApiKeyOnSuccess() {
    setupValidApiKey();
    setupRequiredFields("job-1", "TestSim", "/nonexistent");

    Optional<String> result = handler.processFormData(exchange, formData);

    assertTrue(result.isPresent(), "Should return API key");
    assertTrue(result.get().equals("valid-key"));
  }

  @Test
  void processFormDataReturnsEmptyOnBadApiKey() {
    FormData.FormValue apiKeyValue = mock(FormData.FormValue.class);
    when(formData.contains("apiKey")).thenReturn(true);
    when(formData.getFirst("apiKey")).thenReturn(apiKeyValue);
    when(apiKeyValue.getValue()).thenReturn("bad");
    when(apiDataLayer.apiKeyIsValid("bad")).thenReturn(false);

    Optional<String> result = handler.processFormData(exchange, formData);

    assertTrue(result.isEmpty(), "Should return empty for bad API key");
  }

  @Test
  void handleRequestLogsRuntime() throws Exception {
    when(exchange.getRequestMethod()).thenReturn(new HttpString("POST"));

    handler.handleRequest(exchange);

    verify(apiDataLayer).log(anyString(), anyString(), any(long.class));
  }

  // --- Helpers ---

  private void setupValidApiKey() {
    FormData.FormValue apiKeyValue = mock(FormData.FormValue.class);
    when(formData.contains("apiKey")).thenReturn(true);
    when(formData.getFirst("apiKey")).thenReturn(apiKeyValue);
    when(apiKeyValue.getValue()).thenReturn("valid-key");
    when(apiDataLayer.apiKeyIsValid("valid-key")).thenReturn(true);
  }

  private void setupRequiredFields(String jobId, String simulation, String workDir) {
    when(formData.contains("jobId")).thenReturn(true);
    when(formData.contains("simulation")).thenReturn(true);
    when(formData.contains("workDir")).thenReturn(true);

    FormData.FormValue jobIdVal = mock(FormData.FormValue.class);
    FormData.FormValue simVal = mock(FormData.FormValue.class);
    FormData.FormValue workDirVal = mock(FormData.FormValue.class);

    when(formData.getFirst("jobId")).thenReturn(jobIdVal);
    when(formData.getFirst("simulation")).thenReturn(simVal);
    when(formData.getFirst("workDir")).thenReturn(workDirVal);

    when(jobIdVal.getValue()).thenReturn(jobId);
    when(simVal.getValue()).thenReturn(simulation);
    when(workDirVal.getValue()).thenReturn(workDir);
  }

  /**
   * Minimal Sender stub that captures the response body.
   */
  private static class StubSender implements io.undertow.io.Sender {
    private final ByteArrayOutputStream out;

    StubSender(ByteArrayOutputStream out) {
      this.out = out;
    }

    @Override
    public void send(String data, io.undertow.io.IoCallback callback) {
      out.writeBytes(data.getBytes());
    }

    @Override
    public void send(String data) {
      out.writeBytes(data.getBytes());
    }

    @Override
    public void send(String data, java.nio.charset.Charset charset,
        io.undertow.io.IoCallback callback) {
      out.writeBytes(data.getBytes(charset));
    }

    @Override
    public void send(String data, java.nio.charset.Charset charset) {
      out.writeBytes(data.getBytes(charset));
    }

    @Override
    public void send(java.nio.ByteBuffer buffer, io.undertow.io.IoCallback callback) {
      byte[] bytes = new byte[buffer.remaining()];
      buffer.get(bytes);
      out.writeBytes(bytes);
    }

    @Override
    public void send(java.nio.ByteBuffer buffer) {
      byte[] bytes = new byte[buffer.remaining()];
      buffer.get(bytes);
      out.writeBytes(bytes);
    }

    @Override
    public void send(java.nio.ByteBuffer[] buffers, io.undertow.io.IoCallback callback) {
      for (java.nio.ByteBuffer buf : buffers) {
        send(buf);
      }
    }

    @Override
    public void send(java.nio.ByteBuffer[] buffers) {
      for (java.nio.ByteBuffer buf : buffers) {
        send(buf);
      }
    }

    @Override
    public void transferFrom(java.nio.channels.FileChannel channel,
        io.undertow.io.IoCallback callback) {
      // stub
    }

    @Override
    public void close(io.undertow.io.IoCallback callback) {
      // stub
    }

    @Override
    public void close() {
      // stub
    }
  }
}
