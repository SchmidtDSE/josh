/**
 * Tests for OutputTarget class.
 *
 * @license BSD-3-Clause
 */

package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Test suite for OutputTarget class.
 *
 * <p>These tests verify that OutputTarget correctly stores URI components, extracts file types,
 * reconstructs URIs, and provides helper methods for identifying destination types.</p>
 */
class OutputTargetTest {

  // ========== File Type Extraction Tests ==========

  @Test
  void testGetFileTypeWithValidExtension() {
    // Arrange
    OutputTarget target = new OutputTarget("local", "", "/files/document.txt");

    // Act
    String fileType = target.getFileType();

    // Assert
    assertEquals("txt", fileType);
  }

  @Test
  void testGetFileTypeWithNoExtension() {
    // Arrange
    OutputTarget target = new OutputTarget("local", "/files/document");

    // Act
    String fileType = target.getFileType();

    // Assert
    assertEquals("", fileType);
  }

  @Test
  void testGetFileTypeWithPathEndingWithDot() {
    // Arrange
    OutputTarget target = new OutputTarget("/files/document.");

    // Act
    String fileType = target.getFileType();

    // Assert
    assertEquals("", fileType);
  }

  @Test
  void testGetFileTypeWithMultipleDots() {
    // Arrange
    OutputTarget target = new OutputTarget("local", "", "/files/archive.tar.gz");

    // Act
    String fileType = target.getFileType();

    // Assert
    assertEquals("gz", fileType);
  }

  @Test
  void testGetFileTypeOnRootFile() {
    // Arrange
    OutputTarget target = new OutputTarget("local", "/document.pdf");

    // Act
    String fileType = target.getFileType();

    // Assert
    assertEquals("pdf", fileType);
  }

  @Test
  void testGetFileTypeWithEmptyPath() {
    // Arrange
    OutputTarget target = new OutputTarget("local", "", "");

    // Act
    String fileType = target.getFileType();

    // Assert
    assertEquals("", fileType);
  }

  @Test
  void testGetFileTypeWithPathOnlySlash() {
    // Arrange
    OutputTarget target = new OutputTarget("local", "", "/");

    // Act
    String fileType = target.getFileType();

    // Assert
    assertEquals("", fileType);
  }

  // ========== URI Reconstruction Tests ==========

  @Test
  void testToUriWithFileProtocolEmptyHost() {
    // Arrange
    OutputTarget target = new OutputTarget("file", "", "/tmp/output.csv");

    // Act
    String uri = target.toUri();

    // Assert
    assertEquals("file:///tmp/output.csv", uri);
  }

  @Test
  void testToUriWithMinioProtocolAndHost() {
    // Arrange
    OutputTarget target = new OutputTarget("minio", "bucket", "/path/to/file.csv");

    // Act
    String uri = target.toUri();

    // Assert
    assertEquals("minio://bucket/path/to/file.csv", uri);
  }

  @Test
  void testToUriWithMemoryProtocol() {
    // Arrange
    OutputTarget target = new OutputTarget("memory", "editor", "/output.csv");

    // Act
    String uri = target.toUri();

    // Assert
    assertEquals("memory://editor/output.csv", uri);
  }

  @Test
  void testToUriWithStdoutProtocol() {
    // Arrange
    OutputTarget target = new OutputTarget("stdout", "", "/output");

    // Act
    String uri = target.toUri();

    // Assert
    assertEquals("stdout:///output", uri);
  }

  @Test
  void testToUriWithLocalPathOnly() {
    // Arrange - no protocol
    OutputTarget target = new OutputTarget("/local/path/file.txt");

    // Act
    String uri = target.toUri();

    // Assert
    assertEquals("/local/path/file.txt", uri);
  }

  @Test
  void testToUriRoundTripWithFileProtocol() {
    // Arrange - Test that parse -> toUri -> parse works
    String originalUri = "file:///tmp/test.csv";
    ExportTarget parsed = ExportTargetParser.parse(originalUri);
    OutputTarget target = new OutputTarget(
        parsed.getProtocol(),
        parsed.getHost(),
        parsed.getPath()
    );

    // Act
    String reconstructed = target.toUri();

    // Assert
    assertEquals(originalUri, reconstructed);
  }

