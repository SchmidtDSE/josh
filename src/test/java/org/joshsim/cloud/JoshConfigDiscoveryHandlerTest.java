/**
 * Unit tests for JoshConfigDiscoveryHandler.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for JoshConfigDiscoveryHandler.
 */
public class JoshConfigDiscoveryHandlerTest {

  @Mock
  private CloudApiDataLayer mockDataLayer;
  
  @Mock
  private HttpServerExchange mockExchange;

  @Mock
  private FormDataParser mockFormParser;

  @Mock
  private FormData mockFormData;

  @Mock
  private FormData.FormValue mockFormValue;

  @Mock
  private Sender mockSender;

  private JoshConfigDiscoveryHandler handler;

  /**
   * Set up test fixtures before each test.
   */
  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    handler = new JoshConfigDiscoveryHandler(mockDataLayer);
  }

  @Test
  public void testHandlerConstructor() {
    // Test that handler can be constructed successfully
    JoshConfigDiscoveryHandler testHandler = new JoshConfigDiscoveryHandler(mockDataLayer);
    
    // Verify handler is created
    assertNotNull(testHandler);
  }

  @Test
  public void testGetMethodReturns405() {
    // Setup GET request
    HttpString getMethod = new HttpString("GET");
    when(mockExchange.getRequestMethod()).thenReturn(getMethod);
    
    // Execute
    Optional<String> result = handler.handleRequestTrusted(mockExchange);
    
    // Verify method not allowed
    assertFalse(result.isPresent());
    verify(mockExchange).setStatusCode(405);
  }

  @Test
  public void testPutMethodReturns405() {
    // Setup PUT request  
    HttpString putMethod = new HttpString("PUT");
    when(mockExchange.getRequestMethod()).thenReturn(putMethod);
    
    // Execute
    Optional<String> result = handler.handleRequestTrusted(mockExchange);
    
    // Verify method not allowed
    assertFalse(result.isPresent());
    verify(mockExchange).setStatusCode(405);
  }

  @Test
  public void testDeleteMethodReturns405() {
    // Setup DELETE request
    HttpString deleteMethod = new HttpString("DELETE");
    when(mockExchange.getRequestMethod()).thenReturn(deleteMethod);
    
    // Execute
    Optional<String> result = handler.handleRequestTrusted(mockExchange);
    
    // Verify method not allowed  
    assertFalse(result.isPresent());
    verify(mockExchange).setStatusCode(405);
  }

  @Test
  public void testPatchMethodReturns405() {
    // Setup PATCH request
    HttpString patchMethod = new HttpString("PATCH");
    when(mockExchange.getRequestMethod()).thenReturn(patchMethod);
    
    // Execute
    Optional<String> result = handler.handleRequestTrusted(mockExchange);
    
    // Verify method not allowed  
    assertFalse(result.isPresent());
    verify(mockExchange).setStatusCode(405);
  }

  @Test
  public void testOptionsMethodReturns405() {
    // Setup OPTIONS request
    HttpString optionsMethod = new HttpString("OPTIONS");
    when(mockExchange.getRequestMethod()).thenReturn(optionsMethod);
    
    // Execute
    Optional<String> result = handler.handleRequestTrusted(mockExchange);
    
    // Verify method not allowed  
    assertFalse(result.isPresent());
    verify(mockExchange).setStatusCode(405);
  }

  @Test
  public void testNullFormParserReturns400() {
    // Setup POST request with null form parser
    when(mockExchange.getRequestMethod()).thenReturn(new HttpString("POST"));
    when(mockExchange.getResponseHeaders()).thenReturn(mock(HeaderMap.class));
    
    // Mock FormParserFactory to return null parser
    try (MockedStatic<FormParserFactory> mockStatic = mockStatic(FormParserFactory.class)) {
      FormParserFactory mockFactory = mock(FormParserFactory.class);
      FormParserFactory.Builder mockBuilder = mock(FormParserFactory.Builder.class);
      when(FormParserFactory.builder()).thenReturn(mockBuilder);
      when(mockBuilder.build()).thenReturn(mockFactory);
      when(mockFactory.createParser(any())).thenReturn(null);
      
      // Execute
      Optional<String> result = handler.handleRequestTrusted(mockExchange);
      
      // Verify bad request
      assertFalse(result.isPresent());
      verify(mockExchange).setStatusCode(400);
    }
  }

  @Test
  public void testFormParsingIoExceptionThrowsRuntimeException() {
    // Setup POST request
    when(mockExchange.getRequestMethod()).thenReturn(new HttpString("POST"));
    when(mockExchange.getResponseHeaders()).thenReturn(mock(HeaderMap.class));
    when(mockExchange.getResponseSender()).thenReturn(mockSender);
    
    // Mock FormParserFactory
    try (MockedStatic<FormParserFactory> mockStatic = mockStatic(FormParserFactory.class)) {
      FormParserFactory mockFactory = mock(FormParserFactory.class);
      FormParserFactory.Builder mockBuilder = mock(FormParserFactory.Builder.class);
      when(FormParserFactory.builder()).thenReturn(mockBuilder);
      when(mockBuilder.build()).thenReturn(mockFactory);
      when(mockFactory.createParser(any())).thenReturn(mockFormParser);
      
      // Mock parsing to throw IOException
      try {
        when(mockFormParser.parseBlocking()).thenThrow(new IOException("Test IO error"));
      } catch (IOException e) {
        // This won't actually throw since it's a mock
      }
      
      // Execute and expect RuntimeException
      try {
        handler.handleRequestTrusted(mockExchange);
        assertTrue(false, "Expected RuntimeException was not thrown");
      } catch (RuntimeException e) {
        // Verify it's the IOException wrapped in RuntimeException
        assertTrue(e.getCause() instanceof IOException);
        assertEquals("Test IO error", e.getCause().getMessage());
      }
    }
  }

  @Test
  public void testInvalidApiKeyReturns401() {
    // Setup POST request
    when(mockExchange.getRequestMethod()).thenReturn(new HttpString("POST"));
    when(mockExchange.getResponseHeaders()).thenReturn(mock(HeaderMap.class));
    when(mockExchange.getResponseSender()).thenReturn(mockSender);
    
    // Mock FormParserFactory
    try (MockedStatic<FormParserFactory> mockStatic = mockStatic(FormParserFactory.class)) {
      FormParserFactory mockFactory = mock(FormParserFactory.class);
      FormParserFactory.Builder mockBuilder = mock(FormParserFactory.Builder.class);
      when(FormParserFactory.builder()).thenReturn(mockBuilder);
      when(mockBuilder.build()).thenReturn(mockFactory);
      when(mockFactory.createParser(any())).thenReturn(mockFormParser);
      
      try {
        when(mockFormParser.parseBlocking()).thenReturn(mockFormData);
      } catch (IOException e) {
        // Won't throw for mock
      }
      
      // Mock invalid API key
      try (MockedStatic<ApiKeyUtil> apiKeyMockStatic = mockStatic(ApiKeyUtil.class)) {
        ApiKeyUtil.ApiCheckResult invalidResult = new ApiKeyUtil.ApiCheckResult("", false);
        apiKeyMockStatic.when(() -> ApiKeyUtil.checkApiKey(any(), any())).thenReturn(invalidResult);
        
        // Execute
        Optional<String> result = handler.handleRequestTrusted(mockExchange);
        
        // Verify unauthorized
        assertFalse(result.isPresent());
        verify(mockExchange).setStatusCode(401);
      }
    }
  }

  @Test
  public void testMissingCodeParameterReturns400() {
    // Setup POST request
    when(mockExchange.getRequestMethod()).thenReturn(new HttpString("POST"));
    when(mockExchange.getResponseHeaders()).thenReturn(mock(HeaderMap.class));
    when(mockExchange.getResponseSender()).thenReturn(mockSender);
    
    // Mock FormParserFactory
    try (MockedStatic<FormParserFactory> mockStatic = mockStatic(FormParserFactory.class)) {
      FormParserFactory mockFactory = mock(FormParserFactory.class);
      FormParserFactory.Builder mockBuilder = mock(FormParserFactory.Builder.class);
      when(FormParserFactory.builder()).thenReturn(mockBuilder);
      when(mockBuilder.build()).thenReturn(mockFactory);
      when(mockFactory.createParser(any())).thenReturn(mockFormParser);
      
      try {
        when(mockFormParser.parseBlocking()).thenReturn(mockFormData);
      } catch (IOException e) {
        // Won't throw for mock
      }
      
      // Mock valid API key
      try (MockedStatic<ApiKeyUtil> apiKeyMockStatic = mockStatic(ApiKeyUtil.class)) {
        ApiKeyUtil.ApiCheckResult validResult = new ApiKeyUtil.ApiCheckResult("test-key", true);
        apiKeyMockStatic.when(() -> ApiKeyUtil.checkApiKey(any(), any())).thenReturn(validResult);
        
        // Mock missing code parameter
        when(mockFormData.contains("code")).thenReturn(false);
        
        // Execute
        Optional<String> result = handler.handleRequestTrusted(mockExchange);
        
        // Verify bad request but API key is returned
        assertTrue(result.isPresent());
        assertEquals("test-key", result.get());
        verify(mockExchange).setStatusCode(400);
      }
    }
  }

  @Test
  public void testSuccessfulConfigDiscovery() {
    // Setup POST request
    when(mockExchange.getRequestMethod()).thenReturn(new HttpString("POST"));
    when(mockExchange.getResponseHeaders()).thenReturn(mock(HeaderMap.class));
    when(mockExchange.getResponseSender()).thenReturn(mockSender);
    
    // Mock FormParserFactory
    try (MockedStatic<FormParserFactory> mockStatic = mockStatic(FormParserFactory.class)) {
      FormParserFactory mockFactory = mock(FormParserFactory.class);
      FormParserFactory.Builder mockBuilder = mock(FormParserFactory.Builder.class);
      when(FormParserFactory.builder()).thenReturn(mockBuilder);
      when(mockBuilder.build()).thenReturn(mockFactory);
      when(mockFactory.createParser(any())).thenReturn(mockFormParser);
      
      try {
        when(mockFormParser.parseBlocking()).thenReturn(mockFormData);
      } catch (IOException e) {
        // Won't throw for mock
      }
      
      // Mock valid API key
      try (MockedStatic<ApiKeyUtil> apiKeyMockStatic = mockStatic(ApiKeyUtil.class)) {
        ApiKeyUtil.ApiCheckResult validResult = new ApiKeyUtil.ApiCheckResult("test-key", true);
        apiKeyMockStatic.when(() -> ApiKeyUtil.checkApiKey(any(), any())).thenReturn(validResult);
        
        // Mock form data with code parameter
        when(mockFormData.contains("code")).thenReturn(true);
        when(mockFormData.getFirst("code")).thenReturn(mockFormValue);
        when(mockFormValue.getValue()).thenReturn("""
            start simulation Test
              grid.size = config example.testVar
              steps.high = config example.stepCount
            end simulation
            """);
        
        // Execute
        Optional<String> result = handler.handleRequestTrusted(mockExchange);
        
        // Verify success
        assertTrue(result.isPresent());
        assertEquals("test-key", result.get());
        verify(mockExchange).setStatusCode(200);
      }
    }
  }

  @Test
  public void testEmptyConfigDiscovery() {
    // Setup POST request
    when(mockExchange.getRequestMethod()).thenReturn(new HttpString("POST"));
    when(mockExchange.getResponseHeaders()).thenReturn(mock(HeaderMap.class));
    when(mockExchange.getResponseSender()).thenReturn(mockSender);
    
    // Mock FormParserFactory
    try (MockedStatic<FormParserFactory> mockStatic = mockStatic(FormParserFactory.class)) {
      FormParserFactory mockFactory = mock(FormParserFactory.class);
      FormParserFactory.Builder mockBuilder = mock(FormParserFactory.Builder.class);
      when(FormParserFactory.builder()).thenReturn(mockBuilder);
      when(mockBuilder.build()).thenReturn(mockFactory);
      when(mockFactory.createParser(any())).thenReturn(mockFormParser);
      
      try {
        when(mockFormParser.parseBlocking()).thenReturn(mockFormData);
      } catch (IOException e) {
        // Won't throw for mock
      }
      
      // Mock valid API key
      try (MockedStatic<ApiKeyUtil> apiKeyMockStatic = mockStatic(ApiKeyUtil.class)) {
        ApiKeyUtil.ApiCheckResult validResult = new ApiKeyUtil.ApiCheckResult("test-key", true);
        apiKeyMockStatic.when(() -> ApiKeyUtil.checkApiKey(any(), any())).thenReturn(validResult);
        
        // Mock form data with code parameter (no config variables)
        when(mockFormData.contains("code")).thenReturn(true);
        when(mockFormData.getFirst("code")).thenReturn(mockFormValue);
        when(mockFormValue.getValue()).thenReturn("""
            start simulation Test
              grid.size = 100
              steps.high = 50
            end simulation
            """);
        
        // Execute
        Optional<String> result = handler.handleRequestTrusted(mockExchange);
        
        // Verify success with empty result
        assertTrue(result.isPresent());
        assertEquals("test-key", result.get());
        verify(mockExchange).setStatusCode(200);
      }
    }
  }

}