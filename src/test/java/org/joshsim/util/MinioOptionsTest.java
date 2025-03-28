package org.joshsim.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.minio.MinioClient;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

public class MinioOptionsTest {

  private MinioOptions options;

  @BeforeEach
  void setUp() {
    options = new MinioOptions();
  }

  @Test
  void isMinioOutput_withNullOutput_returnsFalse() {
    assertFalse(options.isMinioOutput());
  }

  @Test
  void isMinioOutput_withMinioUrl_returnsTrue() throws Exception {
    setField(options, "output", "minio://bucket/path");
    assertTrue(options.isMinioOutput());
  }

  @Test
  void isMinioOutput_withNonMinioUrl_returnsFalse() throws Exception {
    setField(options, "output", "http://example.com/file");
    assertFalse(options.isMinioOutput());
  }

  @Test
  void getBucketName_withNullOutput_returnsNull() {
    assertNull(options.getBucketName());
  }

  @Test
  void getBucketName_withValidUrl_returnsBucketName() throws Exception {
    setField(options, "output", "minio://mybucket/path/to/file");
    assertEquals("mybucket", options.getBucketName());
  }

  @Test
  void getBucketName_withNoPaths_returnsBucketName() throws Exception {
    setField(options, "output", "minio://mybucket");
    assertEquals("mybucket", options.getBucketName());
  }

  @Test
  void getObjectName_withNullOutput_returnsNull() {
    assertNull(options.getObjectName());
  }

  @Test
  void getObjectName_withValidUrl_returnsPath() throws Exception {
    setField(options, "output", "minio://mybucket/path/to/file");
    assertEquals("path/to/file", options.getObjectName());
  }

  @Test
  void getObjectName_withNoPaths_returnsEmptyString() throws Exception {
    setField(options, "output", "minio://mybucket");
    assertEquals("", options.getObjectName());
  }

  @Test
  void getMinioEndpoint_returnsConfiguredValue() throws Exception {
    setField(options, "minioEndpoint", "https://minio.example.com");
    assertEquals("https://minio.example.com", options.getMinioEndpoint());
  }

  @Test
  void getCredentials_withCommandLineArgs_usesCommandLineArgs() throws Exception {
    // Set up command line args
    setField(options, "minioKey", "cli-key");
    setField(options, "minioSecret", "cli-secret");

    // Use reflection to call private getCredentials method
    String[] credentials = invokeGetCredentials(options);

    assertEquals("cli-key", credentials[0]);
    assertEquals("cli-secret", credentials[1]);
  }

  @Test
  void getCredentials_withEnvironmentVars_usesEnvironmentVars() throws Exception {
    try (MockedStatic<System> mockedSystem = mockStatic(System.class)) {
      // Mock environment variables
      mockedSystem.when(() -> System.getenv("MINIO_ACCESS_KEY")).thenReturn("env-key");
      mockedSystem.when(() -> System.getenv("MINIO_SECRET_KEY")).thenReturn("env-secret");
      mockedSystem.when(() -> System.getenv(anyString())).thenCallRealMethod();

      String[] credentials = invokeGetCredentials(options);

      assertEquals("env-key", credentials[0]);
      assertEquals("env-secret", credentials[1]);
    }
  }

  @Test
  void getCredentials_withJsonFile_usesJsonFile(@TempDir Path tempDir) throws Exception {
    // Create a temporary credentials file
    File credFile = createCredentialsFile(tempDir, "json-key", "json-secret");

    // Set the credentials file in the options
    setField(options, "credentialsFile", credFile);

    String[] credentials = invokeGetCredentials(options);

    assertEquals("json-key", credentials[0]);
    assertEquals("json-secret", credentials[1]);
  }

  @Test
  void getCredentials_withNoValidSource_throwsException() {
    assertThrows(IllegalStateException.class, () -> invokeGetCredentials(options));
  }

  @Test
  void getCredentials_prioritizesCommandLineOverEnvironment() throws Exception {
    // Set up command line args
    setField(options, "minioKey", "cli-key");
    setField(options, "minioSecret", "cli-secret");

    try (MockedStatic<System> mockedSystem = mockStatic(System.class)) {
      // Mock environment variables
      mockedSystem.when(() -> System.getenv("MINIO_ACCESS_KEY")).thenReturn("env-key");
      mockedSystem.when(() -> System.getenv("MINIO_SECRET_KEY")).thenReturn("env-secret");
      mockedSystem.when(() -> System.getenv(anyString())).thenCallRealMethod();

      String[] credentials = invokeGetCredentials(options);

      assertEquals("cli-key", credentials[0]);
      assertEquals("cli-secret", credentials[1]);
    }
  }

  @Test
  void getCredentials_prioritizesEnvironmentOverJsonFile(@TempDir Path tempDir) throws Exception {
    // Create a temporary credentials file
    File credFile = createCredentialsFile(tempDir, "json-key", "json-secret");

    // Set the credentials file in the options
    setField(options, "credentialsFile", credFile);

    try (MockedStatic<System> mockedSystem = mockStatic(System.class)) {
      // Mock environment variables
      mockedSystem.when(() -> System.getenv("MINIO_ACCESS_KEY")).thenReturn("env-key");
      mockedSystem.when(() -> System.getenv("MINIO_SECRET_KEY")).thenReturn("env-secret");
      mockedSystem.when(() -> System.getenv(anyString())).thenCallRealMethod();

      String[] credentials = invokeGetCredentials(options);

      assertEquals("env-key", credentials[0]);
      assertEquals("env-secret", credentials[1]);
    }
  }

  @Test
  void getMinioClient_buildsClientWithCredentials() throws Exception {
    // Create a MinioOptions with command line credentials
    MinioOptions testOptions = new MinioOptions();
    setField(testOptions, "minioKey", "test-key");
    setField(testOptions, "minioSecret", "test-secret");
    setField(testOptions, "minioEndpoint", "http://localhost:9000");

    // Call getMinioClient
    MinioClient client = testOptions.getMinioClient();

    // We can't directly inspect MinioClient fields as they're private
    // This is more of an integration test to ensure it builds without exceptions
    assertNotNull(client);
  }

  // Helper methods

  private void setField(Object obj, String fieldName, Object value) throws Exception {
    Field field = obj.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(obj, value);
  }

  private String[] invokeGetCredentials(MinioOptions options) throws Exception {
    var method = MinioOptions.class.getDeclaredMethod("getCredentials");
    method.setAccessible(true);
    return (String[]) method.invoke(options);
  }

  private File createCredentialsFile(Path tempDir, String key, String secret) throws IOException {
    File credFile = tempDir.resolve("credentials.json").toFile();

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode rootNode = mapper.createObjectNode();
    rootNode.put("minio_access_key", key);
    rootNode.put("minio_secret_key", secret);

    mapper.writeValue(credFile, rootNode);
    return credFile;
  }
}
