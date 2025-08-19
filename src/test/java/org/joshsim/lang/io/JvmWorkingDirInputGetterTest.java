package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class JvmWorkingDirInputGetterTest {

  private static final String TEST_FILE_PATH = "/tmp/josh-working-dir-test.txt";
  private static final String TEST_CONTENT = "working directory test content";
  private File testFile;
  private JvmWorkingDirInputGetter inputGetter;

  @BeforeEach
  void setUp() throws Exception {
    testFile = new File(TEST_FILE_PATH);
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write(TEST_CONTENT);
    }
    inputGetter = new JvmWorkingDirInputGetter();
  }

  @AfterEach
  void tearDown() {
    if (testFile != null && testFile.exists()) {
      testFile.delete();
    }
  }

  @Test
  void testOpenExistingFile() throws Exception {
    // Act
    InputStream inputStream = inputGetter.open(TEST_FILE_PATH);

    // Assert
    String result = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    assertEquals(TEST_CONTENT, result);
  }

  @Test
  void testExistsForExistingFile() {
    // Act & Assert
    assertTrue(inputGetter.exists(TEST_FILE_PATH));
  }

  @Test
  void testExistsForNonExistentFile() {
    // Act & Assert
    assertFalse(inputGetter.exists("/tmp/non-existent-file.txt"));
  }

  @Test
  void testOpenNonExistentFileThrowsException() {
    // Act & Assert
    assertThrows(RuntimeException.class, () -> {
      inputGetter.open("/tmp/non-existent-file.txt");
    });
  }

  @Test
  void testUriExists() {
    // URIs should always return true for exists check
    assertTrue(inputGetter.exists("http://example.com/test.txt"));
    assertTrue(inputGetter.exists("file:///tmp/test.txt"));
  }
}