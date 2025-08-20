package org.joshsim.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class RunCommandDataOptionTest {

  private static final String TEST_CONTENT = "test content";
  private File testFile1;
  private File testFile2;
  private RunCommand runCommand;

  @BeforeEach
  void setUp() throws Exception {
    testFile1 = new File("/tmp/test-config.jshc");
    try (FileWriter writer = new FileWriter(testFile1)) {
      writer.write(TEST_CONTENT);
    }

    testFile2 = new File("/tmp/test-data.jshd");
    try (FileWriter writer = new FileWriter(testFile2)) {
      writer.write(TEST_CONTENT);
    }

    runCommand = new RunCommand();
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
  void testParseDataFilesSingleEntry() throws Exception {
    // Arrange
    String[] dataFiles = {"config.jshc=/tmp/test-config.jshc"};

    // Act
    Map<String, String> result = invokeParseDataFiles(dataFiles);

    // Assert
    assertEquals(1, result.size());
    assertEquals("/tmp/test-config.jshc", result.get("config.jshc"));
  }

  @Test
  void testParseDataFilesMultipleEntries() throws Exception {
    // Arrange
    String[] dataFiles = {
        "config.jshc=/tmp/test-config.jshc",
        "data.jshd=/tmp/test-data.jshd"
    };

    // Act
    Map<String, String> result = invokeParseDataFiles(dataFiles);

    // Assert
    assertEquals(2, result.size());
    assertEquals("/tmp/test-config.jshc", result.get("config.jshc"));
    assertEquals("/tmp/test-data.jshd", result.get("data.jshd"));
  }

  @Test
  void testParseDataFilesWithSpaces() throws Exception {
    // Arrange
    String[] dataFiles = {" config.jshc = /tmp/test-config.jshc "};

    // Act
    Map<String, String> result = invokeParseDataFiles(dataFiles);

    // Assert
    assertEquals(1, result.size());
    assertEquals("/tmp/test-config.jshc", result.get("config.jshc"));
  }

  @Test
  void testParseDataFilesInvalidFormat() throws Exception {
    // Arrange
    String[] dataFiles = {"invalid-format-no-equals"};

    // Act & Assert
    Exception exception = assertThrows(Exception.class, () -> {
      invokeParseDataFiles(dataFiles);
    });
    // Check if it's the expected IllegalArgumentException wrapped in InvocationTargetException
    assertTrue(exception.getCause() instanceof IllegalArgumentException);
    assertTrue(exception.getCause().getMessage().contains("Invalid data file format"));
  }

  @Test
  void testParseDataFilesEmptyArray() throws Exception {
    // Arrange
    String[] dataFiles = {};

    // Act
    Map<String, String> result = invokeParseDataFiles(dataFiles);

    // Assert
    assertEquals(0, result.size());
  }

  @Test
  void testParseDataFilesWithEqualsInPath() throws Exception {
    // Arrange - path contains equals sign
    String[] dataFiles = {"config.jshc=/tmp/path=with=equals.jshc"};

    // Act
    Map<String, String> result = invokeParseDataFiles(dataFiles);

    // Assert
    assertEquals(1, result.size());
    assertEquals("/tmp/path=with=equals.jshc", result.get("config.jshc"));
  }

  /**
   * Helper method to invoke the private parseDataFiles method using reflection.
   */
  private Map<String, String> invokeParseDataFiles(String[] dataFiles) throws Exception {
    Method method = RunCommand.class.getDeclaredMethod("parseDataFiles", String[].class);
    method.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<String, String> result = (Map<String, String>) method.invoke(runCommand,
        (Object) dataFiles);
    return result;
  }
}
