/**
 * Unit tests for JoshConfigDiscoveryHandler.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.cloud;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


/**
 * Unit tests for JoshConfigDiscoveryHandler.
 */
public class JoshConfigDiscoveryHandlerTest {

  @Mock
  private CloudApiDataLayer mockDataLayer;
  
  @Mock
  private HttpServerExchange mockExchange;

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
  }
}