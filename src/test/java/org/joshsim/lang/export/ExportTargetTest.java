package org.joshsim.lang.export;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}