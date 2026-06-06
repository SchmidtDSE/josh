/**
 * Tests for the /preprocessBatch endpoint handler.
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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for JoshSimPreprocessBatchHandler.
 *
 * <p>Tests HTTP-layer validation (method, API key, required fields) via
 * {@code processFormData}. Mirrors the structure of
 * {@link JoshSimBatchHandlerTest}.</p>
 */
class JoshSimPreprocessBatchHandlerTest {

  @Mock
  private HttpServerExchange exchange;
  @Mock
  private CloudApiDataLayer apiDataLayer;
  @Mock
  private HeaderMap headerMap;
  @Mock
  private FormData formData;

  private JoshSimPreprocessBatchHandler handler;
  private ByteArrayOutputStream responseBody;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    handler = new JoshSimPreprocessBatchHandler(apiDataLayer);
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
  void whenMissingRequiredFields_shouldReturn400() {
    setupValidApiKey();
    when(formData.contains("jobId")).thenReturn(false);
    when(formData.contains("simulation")).thenReturn(false);
    when(formData.contains("dataFile")).thenReturn(false);
    when(formData.contains("variable")).thenReturn(false);
    when(formData.contains("units")).thenReturn(false);
    when(formData.contains("outputFile")).thenReturn(false);
    when(formData.contains("workDir")).thenReturn(false);

    handler.processFormData(exchange, formData);

    verify(exchange).setStatusCode(400);
    assertTrue(responseBody.toString().contains("missing-fields"));
  }

  @Test
  void whenMissingDataFile_shouldReturn400() {
    setupValidApiKey();
    when(formData.contains("jobId")).thenReturn(true);
    when(formData.contains("simulation")).thenReturn(true);
    when(formData.contains("dataFile")).thenReturn(false);
    when(formData.contains("variable")).thenReturn(true);
    when(formData.contains("units")).thenReturn(true);
    when(formData.contains("outputFile")).thenReturn(true);
    when(formData.contains("workDir")).thenReturn(true);

    handler.processFormData(exchange, formData);

    verify(exchange).setStatusCode(400);
  }

  @Test
  void whenWorkDirDoesNotExist_shouldReturn400() {
    setupValidApiKey();
    setupRequiredFields("test-pp-123", "TestSim", "/nonexistent/path",
        "data.nc", "temp", "celsius", "output.jshd");

    handler.processFormData(exchange, formData);

    verify(exchange).setStatusCode(400);
    assertTrue(responseBody.toString().contains("does not exist"));
  }

  @Test
  void whenStageFromMinioTrueButMissingPrefix_shouldReturn400() {
    setupValidApiKey();
    setupRequiredFields("job-stage", "TestSim", "/tmp/some-dir",
        "data.nc", "temp", "celsius", "output.jshd");

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
  void processFormDataReturnsApiKeyOnValidation() {
    setupValidApiKey();
    setupRequiredFields("job-1", "TestSim", "/nonexistent",
        "data.nc", "temp", "celsius", "output.jshd");

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

  @TempDir
  File tempDir;

  @Test
  void whenValidWorkDir_shouldReturn202() throws IOException {
    setupValidApiKey();

    File joshScript = new File(tempDir, "test.josh");
    Files.writeString(joshScript.toPath(), "start simulation Test end simulation");

    setupRequiredFields("pp-async-1", "Test", tempDir.getAbsolutePath(),
        "data.nc", "temperature", "celsius", "output.jshd");
    when(formData.contains("stageFromMinio")).thenReturn(false);

    handler.processFormData(exchange, formData);

    verify(exchange).setStatusCode(202);
    String body = responseBody.toString();
    assertTrue(body.contains("\"status\":\"accepted\""));
    assertTrue(body.contains("\"jobId\":\"pp-async-1\""));
    assertTrue(body.contains("\"statusPath\":\"batch-status/pp-async-1/status.json\""));
  }

  // --- Helpers ---

  private void setupValidApiKey() {
    FormData.FormValue apiKeyValue = mock(FormData.FormValue.class);
    when(formData.contains("apiKey")).thenReturn(true);
    when(formData.getFirst("apiKey")).thenReturn(apiKeyValue);
    when(apiKeyValue.getValue()).thenReturn("valid-key");
    when(apiDataLayer.apiKeyIsValid("valid-key")).thenReturn(true);
  }

  private void setupRequiredFields(String jobId, String simulation, String workDir,
      String dataFile, String variable, String units, String outputFile) {
    when(formData.contains("jobId")).thenReturn(true);
    when(formData.contains("simulation")).thenReturn(true);
    when(formData.contains("workDir")).thenReturn(true);
    when(formData.contains("dataFile")).thenReturn(true);
    when(formData.contains("variable")).thenReturn(true);
    when(formData.contains("units")).thenReturn(true);
    when(formData.contains("outputFile")).thenReturn(true);

    FormData.FormValue jobIdVal = mock(FormData.FormValue.class);
    FormData.FormValue simVal = mock(FormData.FormValue.class);
    FormData.FormValue workDirVal = mock(FormData.FormValue.class);
    FormData.FormValue dataFileVal = mock(FormData.FormValue.class);
    FormData.FormValue variableVal = mock(FormData.FormValue.class);
    FormData.FormValue unitsVal = mock(FormData.FormValue.class);
    FormData.FormValue outputFileVal = mock(FormData.FormValue.class);

    when(formData.getFirst("jobId")).thenReturn(jobIdVal);
    when(formData.getFirst("simulation")).thenReturn(simVal);
    when(formData.getFirst("workDir")).thenReturn(workDirVal);
    when(formData.getFirst("dataFile")).thenReturn(dataFileVal);
    when(formData.getFirst("variable")).thenReturn(variableVal);
    when(formData.getFirst("units")).thenReturn(unitsVal);
    when(formData.getFirst("outputFile")).thenReturn(outputFileVal);

    when(jobIdVal.getValue()).thenReturn(jobId);
    when(simVal.getValue()).thenReturn(simulation);
    when(workDirVal.getValue()).thenReturn(workDir);
    when(dataFileVal.getValue()).thenReturn(dataFile);
    when(variableVal.getValue()).thenReturn(variable);
    when(unitsVal.getValue()).thenReturn(units);
    when(outputFileVal.getValue()).thenReturn(outputFile);
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
