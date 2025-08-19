/**
 * Unit tests for RunRemoteCommand class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/**
 * Test cases for RunRemoteCommand functionality.
 */
public class RunRemoteCommandTest {

  private RunRemoteCommand command;

  /**
   * Set up test instance.
   */
  @BeforeEach
  public void setUp() {
    command = new RunRemoteCommand();
  }

  @Test
  public void testValidateAndParseEndpoint() throws Exception {
    Method method = RunRemoteCommand.class.getDeclaredMethod(
        "validateAndParseEndpoint", String.class);
    method.setAccessible(true);
    
    // Test valid HTTP endpoint
    URI result = (URI) method.invoke(command, "http://example.com/runReplicates");
    assertEquals("http://example.com/runReplicates", result.toString());
    
    // Test valid HTTPS endpoint
    result = (URI) method.invoke(command, "https://example.com/runReplicates");
    assertEquals("https://example.com/runReplicates", result.toString());
    
    // Test endpoint without /runReplicates - should be appended
    result = (URI) method.invoke(command, "http://example.com");
    assertEquals("http://example.com/runReplicates", result.toString());
    
    // Test endpoint with path but without /runReplicates
    result = (URI) method.invoke(command, "http://example.com/api");
    assertEquals("http://example.com/api/runReplicates", result.toString());
  }

