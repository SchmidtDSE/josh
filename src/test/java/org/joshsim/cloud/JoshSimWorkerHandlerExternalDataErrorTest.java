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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Test class specifically for testing external data error handling
 * in JoshSimWorkerHandler when external data files are missing or cannot be loaded.
 */
class JoshSimWorkerHandlerExternalDataErrorTest {

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

  private JoshSimWorkerHandler handler;
  private ByteArrayOutputStream outputStream;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    handler = new JoshSimWorkerHandler(apiDataLayer, true, java.util.Optional.empty(), true);
    outputStream = new ByteArrayOutputStream();

    when(exchange.getRequestHeaders()).thenReturn(headerMap);
    when(exchange.getResponseHeaders()).thenReturn(headerMap);
    when(headerMap.get("X-API-Key")).thenReturn(headerValues);
    when(exchange.getOutputStream()).thenReturn(outputStream);
    
    // Mock form parser factory behavior
    FormParserFactory mockFactory = mock(FormParserFactory.class);
    when(mockFactory.createParser(any())).thenReturn(formDataParser);
  }

  @Test
  void whenExternalDataFileNotFound_shouldReturn500WithInformativeError() throws Exception {
    // Given
    FormData.FormValue codeValue = mock(FormData.FormValue.class);
    FormData.FormValue nameValue = mock(FormData.FormValue.class);
    FormData.FormValue externalDataValue = mock(FormData.FormValue.class);

    when(headerValues.getFirst()).thenReturn("valid-key");
    when(apiDataLayer.apiKeyIsValid(anyString())).thenReturn(true);
    when(exchange.getRequestMethod()).thenReturn(new HttpString("POST"));
    when(formData.contains("code")).thenReturn(true);
    when(formData.contains("name")).thenReturn(true);
    when(formData.contains("externalData")).thenReturn(true);
    when(formData.getFirst("code")).thenReturn(codeValue);
    when(formData.getFirst("name")).thenReturn(nameValue);
    when(formData.getFirst("externalData")).thenReturn(externalDataValue);
    when(formDataParser.parseBlocking()).thenReturn(formData);

    // Simulation code that references external data file that doesn't exist
    String simulationCode = """
        setup() {
            external_data = open_external_data("nonexistent_file.csv");
        }
        """;
    when(codeValue.getValue()).thenReturn(simulationCode);
    when(nameValue.getValue()).thenReturn("test simulation");
    
    // External data with reference to non-existent file
    String externalDataSerialization = "nonexistent_file.csv\tfile_content_placeholder";
    when(externalDataValue.getValue()).thenReturn(externalDataSerialization);

    // When
    handler.handleRequestTrusted(exchange);

    // Then
    verify(exchange).setStatusCode(500);
    
    // Verify that the error response contains informative message about missing external data
    String responseContent = outputStream.toString();
    assert responseContent.contains("External data file not found") 
        || responseContent.contains("Cannot find virtual file")
        || responseContent.contains("nonexistent_file.csv");
  }

  @Test
  void whenExternalDataFileMalformed_shouldReturn500WithSpecificError() throws Exception {
    // Given
    FormData.FormValue codeValue = mock(FormData.FormValue.class);
    FormData.FormValue nameValue = mock(FormData.FormValue.class);
    FormData.FormValue externalDataValue = mock(FormData.FormValue.class);

    when(headerValues.getFirst()).thenReturn("valid-key");
    when(apiDataLayer.apiKeyIsValid(anyString())).thenReturn(true);
    when(exchange.getRequestMethod()).thenReturn(new HttpString("POST"));
    when(formData.contains("code")).thenReturn(true);
    when(formData.contains("name")).thenReturn(true);
    when(formData.contains("externalData")).thenReturn(true);
    when(formData.getFirst("code")).thenReturn(codeValue);
    when(formData.getFirst("name")).thenReturn(nameValue);
    when(formData.getFirst("externalData")).thenReturn(externalDataValue);
    when(formDataParser.parseBlocking()).thenReturn(formData);

    // Simulation code that references external CSV data
    String simulationCode = """
        setup() {
            external_data = open_external_data("malformed_data.csv");
        }
        """;
    when(codeValue.getValue()).thenReturn(simulationCode);
    when(nameValue.getValue()).thenReturn("test simulation");
    
    // External data with malformed CSV content (missing required columns)
    String malformedCsvContent = "invalid_header1,invalid_header2\nvalue1,value2";
    String externalDataSerialization = "malformed_data.csv\t" + malformedCsvContent;
    when(externalDataValue.getValue()).thenReturn(externalDataSerialization);

    // When
    handler.handleRequestTrusted(exchange);

    // Then
    verify(exchange).setStatusCode(500);
    
    // Verify that the error response contains specific information about malformed data
    String responseContent = outputStream.toString();
    assert responseContent.contains("CSV must contain 'longitude' and 'latitude' columns")
        || responseContent.contains("malformed")
        || responseContent.contains("Invalid");
  }

  @Test
  void whenExternalDataHasInvalidNumericValues_shouldReturn500WithValueError() throws Exception {
    // Given
    FormData.FormValue codeValue = mock(FormData.FormValue.class);
    FormData.FormValue nameValue = mock(FormData.FormValue.class);
    FormData.FormValue externalDataValue = mock(FormData.FormValue.class);

    when(headerValues.getFirst()).thenReturn("valid-key");
    when(apiDataLayer.apiKeyIsValid(anyString())).thenReturn(true);
    when(exchange.getRequestMethod()).thenReturn(new HttpString("POST"));
    when(formData.contains("code")).thenReturn(true);
    when(formData.contains("name")).thenReturn(true);
    when(formData.contains("externalData")).thenReturn(true);
    when(formData.getFirst("code")).thenReturn(codeValue);
    when(formData.getFirst("name")).thenReturn(nameValue);
    when(formData.getFirst("externalData")).thenReturn(externalDataValue);
    when(formDataParser.parseBlocking()).thenReturn(formData);

    // Simulation code that references external CSV data
    String simulationCode = """
        setup() {
            external_data = open_external_data("invalid_numeric.csv");
        }
        """;
    when(codeValue.getValue()).thenReturn(simulationCode);
    when(nameValue.getValue()).thenReturn("test simulation");
    
    // External data with invalid numeric values
    String invalidCsvContent = "longitude,latitude,value\n\"not_a_number\",45.0,100\n-120.0,\"invalid\",200";
    String externalDataSerialization = "invalid_numeric.csv\t" + invalidCsvContent;
    when(externalDataValue.getValue()).thenReturn(externalDataSerialization);

    // When
    handler.handleRequestTrusted(exchange);

    // Then
    verify(exchange).setStatusCode(500);
    
    // Verify that the error response contains information about invalid numeric values
    String responseContent = outputStream.toString();
    assert responseContent.contains("Invalid numeric value")
        || responseContent.contains("not_a_number")
        || responseContent.contains("NumberFormatException");
  }

  @Test
  void whenMultipleExternalDataFilesWithOneMissing_shouldReturn500WithSpecificFileError() throws Exception {
    // Given
    FormData.FormValue codeValue = mock(FormData.FormValue.class);
    FormData.FormValue nameValue = mock(FormData.FormValue.class);
    FormData.FormValue externalDataValue = mock(FormData.FormValue.class);

    when(headerValues.getFirst()).thenReturn("valid-key");
    when(apiDataLayer.apiKeyIsValid(anyString())).thenReturn(true);
    when(exchange.getRequestMethod()).thenReturn(new HttpString("POST"));
    when(formData.contains("code")).thenReturn(true);
    when(formData.contains("name")).thenReturn(true);
    when(formData.contains("externalData")).thenReturn(true);
    when(formData.getFirst("code")).thenReturn(codeValue);
    when(formData.getFirst("name")).thenReturn(nameValue);
    when(formData.getFirst("externalData")).thenReturn(externalDataValue);
    when(formDataParser.parseBlocking()).thenReturn(formData);

    // Simulation code that references multiple external data files
    String simulationCode = """
        setup() {
            data1 = open_external_data("existing_file.csv");
            data2 = open_external_data("missing_file.csv");
        }
        """;
    when(codeValue.getValue()).thenReturn(simulationCode);
    when(nameValue.getValue()).thenReturn("test simulation");
    
    // External data with one valid file and one missing file reference
    String validCsvContent = "longitude,latitude,value\n-120.0,45.0,100";
    String externalDataSerialization = 
        "existing_file.csv\t" + validCsvContent + "\n" +
        "missing_file.csv\t"; // Missing content for second file
    when(externalDataValue.getValue()).thenReturn(externalDataSerialization);

    // When
    handler.handleRequestTrusted(exchange);

    // Then
    verify(exchange).setStatusCode(500);
    
    // Verify that the error response identifies the specific missing file
    String responseContent = outputStream.toString();
    assert responseContent.contains("missing_file.csv")
        || responseContent.contains("Cannot find virtual file");
  }
}