  @Test
  void testToUriRoundTripWithMinioProtocol() {
    // Arrange
    String originalUri = "minio://my-bucket/path/data.csv";
    ExportTarget parsed = ExportTargetParser.parse(originalUri);
    OutputTarget target = new OutputTarget(
        parsed.getProtocol(),
        parsed.getHost(),
        parsed.getPath()
    );

    // Act
    String reconstructed = target.toUri();

    // Assert
    assertEquals(originalUri, reconstructed);
  }

  @Test
  void testToUriRoundTripWithMemoryProtocol() {
    // Arrange
    String originalUri = "memory://editor/output.csv";
    ExportTarget parsed = ExportTargetParser.parse(originalUri);
    OutputTarget target = new OutputTarget(
        parsed.getProtocol(),
        parsed.getHost(),
        parsed.getPath()
    );

    // Act
    String reconstructed = target.toUri();

    // Assert
    assertEquals(originalUri, reconstructed);
  }

  // ========== Protocol Detection Tests ==========

  @Test
  void testIsMinioTargetWithMinioProtocol() {
    // Arrange
    OutputTarget target = new OutputTarget("minio", "bucket", "/path/file.csv");

    // Act & Assert
    assertTrue(target.isMinioTarget());
    assertFalse(target.isFileTarget());
    assertFalse(target.isStdoutTarget());
    assertFalse(target.isMemoryTarget());
  }

  @Test
  void testIsFileTargetWithFileProtocol() {
    // Arrange
    OutputTarget target = new OutputTarget("file", "", "/tmp/output.txt");

    // Act & Assert
    assertTrue(target.isFileTarget());
    assertFalse(target.isMinioTarget());
    assertFalse(target.isStdoutTarget());
    assertFalse(target.isMemoryTarget());
  }

  @Test
  void testIsFileTargetWithEmptyProtocol() {
    // Arrange - local path without protocol
    OutputTarget target = new OutputTarget("/tmp/output.txt");

    // Act & Assert
    assertTrue(target.isFileTarget());
    assertFalse(target.isMinioTarget());
    assertFalse(target.isStdoutTarget());
    assertFalse(target.isMemoryTarget());
  }

  @Test
  void testIsStdoutTargetWithStdoutProtocol() {
    // Arrange
    OutputTarget target = new OutputTarget("stdout", "", "/output");

    // Act & Assert
    assertTrue(target.isStdoutTarget());
    assertFalse(target.isFileTarget());
    assertFalse(target.isMinioTarget());
    assertFalse(target.isMemoryTarget());
  }

  @Test
  void testIsMemoryTargetWithMemoryProtocol() {
    // Arrange
    OutputTarget target = new OutputTarget("memory", "editor", "/output.csv");

    // Act & Assert
    assertTrue(target.isMemoryTarget());
    assertFalse(target.isFileTarget());
    assertFalse(target.isMinioTarget());
    assertFalse(target.isStdoutTarget());
  }

  @Test
  void testProtocolDetectionIsCaseInsensitive() {
    // Arrange - test with different case variations
    final OutputTarget minioUpper = new OutputTarget("MINIO", "bucket", "/path");
    final OutputTarget minioMixed = new OutputTarget("MiNiO", "bucket", "/path");
    final OutputTarget fileUpper = new OutputTarget("FILE", "", "/tmp/file");
    final OutputTarget stdoutUpper = new OutputTarget("STDOUT", "", "/output");
    final OutputTarget memoryUpper = new OutputTarget("MEMORY", "editor", "/output");

    // Act & Assert
    assertTrue(minioUpper.isMinioTarget());
    assertTrue(minioMixed.isMinioTarget());
    assertTrue(fileUpper.isFileTarget());
    assertTrue(stdoutUpper.isStdoutTarget());
    assertTrue(memoryUpper.isMemoryTarget());
  }

  // ========== Accessor Tests ==========