  @Test
  public void testValidateAndParseEndpointInvalidScheme() throws Exception {
    Method method = RunRemoteCommand.class.getDeclaredMethod(
        "validateAndParseEndpoint", String.class);
    method.setAccessible(true);
    
    // Test invalid scheme - should wrap in InvocationTargetException
    assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
      method.invoke(command, "ftp://example.com");
    });
    
    // Test missing scheme - should wrap in InvocationTargetException
    assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
      method.invoke(command, "example.com");
    });
  }

  @Test
  public void testIsTextFile() throws Exception {
    Method method = RunRemoteCommand.class.getDeclaredMethod("isTextFile", String.class);
    method.setAccessible(true);
    
    // Test text file extensions
    assertTrue((Boolean) method.invoke(command, "test.csv"));
    assertTrue((Boolean) method.invoke(command, "test.txt"));
    assertTrue((Boolean) method.invoke(command, "test.jshc"));
    assertTrue((Boolean) method.invoke(command, "test.josh"));
    
    // Test binary file extensions
    assertEquals(false, (Boolean) method.invoke(command, "test.jshd"));
    assertEquals(false, (Boolean) method.invoke(command, "test.bin"));
    assertEquals(false, (Boolean) method.invoke(command, "test.pdf"));
  }

  @Test
  public void testParseDataFiles() throws Exception {
    Method method = RunRemoteCommand.class.getDeclaredMethod("parseDataFiles", String[].class);
    method.setAccessible(true);
    
    String[] dataFiles = {"config.jshc=/path/to/config.jshc", "data.jshd=/path/to/data.jshd"};
    Map<String, String> result = (Map<String, String>) method.invoke(command, (Object) dataFiles);
    
    assertEquals(2, result.size());
    assertEquals("/path/to/config.jshc", result.get("config.jshc"));
    assertEquals("/path/to/data.jshd", result.get("data.jshd"));
  }

  @Test
  public void testParseDataFilesInvalidFormat() throws Exception {
    Method method = RunRemoteCommand.class.getDeclaredMethod("parseDataFiles", String[].class);
    method.setAccessible(true);
    
    String[] invalidDataFiles = {"invalid_format"};
    
    assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
      method.invoke(command, (Object) invalidDataFiles);
    });
  }

  @Test
  public void testParseDataFilesWithSpaces() throws Exception {
    Method method = RunRemoteCommand.class.getDeclaredMethod(
        "parseDataFiles", String[].class);
    method.setAccessible(true);
    
    String[] dataFiles = {" config.jshc = /path/to/config.jshc ", 
        " data.jshd = /path/to/data.jshd "};
    Map<String, String> result = (Map<String, String>) method.invoke(
        command, (Object) dataFiles);
    
    assertEquals(2, result.size());
    assertEquals("/path/to/config.jshc", result.get("config.jshc"));
    assertEquals("/path/to/data.jshd", result.get("data.jshd"));
  }

  @Test
  public void testSerializeExternalDataText(@TempDir Path tempDir) throws Exception {
    // Create test files
    Path textFile = tempDir.resolve("test.txt");
    Files.writeString(textFile, "Hello\tWorld\nLine 2");
    
    // Set up command with data files using reflection
    java.lang.reflect.Field dataFilesField = RunRemoteCommand.class.getDeclaredField("dataFiles");
    dataFilesField.setAccessible(true);
    String[] dataFiles = {"test.txt=" + textFile.toString()};
    dataFilesField.set(command, dataFiles);
    
    // Test serialization
    Method method = RunRemoteCommand.class.getDeclaredMethod("serializeExternalData");
    method.setAccessible(true);
    String result = (String) method.invoke(command);
    
    // Just verify it contains the basic structure (the exact spacing might vary)
    assertTrue(result.contains("test.txt\t0\t"));
    assertTrue(result.contains("Hello"));
    assertTrue(result.contains("World"));
    assertTrue(result.contains("Line 2"));
  }

  @Test
  public void testSerializeExternalDataBinary(@TempDir Path tempDir) throws Exception {
    // Create test binary file
    Path binaryFile = tempDir.resolve("test.jshd");
    byte[] binaryData = {1, 2, 3, 4, 5};
    Files.write(binaryFile, binaryData);
    
    // Set up command with data files using reflection
    java.lang.reflect.Field dataFilesField = RunRemoteCommand.class.getDeclaredField("dataFiles");
    dataFilesField.setAccessible(true);
    String[] dataFiles = {"test.jshd=" + binaryFile.toString()};
    dataFilesField.set(command, dataFiles);
    
    Method serializeMethod = RunRemoteCommand.class.getDeclaredMethod("serializeExternalData");
    serializeMethod.setAccessible(true);
    String result = (String) serializeMethod.invoke(command);
    
    // Verify format: filename\tbinary_flag\tcontent\t
    assertTrue(result.contains("test.jshd\t1\t"));
    assertTrue(result.contains("AQIDBAU=\t")); // Base64 of {1,2,3,4,5}
  }

  @Test
  public void testBuildFormData() throws Exception {
    Method method = RunRemoteCommand.class.getDeclaredMethod("buildFormData", 
        String.class, String.class, String.class, String.class);
    method.setAccessible(true);
    
    String result = (String) method.invoke(command, 
        "simulation code", "TestSim", "test-api-key", "external data");
    
    // Verify all required fields are present and URL encoded (spaces become + in URL encoding)
    assertTrue(result.contains("code=simulation+code"));
    assertTrue(result.contains("name=TestSim"));
    assertTrue(result.contains("apiKey=test-api-key"));
    assertTrue(result.contains("externalData=external+data"));
    assertTrue(result.contains("replicates=1"));
    assertTrue(result.contains("favorBigDecimal=true"));
  }

  @Test
  public void testBuildFormDataWithFloat64() throws Exception {
    // Set useFloat64 to true via reflection
    java.lang.reflect.Field field = RunRemoteCommand.class.getDeclaredField(
        "useFloat64");
    field.setAccessible(true);
    field.set(command, true);
    
    Method method = RunRemoteCommand.class.getDeclaredMethod("buildFormData", 
        String.class, String.class, String.class, String.class);
    method.setAccessible(true);
    
    String result = (String) method.invoke(command, 
        "simulation code", "TestSim", "test-api-key", "external data");
    
    assertTrue(result.contains("favorBigDecimal=false"));
  }

  @Test
  public void testEmptyDataFiles() throws Exception {
    Method method = RunRemoteCommand.class.getDeclaredMethod("parseDataFiles", String[].class);
    method.setAccessible(true);
    
    String[] emptyDataFiles = {};
    Map<String, String> result = (Map<String, String>) method.invoke(
        command, (Object) emptyDataFiles);
    
    assertEquals(0, result.size());
  }
}