/**
 * Unit tests for JoshJob class.
 *
 * <p>Tests the immutable job definition including constructor validation,
 * getter methods, defensive copying, and edge cases.</p>
 *
 * @license BSD-3-Clause
 */

package org.joshsim.pipeline.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for JoshJob immutable job definition.
 *
 * <p>Validates immutability, defensive copying, getter methods,
 * and constructor validation behavior.</p>
 */
public class JoshJobTest {

  @Test
  public void testValidConstruction() {
    Map<String, JoshJobFileInfo> fileInfos = new HashMap<>();
    fileInfos.put("example.jshc", new JoshJobFileInfo("example_1", "test_data/example_1.jshc"));
    fileInfos.put("other.jshd", new JoshJobFileInfo("other_1", "test_data/other_1.jshd"));

    JoshJob job = new JoshJob(fileInfos, 5);

    assertNotNull(job);
    assertEquals(5, job.getReplicates());
    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
    assertEquals("test_data/other_1.jshd", job.getFilePath("other.jshd"));
  }

  @Test
  public void testInvalidReplicatesZero() {
    Map<String, JoshJobFileInfo> fileInfos = new HashMap<>();

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new JoshJob(fileInfos, 0)
    );

    assertTrue(exception.getMessage().contains("Number of replicates must be greater than 0"));
  }

  @Test
  public void testInvalidReplicatesNegative() {
    Map<String, JoshJobFileInfo> fileInfos = new HashMap<>();

    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new JoshJob(fileInfos, -3)
    );

    assertTrue(exception.getMessage().contains("Number of replicates must be greater than 0"));
  }

  @Test
  public void testGetFilePathNonExistent() {
    Map<String, JoshJobFileInfo> fileInfos = new HashMap<>();
    fileInfos.put("example.jshc", new JoshJobFileInfo("example_1", "test_data/example_1.jshc"));

    JoshJob job = new JoshJob(fileInfos, 1);

    assertNull(job.getFilePath("nonexistent.jshc"));
  }

  @Test
  public void testDefensiveCopyingGetFilePaths() {
    Map<String, JoshJobFileInfo> originalFileInfos = new HashMap<>();
    originalFileInfos.put("example.jshc",
        new JoshJobFileInfo("example_1", "test_data/example_1.jshc"));

    JoshJob job = new JoshJob(originalFileInfos, 1);

    // Get the map and modify it
    Map<String, String> retrievedPaths = job.getFilePaths();
    retrievedPaths.put("malicious.jshc", "malicious_path");

    // Original job should be unaffected
    assertNull(job.getFilePath("malicious.jshc"));
    assertEquals(1, job.getFileNames().size());
  }

  @Test
  public void testDefensiveCopyingConstructor() {
    Map<String, JoshJobFileInfo> originalFileInfos = new HashMap<>();
    originalFileInfos.put("example.jshc",
        new JoshJobFileInfo("example_1", "test_data/example_1.jshc"));

    JoshJob job = new JoshJob(originalFileInfos, 1);

    // Modify the original map
    originalFileInfos.put("malicious.jshc", new JoshJobFileInfo("malicious", "malicious_path"));

    // Job should be unaffected
    assertNull(job.getFilePath("malicious.jshc"));
    assertEquals(1, job.getFileNames().size());
  }

  @Test
  public void testGetFileNamesDefensiveCopy() {
    Map<String, JoshJobFileInfo> fileInfos = new HashMap<>();
    fileInfos.put("example.jshc", new JoshJobFileInfo("example_1", "test_data/example_1.jshc"));
    fileInfos.put("other.jshd", new JoshJobFileInfo("other_1", "test_data/other_1.jshd"));

    JoshJob job = new JoshJob(fileInfos, 1);

    Set<String> fileNames1 = job.getFileNames();
    Set<String> fileNames2 = job.getFileNames();

    // Should return different instances (defensive copying)
    assertNotSame(fileNames1, fileNames2);
    assertEquals(fileNames1, fileNames2);
    assertEquals(2, fileNames1.size());
    assertTrue(fileNames1.contains("example.jshc"));
    assertTrue(fileNames1.contains("other.jshd"));
  }

  @Test
  public void testEmptyFilePaths() {
    Map<String, JoshJobFileInfo> fileInfos = new HashMap<>();

    JoshJob job = new JoshJob(fileInfos, 1);

    assertEquals(1, job.getReplicates());
    assertTrue(job.getFileNames().isEmpty());
    assertTrue(job.getFilePaths().isEmpty());
    assertNull(job.getFilePath("any.jshc"));
  }

  @Test
  public void testGetFilePathsReturnsDefensiveCopy() {
    Map<String, JoshJobFileInfo> originalFileInfos = new HashMap<>();
    originalFileInfos.put("example.jshc",
        new JoshJobFileInfo("example_1", "test_data/example_1.jshc"));

    JoshJob job = new JoshJob(originalFileInfos, 3);

    Map<String, String> retrievedPaths1 = job.getFilePaths();
    Map<String, String> retrievedPaths2 = job.getFilePaths();

    // Should return different instances (defensive copying)
    assertNotSame(retrievedPaths1, retrievedPaths2);
    assertEquals(retrievedPaths1, retrievedPaths2);
    assertEquals("test_data/example_1.jshc", retrievedPaths1.get("example.jshc"));
  }

  @Test
  public void testLargeNumberOfReplicates() {
    Map<String, JoshJobFileInfo> fileInfos = new HashMap<>();
    fileInfos.put("example.jshc", new JoshJobFileInfo("example_1", "test_data/example_1.jshc"));

    JoshJob job = new JoshJob(fileInfos, Integer.MAX_VALUE);

    assertEquals(Integer.MAX_VALUE, job.getReplicates());
    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
  }

  @Test
  public void testGetFileInfo() {
    Map<String, JoshJobFileInfo> fileInfos = new HashMap<>();
    JoshJobFileInfo expectedFileInfo = new JoshJobFileInfo("example_1", "test_data/example_1.jshc");
    fileInfos.put("example.jshc", expectedFileInfo);

    JoshJob job = new JoshJob(fileInfos, 1);

    JoshJobFileInfo actualFileInfo = job.getFileInfo("example.jshc");
    assertEquals(expectedFileInfo, actualFileInfo);
    assertEquals("example_1", actualFileInfo.getName());
    assertEquals("test_data/example_1.jshc", actualFileInfo.getPath());
  }

  @Test
  public void testGetFileInfoNonExistent() {
    Map<String, JoshJobFileInfo> fileInfos = new HashMap<>();
    fileInfos.put("example.jshc", new JoshJobFileInfo("example_1", "test_data/example_1.jshc"));

    JoshJob job = new JoshJob(fileInfos, 1);

    assertNull(job.getFileInfo("nonexistent.jshc"));
  }

  @Test
  public void testGetFileInfosDefensiveCopy() {
    Map<String, JoshJobFileInfo> originalFileInfos = new HashMap<>();
    JoshJobFileInfo fileInfo = new JoshJobFileInfo("example_1", "test_data/example_1.jshc");
    originalFileInfos.put("example.jshc", fileInfo);

    JoshJob job = new JoshJob(originalFileInfos, 1);

    Map<String, JoshJobFileInfo> retrievedInfos1 = job.getFileInfos();
    Map<String, JoshJobFileInfo> retrievedInfos2 = job.getFileInfos();

    // Should return different instances (defensive copying)
    assertNotSame(retrievedInfos1, retrievedInfos2);
    assertEquals(retrievedInfos1, retrievedInfos2);
    assertEquals(fileInfo, retrievedInfos1.get("example.jshc"));

    // Modifying returned map should not affect job
    retrievedInfos1.put("malicious.jshc", new JoshJobFileInfo("malicious", "malicious_path"));
    assertNull(job.getFileInfo("malicious.jshc"));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testBackwardCompatibilityMethods() {
    Map<String, JoshJobFileInfo> fileInfos = new HashMap<>();
    fileInfos.put("example.jshc", new JoshJobFileInfo("example_1", "test_data/example_1.jshc"));
    fileInfos.put("other.jshd", new JoshJobFileInfo("other_1", "test_data/other_1.jshd"));

    JoshJob job = new JoshJob(fileInfos, 2);

    // Test deprecated getFilePath method
    assertEquals("test_data/example_1.jshc", job.getFilePath("example.jshc"));
    assertEquals("test_data/other_1.jshd", job.getFilePath("other.jshd"));
    assertNull(job.getFilePath("nonexistent.jshc"));

    // Test deprecated getFilePaths method
    Map<String, String> filePaths = job.getFilePaths();
    assertEquals(2, filePaths.size());
    assertEquals("test_data/example_1.jshc", filePaths.get("example.jshc"));
    assertEquals("test_data/other_1.jshd", filePaths.get("other.jshd"));

    // Modifying returned map should not affect job
    filePaths.put("malicious.jshc", "malicious_path");
    assertNull(job.getFilePath("malicious.jshc"));
  }
}