  @Test
  void testGetProtocol() {
    // Arrange
    OutputTarget target = new OutputTarget("minio", "bucket", "/path");

    // Act & Assert
    assertEquals("minio", target.getProtocol());
  }

  @Test
  void testGetHost() {
    // Arrange
    OutputTarget target = new OutputTarget("minio", "bucket", "/path");

    // Act & Assert
    assertEquals("bucket", target.getHost());
  }

  @Test
  void testGetPath() {
    // Arrange
    OutputTarget target = new OutputTarget("minio", "bucket", "/path/to/file.csv");

    // Act & Assert
    assertEquals("/path/to/file.csv", target.getPath());
  }

  @Test
  void testThreeArgConstructor() {
    // Arrange & Act
    OutputTarget target = new OutputTarget("minio", "bucket", "/path");

    // Assert
    assertEquals("minio", target.getProtocol());
    assertEquals("bucket", target.getHost());
    assertEquals("/path", target.getPath());
  }

  @Test
  void testTwoArgConstructor() {
    // Arrange & Act
    OutputTarget target = new OutputTarget("file", "/tmp/output.txt");

    // Assert
    assertEquals("file", target.getProtocol());
    assertEquals("", target.getHost());
    assertEquals("/tmp/output.txt", target.getPath());
  }

  @Test
  void testOneArgConstructor() {
    // Arrange & Act
    OutputTarget target = new OutputTarget("/local/path/file.txt");

    // Assert
    assertEquals("", target.getProtocol());
    assertEquals("", target.getHost());
    assertEquals("/local/path/file.txt", target.getPath());
  }

  // ========== Equals and HashCode Tests ==========

  @Test
  void testEqualsWithSameValues() {
    // Arrange
    OutputTarget target1 = new OutputTarget("minio", "bucket", "/path");
    OutputTarget target2 = new OutputTarget("minio", "bucket", "/path");

    // Act & Assert
    assertEquals(target1, target2);
    assertEquals(target1.hashCode(), target2.hashCode());
  }

  @Test
  void testEqualsWithSameInstance() {
    // Arrange
    OutputTarget target = new OutputTarget("minio", "bucket", "/path");

    // Act & Assert
    assertEquals(target, target);
  }

  @Test
  void testEqualsWithDifferentProtocol() {
    // Arrange
    OutputTarget target1 = new OutputTarget("minio", "bucket", "/path");
    OutputTarget target2 = new OutputTarget("file", "bucket", "/path");

    // Act & Assert
    assertNotEquals(target1, target2);
  }

  @Test
  void testEqualsWithDifferentHost() {
    // Arrange
    OutputTarget target1 = new OutputTarget("minio", "bucket1", "/path");
    OutputTarget target2 = new OutputTarget("minio", "bucket2", "/path");

    // Act & Assert
    assertNotEquals(target1, target2);
  }

  @Test
  void testEqualsWithDifferentPath() {
    // Arrange
    OutputTarget target1 = new OutputTarget("minio", "bucket", "/path1");
    OutputTarget target2 = new OutputTarget("minio", "bucket", "/path2");

    // Act & Assert
    assertNotEquals(target1, target2);
  }

  @Test
  void testEqualsWithNull() {
    // Arrange
    OutputTarget target = new OutputTarget("minio", "bucket", "/path");

    // Act & Assert
    assertNotEquals(null, target);
  }

  @Test
  void testEqualsWithDifferentClass() {
    // Arrange
    OutputTarget target = new OutputTarget("minio", "bucket", "/path");
    String notTarget = "not a target";

    // Act & Assert
    assertNotEquals(target, notTarget);
  }

  // ========== ToString Tests ==========

  @Test
  void testToString() {
    // Arrange
    OutputTarget target = new OutputTarget("minio", "bucket", "/path");

    // Act
    String result = target.toString();

    // Assert
    assertTrue(result.contains("minio"));
    assertTrue(result.contains("bucket"));
    assertTrue(result.contains("/path"));
    assertTrue(result.contains("OutputTarget"));
  }
}
