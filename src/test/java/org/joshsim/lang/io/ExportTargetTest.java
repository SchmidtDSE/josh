package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ExportTargetTest {

  /**
   * Tests for the getFileType method in the ExportTarget class.
   * This method retrieves the file type (extension) from the given path.
   * It returns an empty string if no valid extension exists.
   */

  @Test
  void testGetFileTypeWithValidExtension() {
    // Arrange
    ExportTarget exportTarget = new ExportTarget("local", "", "/files/document.txt");

    // Act
    String fileType = exportTarget.getFileType();

    // Assert
    assertEquals("txt", fileType);
  }

  @Test
  void testGetFileTypeWithNoExtension() {
    // Arrange
    ExportTarget exportTarget = new ExportTarget("local", "/files/document");

    // Act
    String fileType = exportTarget.getFileType();

    // Assert
    assertEquals("", fileType);
  }

  @Test
  void testGetFileTypeWithPathEndingWithDot() {
    // Arrange
    ExportTarget exportTarget = new ExportTarget("/files/document.");

    // Act
    String fileType = exportTarget.getFileType();

    // Assert
    assertEquals("", fileType);
  }

  @Test
  void testGetFileTypeWithMultipleDots() {
    // Arrange
    ExportTarget exportTarget = new ExportTarget("local", "", "/files/archive.tar.gz");

    // Act
    String fileType = exportTarget.getFileType();

    // Assert
    assertEquals("gz", fileType);
  }

  @Test
  void testGetFileTypeOnRootFile() {
    // Arrange
    ExportTarget exportTarget = new ExportTarget("local", "/document.pdf");

    // Act
    String fileType = exportTarget.getFileType();

    // Assert
    assertEquals("pdf", fileType);
  }

  @Test
  void testGetFileTypeWithEmptyPath() {
    // Arrange
    ExportTarget exportTarget = new ExportTarget("local", "", "");

    // Act
    String fileType = exportTarget.getFileType();

    // Assert
    assertEquals("", fileType);
  }

  @Test
  void testGetFileTypeWithPathOnlySlash() {
    // Arrange
    ExportTarget exportTarget = new ExportTarget("local", "", "/");

    // Act
    String fileType = exportTarget.getFileType();

    // Assert
    assertEquals("", fileType);
  }

  @Test
  void testToUriWithFileProtocolEmptyHost() {
    // Arrange
    ExportTarget exportTarget = new ExportTarget("file", "", "/tmp/output.csv");

    // Act
    String uri = exportTarget.toUri();

    // Assert
    assertEquals("file:///tmp/output.csv", uri);
  }

  @Test
  void testToUriWithMinioProtocolAndHost() {
    // Arrange
    ExportTarget exportTarget = new ExportTarget("minio", "bucket", "/path/to/file.csv");

    // Act
    String uri = exportTarget.toUri();

    // Assert
    assertEquals("minio://bucket/path/to/file.csv", uri);
  }

  @Test
  void testToUriWithMemoryProtocol() {
    // Arrange
    ExportTarget exportTarget = new ExportTarget("memory", "editor", "/output.csv");

    // Act
    String uri = exportTarget.toUri();

    // Assert
    assertEquals("memory://editor/output.csv", uri);
  }

  @Test
  void testToUriWithLocalPathOnly() {
    // Arrange - no protocol
    ExportTarget exportTarget = new ExportTarget("/local/path/file.txt");

    // Act
    String uri = exportTarget.toUri();

    // Assert
    assertEquals("/local/path/file.txt", uri);
  }

  @Test
  void testToUriRoundTripWithFileProtocol() {
    // Arrange - Test that parse -> toUri -> parse works
    String originalUri = "file:///tmp/test.csv";
    ExportTarget parsed = ExportTargetParser.parse(originalUri);

    // Act
    String reconstructed = parsed.toUri();

    // Assert
    assertEquals(originalUri, reconstructed);
  }

  @Test
  void testToUriRoundTripWithMinioProtocol() {
    // Arrange
    String originalUri = "minio://my-bucket/path/data.csv";
    ExportTarget parsed = ExportTargetParser.parse(originalUri);

    // Act
    String reconstructed = parsed.toUri();

    // Assert
    assertEquals(originalUri, reconstructed);
  }

  @Test
  void testToUriRoundTripWithMemoryProtocol() {
    // Arrange
    String originalUri = "memory://editor/output.csv";
    ExportTarget parsed = ExportTargetParser.parse(originalUri);

    // Act
    String reconstructed = parsed.toUri();

    // Assert
    assertEquals(originalUri, reconstructed);
  }
}
