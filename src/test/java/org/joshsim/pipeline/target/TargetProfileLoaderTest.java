/**
 * Tests for TargetProfileLoader.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;


/**
 * Unit tests for {@link TargetProfileLoader}.
 */
class TargetProfileLoaderTest {

  @TempDir
  File tempDir;

  @Test
  void loadsHttpProfile() throws Exception {
    String json = """
        {
          "type": "http",
          "http": {
            "endpoint": "https://josh-executor.run.app",
            "apiKey": "test-key-123"
          },
          "minio_endpoint": "https://storage.googleapis.com",
          "minio_access_key": "access",
          "minio_secret_key": "secret",
          "minio_bucket": "josh-storage"
        }
        """;
    writeProfile("cloudrun-prod", json);

    TargetProfileLoader loader = new TargetProfileLoader(tempDir);
    TargetProfile profile = loader.load("cloudrun-prod");

    assertEquals("http", profile.getType());
    assertNotNull(profile.getHttpConfig());
    assertEquals("https://josh-executor.run.app", profile.getHttpConfig().getEndpoint());
    assertEquals("test-key-123", profile.getHttpConfig().getApiKey());
    assertEquals("https://storage.googleapis.com", profile.getMinioEndpoint());
    assertEquals("access", profile.getMinioAccessKey());
    assertEquals("secret", profile.getMinioSecretKey());
    assertEquals("josh-storage", profile.getMinioBucket());
    assertNull(profile.getKubernetesConfig());
  }

  @Test
  void loadsKubernetesProfile() throws Exception {
    String json = """
        {
          "type": "kubernetes",
          "kubernetes": {
            "context": "nautilus",
            "namespace": "joshsim-lab",
            "image": "ghcr.io/schmidtdse/joshsim-job:latest",
            "resources": {
              "requests": { "cpu": "2", "memory": "4Gi" },
              "limits": { "memory": "256Gi" }
            },
            "parallelism": 10,
            "timeoutSeconds": 3600
          },
          "minio_endpoint": "https://minio.example.com",
          "minio_access_key": "k8s-access",
          "minio_secret_key": "k8s-secret",
          "minio_bucket": "k8s-bucket"
        }
        """;
    writeProfile("nautilus", json);

    TargetProfileLoader loader = new TargetProfileLoader(tempDir);
    TargetProfile profile = loader.load("nautilus");

    assertEquals("kubernetes", profile.getType());
    assertNotNull(profile.getKubernetesConfig());
    assertEquals("nautilus", profile.getKubernetesConfig().getContext());
    assertEquals("joshsim-lab", profile.getKubernetesConfig().getNamespace());
    assertEquals("ghcr.io/schmidtdse/joshsim-job:latest",
        profile.getKubernetesConfig().getImage());
    assertEquals(10, profile.getKubernetesConfig().getParallelism());
    assertEquals(3600, profile.getKubernetesConfig().getTimeoutSeconds());
    assertNotNull(profile.getKubernetesConfig().getResources());
    assertEquals("2", profile.getKubernetesConfig().getResources()
        .get("requests").get("cpu"));
    assertNull(profile.getHttpConfig());
  }

  @Test
  void throwsOnMissingFile() {
    TargetProfileLoader loader = new TargetProfileLoader(tempDir);
    IOException exception = assertThrows(IOException.class, () -> loader.load("nonexistent"));
    assertTrue(exception.getMessage().contains("Target profile not found"));
  }

  @Test
  void throwsOnMalformedJson() throws Exception {
    writeProfile("bad", "{ this is not valid json }");
    TargetProfileLoader loader = new TargetProfileLoader(tempDir);
    assertThrows(Exception.class, () -> loader.load("bad"));
  }

  @Test
  void throwsOnMissingType() throws Exception {
    writeProfile("no-type", """
        {
          "http": { "endpoint": "https://example.com", "apiKey": "key" }
        }
        """);
    TargetProfileLoader loader = new TargetProfileLoader(tempDir);
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class, () -> loader.load("no-type")
    );
    assertTrue(exception.getMessage().contains("missing required 'type'"));
  }

  @Test
  void throwsOnHttpTypeWithoutHttpConfig() throws Exception {
    writeProfile("bad-http", """
        {
          "type": "http",
          "minio_endpoint": "https://example.com"
        }
        """);
    TargetProfileLoader loader = new TargetProfileLoader(tempDir);
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class, () -> loader.load("bad-http")
    );
    assertTrue(exception.getMessage().contains("missing 'http' config block"));
  }

  @Test
  void throwsOnKubernetesTypeWithoutKubernetesConfig() throws Exception {
    writeProfile("bad-k8s", """
        {
          "type": "kubernetes",
          "minio_endpoint": "https://example.com"
        }
        """);
    TargetProfileLoader loader = new TargetProfileLoader(tempDir);
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class, () -> loader.load("bad-k8s")
    );
    assertTrue(exception.getMessage().contains("missing 'kubernetes' config block"));
  }

  @Test
  void ignoresUnknownFields() throws Exception {
    writeProfile("extra-fields", """
        {
          "type": "http",
          "http": { "endpoint": "https://example.com", "apiKey": "key" },
          "minio_endpoint": "https://storage.example.com",
          "minio_access_key": "a", "minio_secret_key": "s", "minio_bucket": "b",
          "some_future_field": "should not break parsing"
        }
        """);
    TargetProfileLoader loader = new TargetProfileLoader(tempDir);
    TargetProfile profile = loader.load("extra-fields");
    assertEquals("http", profile.getType());
  }

  private void writeProfile(String name, String content) throws IOException {
    Files.writeString(new File(tempDir, name + ".json").toPath(), content);
  }
}
