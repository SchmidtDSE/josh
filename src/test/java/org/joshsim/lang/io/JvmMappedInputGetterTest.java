package org.joshsim.lang.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class JvmMappedInputGetterTest {

  private static final String TEST_FILE_PATH_1 = "/tmp/josh-mapped-test1.txt";
  private static final String TEST_FILE_PATH_2 = "/tmp/josh-mapped-test2.txt";
  private static final String TEST_CONTENT_1 = "mapped test content 1";
  private static final String TEST_CONTENT_2 = "mapped test content 2";
  private File testFile1;
  private File testFile2;
  private JvmMappedInputGetter inputGetter;

  @BeforeEach
  void setUp() throws Exception {
    testFile1 = new File(TEST_FILE_PATH_1);
    try (FileWriter writer = new FileWriter(testFile1)) {
      writer.write(TEST_CONTENT_1);
    }

    testFile2 = new File(TEST_FILE_PATH_2);
    try (FileWriter writer = new FileWriter(testFile2)) {
      writer.write(TEST_CONTENT_2);
    }

    // Create mapping
    Map<String, String> fileMapping = new HashMap<>();
    fileMapping.put("config.jshc", TEST_FILE_PATH_1);
    fileMapping.put("data.jshd", TEST_FILE_PATH_2);

    inputGetter = new JvmMappedInputGetter(fileMapping);
  }

  @AfterEach
  void tearDown() {
    if (testFile1 != null && testFile1.exists()) {
      testFile1.delete();
    }
    if (testFile2 != null && testFile2.exists()) {
      testFile2.delete();
    }
  }

  @Test
  void testOpenMappedFile() throws Exception {
    // Act
    InputStream inputStream = inputGetter.open("config.jshc");

    // Assert
    String result = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    assertEquals(TEST_CONTENT_1, result);
  }

  @Test
  void testOpenSecondMappedFile() throws Exception {
    // Act
    InputStream inputStream = inputGetter.open("data.jshd");

    // Assert
    String result = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    assertEquals(TEST_CONTENT_2, result);
  }

  @Test
  void testExistsForMappedFile() {
    // Act & Assert
    assertTrue(inputGetter.exists("config.jshc"));
    assertTrue(inputGetter.exists("data.jshd"));
  }

  @Test
  void testExistsForNonMappedFile() {
    // Act & Assert
    assertFalse(inputGetter.exists("non-mapped-file.txt"));
  }

  @Test
  void testOpenNonMappedFileThrowsException() {
    // Act & Assert
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      inputGetter.open("non-mapped-file.txt");
    });
    assertTrue(exception.getMessage().contains("File not found in mapped files"));
  }

  @Test
  void testUriExists() {
    // URIs should always return true for exists check
    assertTrue(inputGetter.exists("http://example.com/test.txt"));
    assertTrue(inputGetter.exists("file:///tmp/test.txt"));
  }

  @Test
  void testGetFileMapping() {
    // Act
    Map<String, String> mapping = inputGetter.getFileMapping();

    // Assert
    assertEquals(2, mapping.size());
    assertEquals(TEST_FILE_PATH_1, mapping.get("config.jshc"));
    assertEquals(TEST_FILE_PATH_2, mapping.get("data.jshd"));
  }

  @Test
  void testFileMappingIsImmutable() {
    // Act
    Map<String, String> mapping = inputGetter.getFileMapping();

    // Assert - attempting to modify should throw exception
    assertThrows(UnsupportedOperationException.class, () -> {
      mapping.put("test", "value");
    });
  }

  @Test
  void testConstructorMakesDefensiveCopy() {
    // Arrange
    Map<String, String> originalMapping = new HashMap<>();
    originalMapping.put("test.txt", "/tmp/test.txt");

    JvmMappedInputGetter getter = new JvmMappedInputGetter(originalMapping);

    // Act - modify original mapping
    originalMapping.put("modified.txt", "/tmp/modified.txt");

    // Assert - getter's mapping should not be affected
    assertFalse(getter.exists("modified.txt"));
    assertEquals(1, getter.getFileMapping().size());
  }
}
