
package org.joshsim.cloud;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;
import java.io.OutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


class JoshSimLeaderHandlerTest {

  @Mock
  private HttpServerExchange exchange;
  @Mock
  private CloudApiDataLayer apiDataLayer;
  @Mock
  private HeaderMap headerMap;
  @Mock
  private FormDataParser formDataParser;
  @Mock
  private FormData formData;
  @Mock
  private OutputStream outputStream;

  private JoshSimLeaderHandler handler;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    handler = new JoshSimLeaderHandler(apiDataLayer, "http://localhost:8080", 4);

    when(exchange.getRequestHeaders()).thenReturn(headerMap);
    when(exchange.getResponseHeaders()).thenReturn(headerMap);
    when(exchange.getOutputStream()).thenReturn(outputStream);
  }

  @Test
  void whenApiKeyIsInvalid_shouldReturn401() throws Exception {
    FormData.FormValue apiKeyValue = mock(FormData.FormValue.class);
    when(exchange.getRequestMethod()).thenReturn(new HttpString("POST"));
    when(formData.contains("apiKey")).thenReturn(true);
    when(formData.getFirst("apiKey")).thenReturn(apiKeyValue);
    when(apiKeyValue.getValue()).thenReturn("invalid-key");
    when(apiDataLayer.apiKeyIsValid(anyString())).thenReturn(false);
    when(formDataParser.parseBlocking()).thenReturn(formData);

    handler.handleRequest(exchange);

    verify(exchange).setStatusCode(401);
  }

  @Test
  void whenMethodIsNotPost_shouldReturn405() throws Exception {
    when(exchange.getRequestMethod()).thenReturn(new HttpString("GET"));

    handler.handleRequestTrusted(exchange, "");

    verify(exchange).setStatusCode(405);
  }

  @Test
  void whenFormParserIsNull_shouldReturn400() throws Exception {
    when(exchange.getRequestMethod()).thenReturn(new HttpString("POST"));
    FormParserFactory mockFactory = mock(FormParserFactory.class);
    when(mockFactory.createParser(any())).thenReturn(null);

    handler.handleRequestTrusted(exchange, "");

    verify(exchange).setStatusCode(400);
  }

  @Test
  void whenFormDataIsMissingRequiredFields_shouldReturn400() throws Exception {
    when(exchange.getRequestMethod()).thenReturn(new HttpString("POST"));
    when(formData.contains("code")).thenReturn(false);
    when(formData.contains("name")).thenReturn(false);
    when(formData.contains("replicates")).thenReturn(false);
    when(formDataParser.parseBlocking()).thenReturn(formData);

    handler.handleRequestTrusted(exchange, "");

    verify(exchange).setStatusCode(400);
  }

  @Test
  void whenValidRequest_shouldProcessSimulation() throws Exception {
    FormData.FormValue codeValue = mock(FormData.FormValue.class);
    FormData.FormValue nameValue = mock(FormData.FormValue.class);
    FormData.FormValue replicatesValue = mock(FormData.FormValue.class);
    FormData.FormValue apiKeyValue = mock(FormData.FormValue.class);

    when(exchange.getRequestMethod()).thenReturn(new HttpString("POST"));
    when(formData.contains("code")).thenReturn(true);
    when(formData.contains("name")).thenReturn(true);
    when(formData.contains("replicates")).thenReturn(true);
    when(formData.contains("apiKey")).thenReturn(true);
    when(formData.getFirst("code")).thenReturn(codeValue);
    when(formData.getFirst("name")).thenReturn(nameValue);
    when(formData.getFirst("replicates")).thenReturn(replicatesValue);
    when(formData.getFirst("apiKey")).thenReturn(apiKeyValue);
    when(codeValue.getValue()).thenReturn("simulation code");
    when(nameValue.getValue()).thenReturn("test simulation");
    when(replicatesValue.getValue()).thenReturn("2");
    when(apiKeyValue.getValue()).thenReturn("valid-key");
    when(apiDataLayer.apiKeyIsValid(anyString())).thenReturn(true);
    when(formDataParser.parseBlocking()).thenReturn(formData);

    handler.handleRequestTrusted(exchange, "");

    verify(exchange).setStatusCode(200);
    verify(exchange).getResponseHeaders();
  }
}
