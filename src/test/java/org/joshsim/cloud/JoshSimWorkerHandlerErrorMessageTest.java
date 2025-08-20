package org.joshsim.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for testing error message building functionality
 * in JoshSimWorkerHandler for external data errors.
 */
class JoshSimWorkerHandlerErrorMessageTest {

  private JoshSimWorkerHandler handler;

  @BeforeEach
  void setUp() {
    CloudApiDataLayer mockApiLayer = new CloudApiDataLayer() {
      @Override
      public boolean apiKeyIsValid(String apiKey) {
        return true;
      }

      @Override
      public void log(String apiKey, String operation, long runtimeSeconds) {
        // No-op for testing
      }
    };

    handler = new JoshSimWorkerHandler(mockApiLayer, true, java.util.Optional.empty(), true);
  }

  @Test
  void testBuildInformativeErrorMessageVirtualFileNotFound() throws Exception {
    // Use reflection to access the private method
    Method method = JoshSimWorkerHandler.class.getDeclaredMethod(
        "buildInformativeErrorMessage", Exception.class);
    method.setAccessible(true);

    RuntimeException exception = new RuntimeException("Cannot find virtual file: test_data.csv");
    String result = (String) method.invoke(handler, exception);

    assertTrue(result.contains("External data file not found: 'test_data.csv'"));
    assertTrue(result.contains("Please ensure the file is included in your external data upload"));
  }

  @Test
  void testBuildInformativeErrorMessageMissingColumns() throws Exception {
    Method method = JoshSimWorkerHandler.class.getDeclaredMethod(
        "buildInformativeErrorMessage", Exception.class);
    method.setAccessible(true);

    IOException exception = new IOException(
        "CSV must contain 'longitude' and 'latitude' columns");
    String result = (String) method.invoke(handler, exception);

    assertTrue(result.contains("External CSV data is missing required columns"));
    assertTrue(result.contains("'longitude' and 'latitude' columns"));
  }

  @Test
  void testBuildInformativeErrorMessageInvalidNumericValue() throws Exception {
    Method method = JoshSimWorkerHandler.class.getDeclaredMethod(
        "buildInformativeErrorMessage", Exception.class);
    method.setAccessible(true);

    RuntimeException exception = new RuntimeException(
        "Invalid numeric value in column 'longitude': not_a_number");
    String result = (String) method.invoke(handler, exception);

    assertTrue(result.contains("External data contains invalid numeric values"));
    assertTrue(result.contains("not_a_number"));
  }

  @Test
  void testBuildInformativeErrorMessageJshdResourceFailure() throws Exception {
    Method method = JoshSimWorkerHandler.class.getDeclaredMethod(
        "buildInformativeErrorMessage", Exception.class);
    method.setAccessible(true);

    RuntimeException exception = new RuntimeException(
        "Failure in loading a jshd resource: File format error");
    String result = (String) method.invoke(handler, exception);

    assertTrue(result.contains("Failed to load external data resource"));
    assertTrue(result.contains("File format error"));
  }

  @Test
  void testBuildInformativeErrorMessageUnsupportedFileFormat() throws Exception {
    Method method = JoshSimWorkerHandler.class.getDeclaredMethod(
        "buildInformativeErrorMessage", Exception.class);
    method.setAccessible(true);

    IOException exception = new IOException(
        "No suitable reader found for file: unknown_format.xyz");
    String result = (String) method.invoke(handler, exception);

    assertTrue(result.contains(
        "Unsupported external data file format: 'unknown_format.xyz'"));
    assertTrue(result.contains("Supported formats include CSV and NetCDF"));
  }

  @Test
  void testBuildInformativeErrorMessageFileAccessError() throws Exception {
    Method method = JoshSimWorkerHandler.class.getDeclaredMethod(
        "buildInformativeErrorMessage", Exception.class);
    method.setAccessible(true);

    IOException exception = new IOException("data.csv (No such file or directory)");
    String result = (String) method.invoke(handler, exception);

    assertTrue(result.contains("External data file could not be accessed"));
    assertTrue(result.contains(
        "Please ensure the file exists and is properly uploaded"));
  }

  @Test
  void testBuildInformativeErrorMessageGenericExternalDataError() throws Exception {
    Method method = JoshSimWorkerHandler.class.getDeclaredMethod(
        "buildInformativeErrorMessage", Exception.class);
    method.setAccessible(true);

    RuntimeException exception = new RuntimeException(
        "External data processing failed unexpectedly");
    String result = (String) method.invoke(handler, exception);

    assertTrue(result.contains("External data processing error"));
    assertTrue(result.contains("Please check your external data files"));
  }

  @Test
  void testBuildInformativeErrorMessageFallbackError() throws Exception {
    Method method = JoshSimWorkerHandler.class.getDeclaredMethod(
        "buildInformativeErrorMessage", Exception.class);
    method.setAccessible(true);

    RuntimeException exception = new RuntimeException("Some unexpected error");
    String result = (String) method.invoke(handler, exception);

    assertEquals("Simulation error: Some unexpected error", result);
  }
}
