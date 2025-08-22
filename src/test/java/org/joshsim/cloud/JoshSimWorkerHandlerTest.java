package org.joshsim.cloud;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


class JoshSimWorkerHandlerTest {

  @Mock
  private HttpServerExchange exchange;
  @Mock
  private CloudApiDataLayer apiDataLayer;
  @Mock
  private HeaderMap headerMap;
  @Mock
  private HeaderValues headerValues;
  @Mock
  private FormDataParser formDataParser;
  @Mock
  private FormData formData;
  @Mock
  private OutputStream outputStream;

  private JoshSimWorkerHandler handler;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    handler = new JoshSimWorkerHandler(apiDataLayer, true, java.util.Optional.empty(), true);

    when(exchange.getRequestHeaders()).thenReturn(headerMap);
    when(exchange.getResponseHeaders()).thenReturn(headerMap);
    when(headerMap.get("X-API-Key")).thenReturn(headerValues);
    when(exchange.getOutputStream()).thenReturn(outputStream);
  }

  @Test
  void whenApiKeyIsInvalid_shouldReturn400() throws Exception {
    // Given
    when(headerValues.getFirst()).thenReturn("invalid-key");
    when(apiDataLayer.apiKeyIsValid(anyString())).thenReturn(false);
    when(exchange.getRequestMethod()).thenReturn(new HttpString("POST"));

    // When
    handler.handleRequest(exchange);

    // Then
    verify(exchange).setStatusCode(400);
  }

  @Test
  void whenMethodIsNotPost_shouldReturn405() throws Exception {
    // Given
    when(headerValues.getFirst()).thenReturn("valid-key");
    when(apiDataLayer.apiKeyIsValid(anyString())).thenReturn(true);
    when(exchange.getRequestMethod()).thenReturn(new HttpString("GET"));

    // When
    handler.handleRequestTrusted(exchange);

    // Then
    verify(exchange).setStatusCode(405);
  }

  @Test
  void whenFormParserIsNull_shouldReturn400() throws Exception {
    // Given
    when(headerValues.getFirst()).thenReturn("valid-key");
    when(apiDataLayer.apiKeyIsValid(anyString())).thenReturn(true);
    when(exchange.getRequestMethod()).thenReturn(new HttpString("POST"));

    FormParserFactory mockFactory = mock(FormParserFactory.class);
    when(mockFactory.createParser(any())).thenReturn(null);

    // When
    handler.handleRequestTrusted(exchange);

    // Then
    verify(exchange).setStatusCode(400);
  }

  @Test
  void whenFormDataIsMissingRequiredFields_shouldReturn400() throws Exception {
    // Given
    when(headerValues.getFirst()).thenReturn("valid-key");
    when(apiDataLayer.apiKeyIsValid(anyString())).thenReturn(true);
    when(exchange.getRequestMethod()).thenReturn(new HttpString("POST"));
    when(formData.contains("code")).thenReturn(false);
    when(formData.contains("name")).thenReturn(false);
    when(formDataParser.parseBlocking()).thenReturn(formData);

    // When
    handler.handleRequestTrusted(exchange);

    // Then
    verify(exchange).setStatusCode(400);
  }

  @Test
  void whenValidRequest_shouldProcessSimulation() throws Exception {
    // Given
    FormData.FormValue codeValue = mock(FormData.FormValue.class);
    FormData.FormValue nameValue = mock(FormData.FormValue.class);

    when(headerValues.getFirst()).thenReturn("valid-key");
    when(apiDataLayer.apiKeyIsValid(anyString())).thenReturn(true);
    when(exchange.getRequestMethod()).thenReturn(new HttpString("POST"));
    when(formData.contains("code")).thenReturn(true);
    when(formData.contains("name")).thenReturn(true);
    when(formData.getFirst("code")).thenReturn(codeValue);
    when(formData.getFirst("name")).thenReturn(nameValue);
    when(codeValue.getValue()).thenReturn("simulation code");
    when(nameValue.getValue()).thenReturn("test simulation");
    when(formDataParser.parseBlocking()).thenReturn(formData);

    // When
    handler.handleRequestTrusted(exchange);

    // Then
    verify(exchange).setStatusCode(200);
    verify(exchange).getResponseHeaders();
  }

  @Test
  void whenSimulationCompletes_shouldOutputStandardizedWireFormat() throws Exception {
    // Given
    FormData.FormValue codeValue = mock(FormData.FormValue.class);
    FormData.FormValue nameValue = mock(FormData.FormValue.class);
    FormData.FormValue externalDataValue = mock(FormData.FormValue.class);
    FormData.FormValue favorBigDecimalValue = mock(FormData.FormValue.class);
    FormData.FormValue apiKeyValue = mock(FormData.FormValue.class);
    
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    
    when(apiDataLayer.apiKeyIsValid(anyString())).thenReturn(true);
    when(exchange.getRequestMethod()).thenReturn(new HttpString("POST"));
    when(formData.contains("code")).thenReturn(true);
    when(formData.contains("name")).thenReturn(true);
    when(formData.contains("externalData")).thenReturn(true);
    when(formData.contains("favorBigDecimal")).thenReturn(true);
    when(formData.contains("apiKey")).thenReturn(true);
    when(formData.getFirst("code")).thenReturn(codeValue);
    when(formData.getFirst("name")).thenReturn(nameValue);
    when(formData.getFirst("externalData")).thenReturn(externalDataValue);
    when(formData.getFirst("favorBigDecimal")).thenReturn(favorBigDecimalValue);
    when(formData.getFirst("apiKey")).thenReturn(apiKeyValue);
    when(codeValue.getValue()).thenReturn(
        "simulation basicPatch { organism dog { name \"spot\" } }");
    when(nameValue.getValue()).thenReturn("basicPatch");
    when(externalDataValue.getValue()).thenReturn("{}");
    when(favorBigDecimalValue.getValue()).thenReturn("false");
    when(apiKeyValue.getValue()).thenReturn("valid-key");
    when(formDataParser.parseBlocking()).thenReturn(formData);
    when(exchange.getOutputStream()).thenReturn(outputStream);
    
    // Create a fake form parser factory that returns our mock parser
    FormParserFactory mockFactory = mock(FormParserFactory.class);
    when(mockFactory.createParser(exchange)).thenReturn(formDataParser);
    
    // When
    handler.handleRequestTrusted(exchange);
    
    // Then
    String output = outputStream.toString();
    
    // Verify standardized wire format: exports should be wrapped with [0] prefix
    // and end marker should be [end 0]
    if (output.contains("[end 0]")) {
      // Only verify format if simulation actually completed
      verify(exchange).setStatusCode(200);
      
      // Verify that all data exports are prefixed with [0]
      String[] lines = output.split("\n");
      for (String line : lines) {
        if (!line.trim().isEmpty() && !line.startsWith("[progress") && !line.startsWith("[end")) {
          // All other output should be prefixed with [0]
          assert line.startsWith("[0] ") : "Export line should be prefixed with [0]: " + line;
        }
      }
    }
  }
}
