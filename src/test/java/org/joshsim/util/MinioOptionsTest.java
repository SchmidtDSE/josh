package org.joshsim.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.minio.MinioClient;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MinioOptionsTest {

  private MinioOptions options;
  private ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    // Use a spy on the real MinioOptions to only mock specific methods
    options = spy(new MinioOptions());
  }

  @Test
  void isMinioOutput_withValidEndpoint_returnsTrue() {
    // Mock getMinioEndpoint to return a valid endpoint
    doReturn("http://minio.example.com").when(options).getMinioEndpoint();

    assertTrue(options.isMinioOutput());
  }

  @Test
  void isMinioOutput_withEmptyEndpoint_returnsFalse() {
    doReturn("").when(options).getMinioEndpoint();

    assertFalse(options.isMinioOutput());
  }

  @Test
  void isMinioOutput_withNullEndpoint_returnsFalse() {
    doReturn(null).when(options).getMinioEndpoint();

    assertFalse(options.isMinioOutput());
  }

  @Test
  void isMinioOutput_withException_returnsFalse() {
    doThrow(new IllegalStateException("No endpoint")).when(options).getMinioEndpoint();

    assertFalse(options.isMinioOutput());
  }

  @Test
  void getMinioEndpoint_fromDirectValue() throws Exception {
    setField(options, "minioEndpointMaybe", "http://direct.example.com");

    assertEquals("http://direct.example.com", options.getMinioEndpoint());

    // Verify source was marked correctly
    Map<String, ValueSource> sources = options.getSources();
    assertEquals(ValueSource.DIRECT, sources.get("minio_endpoint"));
  }

  @Test
  void getMinioEndpoint_fromJsonConfig() {
    // Create mock JSON config with endpoint
    ObjectNode configNode = mapper.createObjectNode();
    configNode.put("minio_endpoint", "http://json.example.com");
    doReturn(configNode).when(options).getJsonConfig();

    assertEquals("http://json.example.com", options.getMinioEndpoint());

    // Verify source was marked correctly
    Map<String, ValueSource> sources = options.getSources();
    assertEquals(ValueSource.CONFIG_FILE, sources.get("minio_endpoint"));
  }

  @Test
  void getMinioEndpoint_fromEnvironment() {
    // Mock environment variable
    doReturn("http://env.example.com").when(options).getEnvValue("MINIO_ENDPOINT");

    assertEquals("http://env.example.com", options.getMinioEndpoint());

    // Verify source was marked correctly
    Map<String, ValueSource> sources = options.getSources();
    assertEquals(ValueSource.ENVIRONMENT, sources.get("minio_endpoint"));
  }

  @Test
  void getMinioEndpoint_priorityOrder() throws Exception {
    // Set up all three sources - direct should win
    setField(options, "minioEndpointMaybe", "http://direct.example.com");

    ObjectNode configNode = mapper.createObjectNode();
    configNode.put("minio_endpoint", "http://json.example.com");
    doReturn(configNode).when(options).getJsonConfig();

    doReturn("http://env.example.com").when(options).getEnvValue("MINIO_ENDPOINT");

    assertEquals("http://direct.example.com", options.getMinioEndpoint());
  }

  @Test
  void getCredentials_fromAllSources() throws Exception {
    // Set direct values for endpoint, access key and secret key
    setField(options, "minioEndpointMaybe", "http://localhost:9000");
    setField(options, "minioAccessKeyMaybe", "direct-access");
    setField(options, "minioSecretKeyMaybe", "direct-secret");

    // Build MinioClient to verify it works without errors
    MinioClient client = options.getMinioClient();
    assertNotNull(client);
  }

  @Test
  void getBucketName_withDefaultValue() {
    // No values set explicitly - should return default
    String bucketName = options.getBucketName();
    assertEquals("default", bucketName);
  }

  @Test
  void getObjectName_withDefaultValue() {
    // No values set explicitly - should return default
    assertEquals("", options.getObjectName());
  }

  @Test
  void toString_includesSourceInfo() throws Exception {
    // Set up values from different sources
    setField(options, "minioEndpointMaybe", "http://localhost:9000");

    ObjectNode configNode = mapper.createObjectNode();
    configNode.put("minio_access_key", "json-access");
    doReturn(configNode).when(options).getJsonConfig();

    doReturn("env-secret").when(options).getEnvValue("MINIO_SECRET_KEY");

    String result = options.toString();

    // Verify endpoint and source info are included
    assertTrue(result.contains("Minio Endpoint: http://localhost:9000"));
    assertTrue(result.contains("from DIRECT"));

    // Verify sensitive information is redacted
    assertTrue(result.contains("[REDACTED]"));

    // Verify source tracking info is included
    assertTrue(result.contains("from CONFIG_FILE"));
    assertTrue(result.contains("from ENVIRONMENT"));
  }

  // Helper method for setting private fields
  private void setField(Object obj, String fieldName, Object value) throws Exception {
    Field field = obj.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(obj, value);
  }
}
