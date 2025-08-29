
package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class JvmInputGetterTest {

  private static final String TEST_FILE_PATH = "/tmp/josh-jvm-example.txt";
  private static final String TEST_CONTENT = "test text";
  private File testFile;
  private JvmInputGetter inputGetter;

  @BeforeEach
  void setUp() throws Exception {
    testFile = new File(TEST_FILE_PATH);
    try (FileWriter writer = new FileWriter(testFile)) {
      writer.write(TEST_CONTENT);
    }
    // Use concrete implementation for testing
    inputGetter = new JvmWorkingDirInputGetter();
  }

  @AfterEach
  void tearDown() {
    if (testFile != null && testFile.exists()) {
      testFile.delete();
    }
  }

  @Test
  void testOpenFileFromWorkingDirectory() throws Exception {
    // Act
    InputStream inputStream = inputGetter.open(TEST_FILE_PATH);

    // Assert
    String result = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    assertEquals(TEST_CONTENT, result);
  }

  @Test
  void testExistsFileFromWorkingDirectory() {
    // Act & Assert
    assertTrue(inputGetter.exists(TEST_FILE_PATH));
  }

  @Test
  void testUriHandling() {
    // Act & Assert - URIs should be assumed to exist (they're handled by loadFromUri)
    // NOTE: This test does NOT generate network traffic. The exists() method for URIs
    // simply returns true without making any HTTP requests for performance reasons.
    assertTrue(inputGetter.exists("http://example.com/test.txt"));
  }
}
