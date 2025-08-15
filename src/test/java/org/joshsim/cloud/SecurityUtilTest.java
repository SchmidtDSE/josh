package org.joshsim.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SecurityUtilTest {

  @Test
  void sanitizeErrorMessage_shouldRemoveApiKeys() {
    // Arrange
    String originalMessage = "Error with api_key=sk123456789012345678901234567890";
    RuntimeException exception = new RuntimeException(originalMessage);

    // Act
    String result = SecurityUtil.sanitizeErrorMessage(originalMessage, exception);

    // Assert
    assertTrue(result.contains("[REDACTED-API-KEY]"));
    assertFalse(result.contains("sk123456789012345678901234567890"));
  }

  @Test
  void sanitizeErrorMessage_shouldRemoveTokens() {
    // Arrange
    String originalMessage = "Request failed for token=abcdef123456789012345";
    RuntimeException exception = new RuntimeException(originalMessage);

    // Act
    String result = SecurityUtil.sanitizeErrorMessage(originalMessage, exception);

    // Assert
    assertTrue(result.contains("[REDACTED-TOKEN]"));
    assertFalse(result.contains("abcdef123456789012345"));
  }

  @Test
  void sanitizeErrorMessage_shouldRemovePasswords() {
    // Arrange
    String originalMessage = "Login failed with password=mysecretpassword";
    RuntimeException exception = new RuntimeException(originalMessage);

    // Act
    String result = SecurityUtil.sanitizeErrorMessage(originalMessage, exception);

    // Assert
    assertTrue(result.contains("[REDACTED-PASSWORD]"));
    assertFalse(result.contains("mysecretpassword"));
  }

  @Test
  void sanitizeErrorMessage_shouldRemoveFilePaths() {
    // Arrange
    String originalMessage = "File not found: /home/user/secret/config.properties";
    RuntimeException exception = new RuntimeException(originalMessage);

    // Act
    String result = SecurityUtil.sanitizeErrorMessage(originalMessage, exception);

    // Assert
    assertTrue(result.contains("[REDACTED-PATH]"));
    assertFalse(result.contains("/home/user/secret/config.properties"));
  }

  @Test
  void sanitizeErrorMessage_shouldRemoveWindowsPaths() {
    // Arrange
    String originalMessage = "Error accessing C:\\Users\\admin\\secrets\\keys.txt";
    RuntimeException exception = new RuntimeException(originalMessage);

    // Act
    String result = SecurityUtil.sanitizeErrorMessage(originalMessage, exception);

    // Assert
    assertTrue(result.contains("[REDACTED-PATH]"));
    assertFalse(result.contains("C:\\Users\\admin\\secrets\\keys.txt"));
  }

  @Test
  void sanitizeErrorMessage_shouldReturnGenericMessageForSensitiveKeywords() {
    // Arrange
    String originalMessage = "Internal database class java.sql.Connection failed";
    RuntimeException exception = new RuntimeException(originalMessage);

    // Act
    String result = SecurityUtil.sanitizeErrorMessage(originalMessage, exception);

    // Assert
    assertEquals("Error occurred during simulation execution", result);
  }

  @Test
  void sanitizeErrorMessage_shouldHandleNullMessage() {
    // Arrange
    RuntimeException exception = new RuntimeException("Test");

    // Act
    String result = SecurityUtil.sanitizeErrorMessage(null, exception);

    // Assert
    assertEquals("Error occurred during simulation execution", result);
  }

  @Test
  void sanitizeErrorMessage_shouldProvideSpecificMessageForParseExceptions() {
    // Arrange
    String originalMessage = "Parse error at line 5";
    // Create a real exception with "Parse" in the class name
    Exception parseException = new IllegalArgumentException("ParseException details");

    // Act
    String result = SecurityUtil.sanitizeErrorMessage(originalMessage, parseException);

    // Assert - Should preserve the original message since it's not sensitive
    assertEquals("Parse error at line 5", result);
  }

  @Test
  void createSafeException_shouldReturnSimulationExecutionExceptionWithSanitizedMessage() {
    // Arrange
    String userMessage = "Error with api_key=secret123456789012345";
    RuntimeException originalException = new RuntimeException("Internal error");

    // Act
    SimulationExecutionException result = SecurityUtil.createSafeException(userMessage,
        originalException);

    // Assert
    assertNotNull(result);
    assertTrue(result.getUserMessage().contains("[REDACTED-API-KEY]"));
    assertFalse(result.getUserMessage().contains("secret123456789012345"));
    assertSame(originalException, result.getOriginalCause());
  }

  @Test
  void logSecureError_shouldUseEnhancedLoggingForEnvCloudApiDataLayer() {
    // Arrange
    EnvCloudApiDataLayer mockDataLayer = Mockito.mock(EnvCloudApiDataLayer.class);
    String apiKey = "test-key";
    String operation = "simulation";
    RuntimeException exception = new RuntimeException("Test error");
    String context = "test context";

    // Act
    SecurityUtil.logSecureError(mockDataLayer, apiKey, operation, exception, context);

    // Assert
    Mockito.verify(mockDataLayer, Mockito.times(1))
        .logError(apiKey, operation, exception, context);
  }

  @Test
  void logSecureError_shouldUseFallbackLoggingForOtherDataLayers() {
    // Arrange
    CloudApiDataLayer mockDataLayer = Mockito.mock(CloudApiDataLayer.class);
    String apiKey = "test-key";
    String operation = "simulation";
    RuntimeException exception = new RuntimeException("Test error");
    String context = "test context";

    // Capture stderr
    ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
    PrintStream originalErr = System.err;
    System.setErr(new PrintStream(errorOutput));

    try {
      // Act
      SecurityUtil.logSecureError(mockDataLayer, apiKey, operation, exception, context);

      // Assert
      Mockito.verify(mockDataLayer, Mockito.times(1))
          .log(apiKey, "error_" + operation, 0);

      String errorLog = errorOutput.toString();
      assertTrue(errorLog.contains("[josh cloud error]"));
      assertTrue(errorLog.contains("Error in simulation"));
      assertTrue(errorLog.contains("RuntimeException"));
    } finally {
      System.setErr(originalErr);
    }
  }

  @Test
  void logSecureError_shouldHandleLoggingExceptions() {
    // Arrange
    CloudApiDataLayer mockDataLayer = Mockito.mock(CloudApiDataLayer.class);
    Mockito.doThrow(new RuntimeException("Logging failed")).when(mockDataLayer)
        .log(Mockito.anyString(), Mockito.anyString(), Mockito.anyLong());

    String apiKey = "test-key";
    String operation = "simulation";
    RuntimeException exception = new RuntimeException("Test error");

    // Capture stderr
    ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
    PrintStream originalErr = System.err;
    System.setErr(new PrintStream(errorOutput));

    try {
      // Act
      SecurityUtil.logSecureError(mockDataLayer, apiKey, operation, exception, null);

      // Assert
      String errorLog = errorOutput.toString();
      assertTrue(errorLog.contains("Failed to log error"));
      assertTrue(errorLog.contains("Logging failed"));
    } finally {
      System.setErr(originalErr);
    }
  }

  @Test
  void sanitizeErrorMessage_shouldPreserveSafeMessages() {
    // Arrange
    String originalMessage = "Simulation completed successfully";
    RuntimeException exception = new RuntimeException(originalMessage);

    // Act
    String result = SecurityUtil.sanitizeErrorMessage(originalMessage, exception);

    // Assert
    assertEquals("Simulation completed successfully", result);
  }

  @Test
  void sanitizeErrorMessage_shouldHandleMultiplePatterns() {
    // Arrange
    String originalMessage = "Failed: api_key=secret123 at /path/to/file with token=abc123def";
    RuntimeException exception = new RuntimeException(originalMessage);

    // Act
    String result = SecurityUtil.sanitizeErrorMessage(originalMessage, exception);

    // Assert
    assertTrue(result.contains("[REDACTED-API-KEY]"));
    assertTrue(result.contains("[REDACTED-PATH]"));
    assertTrue(result.contains("[REDACTED-TOKEN]"));
    assertFalse(result.contains("secret123"));
    assertFalse(result.contains("/path/to/file"));
    assertFalse(result.contains("abc123def"));
  }
}
